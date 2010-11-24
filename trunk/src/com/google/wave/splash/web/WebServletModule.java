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

import com.google.common.base.Charsets;
import com.google.common.base.Predicate;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.servlet.ServletModule;
import com.google.wave.splash.Options;
import com.google.wave.splash.PortableRequestScope;
import com.google.wave.splash.RequestScopeExecutor;
import com.google.wave.splash.auth.PortableRequestScopeExecutor;
import com.google.wave.splash.rpc.Rpc;
import com.google.wave.splash.web.async.EditHandler;
import com.google.wave.splash.web.async.RpcHandler;
import com.google.wave.splash.web.async.RpcServlet;
import com.google.wave.splash.web.async.SearchHandler;
import com.google.wave.splash.web.async.WaveOpenHandler;
import com.google.wave.splash.web.async.WaveUpdateHandler;
import com.google.wave.splash.web.template.ResourceServlet;

import org.waveprotocol.wave.model.waveref.WaverefEncoder;
import org.waveprotocol.wave.model.waveref.WaverefEncoder.PercentEncoderDecoder;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

/**
 * Servlet module for web-services.
 *
 * @author dhanji@gmail.com (Dhanji Prasanna)
 * @author David Byttow
 */
public class WebServletModule extends ServletModule {
  private static final Logger LOG = Logger.getLogger(WebServletModule.class.getName());

  /**
   * Predicate which determines whether or not, given a request, if auth is
   * required.
   */
  @Singleton
  public static class AuthRequiredPredicate implements Predicate<ServletRequest> {
    @Override
    public boolean apply(ServletRequest request) {
      HttpServletRequest req = (HttpServletRequest) request;
      // Allow pass-thru for embed.
      String type = req.getParameter("client.type");
      if ("embedded".equals(type)) {
        return false;
      }

      // Allow pass-thru for /async.
      if (req.getRequestURI().startsWith("/async")) {
        return false;
      }

      String uri = req.getRequestURI();
      int index = uri.lastIndexOf('.');
      if (index >= 0) {
        String filetype = uri.substring(index + 1);
        if (ResourceServlet.STATIC_RESOURCE_TYPES.contains(filetype)) {
          return false;
        }
      }
      return true;
    }
  }

  /**
   * Filter that seeds the http servlet request and response to the
   * PortableRequestScope.
   */
  @Singleton
  static class PortableRequestScopeFilter implements Filter {
    private final PortableRequestScope requestScope;

    @Inject
    PortableRequestScopeFilter(PortableRequestScope requestScope) {
      this.requestScope = requestScope;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
      requestScope.enter();
      try {
        chain.doFilter(request, response);
      } finally {
        requestScope.exit();
      }
    }

    @Override
    public void init(FilterConfig config) {
    }

    @Override
    public void destroy() {
    }
  }

  private final Options options;

  public WebServletModule(Options options) {
    this.options = options;
  }

  @Override
  protected void configureServlets() {
    filter("/*").through(PortableRequestScopeFilter.class);
    filter("/*").through(ProtocolVersionFilter.class);
    serve("/async/*").with(RpcServlet.class);
    serve("/w/*").with(PermalinkClientServlet.class);

    if (options.enableFullClient()) {
      serve("/wave").with(FullClientServlet.class);
    }

    if (options.enableMobileClient()) {
      serve("/m").with(MobileClientServlet.class);
    }

    bind(RequestScopeExecutor.class).to(PortableRequestScopeExecutor.class).in(Scopes.SINGLETON);
  }

  @Provides @Singleton @Rpc
  Map<String, RpcHandler> provideAsyncRpcHandlers(
      SearchHandler searchHandler,
      WaveUpdateHandler waveUpdateHandler,
      WaveOpenHandler waveHandler,
      EditHandler editHandler) {

    Map<String, RpcHandler> map = Maps.newHashMap();
    map.put("open_wave", waveHandler);
    map.put("search", searchHandler);
    map.put("update_wave", waveUpdateHandler);
    map.put("edit_wave", editHandler);
    return map;
  }

  @Provides @Singleton
  WaverefEncoder provideWaverefEncoder() {
    return new WaverefEncoder(new PercentEncoderDecoder() {
      @Override
      public String decode(String str) {
        try {
          return URLDecoder.decode(str, Charsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
          LOG.severe("Failed to decode: " + str);
        }
        return null;
      }

      @Override
      public String pathEncode(String decodedValue) {
        throw new UnsupportedOperationException();
      }

      @Override
      public String queryEncode(String decodedValue) {
        throw new UnsupportedOperationException();
      }
    });
  }

  @Provides @Singleton @Named("authPredicate")
  Predicate<? super HttpServletRequest> provideAuthPredicate() {
    return new AuthRequiredPredicate();
  }
}
