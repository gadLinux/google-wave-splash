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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.wave.splash.web.template.Templates;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Serves /wave (the entire web client initial load). This needs to be uber
 * fast, so we don't wait for anything. And instead load the feed
 * asynchronously.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
@Singleton
public class MobileClientServlet extends HttpServlet {
  private final Templates templates;

  @Inject
  public MobileClientServlet(Templates templates) {
    this.templates = templates;
  }

  @Override
  protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    // TODO: Username is unavailable (for security reasons),
    // which means we probably need to get it via OAuth.

    Map<String, String> context = new HashMap<String, String>();
    context.put("email", "nobody@googlewave.com");
    context.put("displayName", "unknown");
    String html = templates.process(Templates.MOBILE_TEMPLATE, context);
    WebUtil.writeHtmlResponse(resp, html);
  }
}
