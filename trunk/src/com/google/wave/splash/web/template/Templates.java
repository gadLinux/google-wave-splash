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
package com.google.wave.splash.web.template;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.internal.Function;
import com.google.inject.internal.MapMaker;
import com.google.inject.internal.Nullable;
import com.google.wave.splash.Options;
import com.google.wave.splash.text.Markup;

import org.mvel2.templates.CompiledTemplate;
import org.mvel2.templates.TemplateCompiler;
import org.mvel2.templates.TemplateRuntime;
import org.mvel2.templates.util.TemplateTools;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

import javax.servlet.ServletContext;

/**
 * Handles all our html templates, loading, parsing and processing them.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
@Singleton
public class Templates {
  // Template file names go here.
  public static final String BLIP_TEMPLATE = "blip.html.fragment";
  public static final String HEADER_TEMPLATE = "header.html.fragment";
  public static final String FEED_TEMPLATE = "feed.html.fragment";
  public static final String PERMALINK_WAVE_TEMPLATE = "permalink_client.html";
  public static final String CLIENT_TEMPLATE = "full_client.html";
  public static final String MOBILE_TEMPLATE = "mobile_client.html";
  public static final String WAVE_NOT_FOUND_TEMPLATE = "wave_not_found.html.fragment";

  private static final Logger log = Logger.getLogger(Templates.class.getName());

  /**
   * file name of template -> compiled template lazy cache.
   */
  private final ConcurrentMap<String, CompiledTemplate> templates = new MapMaker()
      .makeComputingMap(new Function<String, CompiledTemplate>() {
        @Override
        public CompiledTemplate apply(@Nullable String template) {
          return loadTemplate(template);
        }
      });

  private final Options options;
  private final Markup markup;
  private final Provider<ServletContext> servletContext;

  @Inject
  public Templates(Options options, Markup markup, Provider<ServletContext> servletContext) {
    this.options = options;
    this.markup = markup;
    this.servletContext = servletContext;
  }

  private CompiledTemplate loadTemplate(String template) {
    // Load from jar if in production mode, otherwise servlet root.
    InputStream input = openResource(template);
    Preconditions.checkArgument(input != null, "Could not find template named: " + template);

    try {
      return TemplateCompiler.compileTemplate(TemplateTools.readStream(input));
    } finally {
      try {
        input.close();
      } catch (IOException e) {
        // Can't do much.
        e.printStackTrace();
        log.warning(e.toString());
      }
    }
  }

  /**
   * Opens a packaged resource from the file system.
   *
   * @param file The name of the file/resource to open.
   * @return An {@linkplain InputStream} to the named file, if found
   */
  public InputStream openResource(String file) {
    InputStream stream = options.productionMode()
        ? Templates.class.getResourceAsStream(file)
        : servletContext.get().getResourceAsStream("/" + file);

    // load + compile templates on-demand
    if (null == stream) {
      log.info("Could not find resource named: " + file);
    }
    return stream;
  }

  /**
   * Loads templates if necessary.
   *
   * @param template Name of the template file.
   *                 example: "blip.html.fragment"
   * @param context  an object to process against
   * @return the processed, filled-in template.
   */
  public String process(String template, Object context) {
    // Reload template each time for development mode.
    CompiledTemplate compiledTemplate = options.productionMode()
        ? templates.get(template)
        : loadTemplate(template);

    Map<String, Object> vars = Maps.newHashMap();
    vars.put("markup", markup);
    return TemplateRuntime.execute(compiledTemplate, context, vars).toString();
  }
}
