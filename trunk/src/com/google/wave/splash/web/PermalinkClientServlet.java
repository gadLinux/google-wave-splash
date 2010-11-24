/**
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.google.wave.splash.web;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.wave.splash.Options;
import com.google.wave.splash.rpc.ClientAction;
import com.google.wave.splash.rpc.FeedRpc;
import com.google.wave.splash.rpc.WaveletUpdateRpc;
import com.google.wave.splash.text.Markup;
import com.google.wave.splash.text.Styler;
import com.google.wave.splash.web.async.RequestWrapper;
import com.google.wave.splash.web.async.RpcHandler;
import com.google.wave.splash.web.template.Templates;
import com.google.wave.splash.web.template.WaveRenderer;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.waveref.InvalidWaveRefException;
import org.waveprotocol.wave.model.waveref.WaveRef;
import org.waveprotocol.wave.model.waveref.WaverefEncoder;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Renders the standalone wave client (permalink).
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
@Singleton
public class PermalinkClientServlet extends HttpServlet {
  private final static Logger LOG = Logger.getLogger(PermalinkClientServlet.class.getName());
  private static final String WAVEREF_PATH = "/waveref/";
  private static final Pattern CRAWLER_UA =
      Pattern.compile("Googlebot|msnbot|Yahoo! Slurp|ia_archiver|Ask Jeeves");

  private final Templates templates;
  private final Options options;
  private final WaverefEncoder waverefEncoder;
  private final WaveletUpdateRpc waveletUpdateRpc;

  @Inject
  public PermalinkClientServlet(Templates templates, Options options,
      WaverefEncoder waverefEncoder, WaveletUpdateRpc waveletUpdateRpc) {
    this.templates = templates;
    this.options = options;
    this.waverefEncoder = waverefEncoder;
    this.waveletUpdateRpc = waveletUpdateRpc;
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    RpcHandler.Params params = RequestWrapper.wrap(req);
    Map<String, Object> context = Maps.newHashMap();
    context.put("style", Styler.fromEmbedOptions(params));
    context.put("request", params);

    // TODO: Derive this properly.
    String pathToRoot;

    // If we have the wave id, stash it here. This makes it possible to use this servlet
    // for both embed as well as permalink rendering. We may split them off in the future.
    String waveIdAsString;

    WaveRef waveRef = getWaveRef(req);
    if (waveRef != null) {
      // TODO: Support wavelets other than conv+root and figure
      // the right way around this URLDecoding hack, seriously, why do we have
      // '+' in our wave ids anyway?
      // The problem is that URLDecoder replaces '+' with space and apparently
      // our WaveId serialization does not handle that.
      waveIdAsString = waveRef.getWaveId().serialise().replace(' ', '+');
      pathToRoot = "../../../";
    } else {
      String id = params.get("wave_id");
      // Another hack to support the "+", seriously why use "+".
      waveIdAsString = null == id ? null : id.replace(' ', '+');
      pathToRoot = "../";
    }

    if (null == waveIdAsString) {
      waveIdAsString = "";
    }

    // This needs to be coordinated with the GWT client embed api. Our own rendering
    // options are slightly different and passed along in the wave-update async rpc.
    String headerParam = params.get("embed_header");
    boolean showHeader = (null != headerParam) ? Boolean.valueOf(headerParam) : false;

    // NOTE: This means we support waveref format for embed as well.

    // It is an embed client if either wave_id is a param or if client.type is embedded,
    // Permalink requests always specify their wave id in the /w/[id] format.
    boolean isEmbedded = !Strings.isNullOrEmpty(waveIdAsString)
        && "embedded".equals(req.getParameter("client.type"));
    context.put("waveId", waveIdAsString);
    context.put("embedded", isEmbedded);
    context.put("showHeader", showHeader);
    context.put("showLoginLink", options.showLoginLinkInEmbed());
    context.put("pathToRoot", pathToRoot);

    if (isEmbedded) {
      context.put("waveUri", "wave://" + waveIdAsString);
      context.put("waveLink", "http://wave.google.com/wave/" + waveIdAsString);
      context.put("waveEmbed", Markup.embedSnippet(waveIdAsString));
    }

    if (isCrawler(req) && waveRef != null) {
      addRenderedWave(waveRef.getWaveId(), context);
    } else {
      context.put("content", "");
    }

    String page = templates.process(Templates.PERMALINK_WAVE_TEMPLATE, context);
    WebUtil.writeHtmlResponse(resp, page);
  }


  /**
   * Tests if this request comes from one of:
   * Google, Bing, Yahoo, Alexa, Ask.com
   *
   * @return True if the user-agent is a known crawling robot
   */
  private static boolean isCrawler(HttpServletRequest req) {
    return CRAWLER_UA.matcher(req.getHeader("User-Agent")).matches();
  }

  /**
   * Alternate method renders the entire wave at once with no javascript.
   */
  private void addRenderedWave(WaveId waveId, Map<String, Object> context) {
    Collection<ClientAction> actions = waveletUpdateRpc.smartFetch(waveId,
        FeedRpc.getConversationWaveletId(waveId.getDomain()), WaveRenderer.ALL_PAGES
    );

    String html = null;
    for (ClientAction action : actions) {
      if ("update-wave".equals(action.getAction())) {
        html = action.getHtml();
        break;
      }
    }
    context.put("content", html);
  }

  private WaveRef getWaveRef(HttpServletRequest req) {
    String pathInfo = req.getPathInfo();
    if (pathInfo == null) {
      return null;
    }
    int index = pathInfo.indexOf(WAVEREF_PATH);
    if (index == -1) {
      return null;
    }
    String wavePortion = pathInfo.substring(index + WAVEREF_PATH.length());
    if (wavePortion.isEmpty()) {
      return null;
    }
    try {
      return waverefEncoder.decodeWaveRefFromPath(wavePortion);
    } catch (InvalidWaveRefException e) {
      LOG.info("Invalid waveref request: " + pathInfo);
    }
    return null;
  }
}
