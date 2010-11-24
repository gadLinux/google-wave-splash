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

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

/**
 * Set of utilities for web services.
 *
 * @author David Byttow
 */
class WebUtil {
  /**
   * Writes an html-based response.
   *
   * @param response the response to write to.
   * @param html the content html.
   * @throws IOException
   */
  public static void writeHtmlResponse(HttpServletResponse response, String html)
      throws IOException {
    response.setStatus(HttpServletResponse.SC_OK);
    response.setContentType("text/html; charset=utf-8");
    response.setHeader("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
    response.setHeader("Pragma", "no-cache");
    response.setHeader("X-Frame-Options", "ALLOWALL");
    response.getWriter().write(html);
    response.getWriter().flush();
    response.getWriter().close();
  }
}
