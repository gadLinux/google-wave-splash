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
package com.google.wave.splash.web.stats;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.wave.splash.data.Memcache;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
* @author dhanji@gmail.com (Dhanji R. Prasanna)
*/
@Singleton
class StatuszServlet extends HttpServlet {
  @Inject
  private Memcache cache;

  @Inject
  private StatsRecorder stats;

  @Override
  protected void service(HttpServletRequest req, HttpServletResponse resp) throws
      IOException {
    String flush = req.getParameter("f");
    if ("yes".equals(flush)) {
      System.out.println("Flushing caches...");
      cache.flush();
    }

    resp.setContentType("text/html");

    PrintWriter writer = resp.getWriter();
    writeHeader(writer);

    String show = req.getParameter("show");
    if (show == null) {
      show = "";
    }
    if (show.equals("stats")) {
      writeStats(writer);
    } else {
      writeProfiling(writer);
    }
  }

  protected void writeHeader(PrintWriter writer) {
    writer.write("Show: <a href=\"?show=measurements\">Measurements</a>");
    writer.write(" | <a href=\"?show=stats\">Stats</a>");
    writer.write("<h2>Cache</h2>");
    writer.print("<input type='button' value='flush caches'"
        + " onclick='window.location=\"/statusz?f=yes\"'>"
        + "<p><pre>");
    writer.print("<input type='button' value='smart fetch'"
        + " onclick='window.location=\"/statusz?f=yes\"'>"
        + "<p><pre>");
    writer.print(cache.toString().replaceAll("\n", "<br>") + "</pre>");
  }

  protected void writeProfiling(PrintWriter writer) {
    writer.write("<h2>Measurements</h2>");
    writer.write(stats.toString());
  }

  protected void writeStats(PrintWriter writer) {
    writer.write("<h2>Stats</h2>");
    StringBuilder builder = new StringBuilder();
    for (Stats.Entry entry : Stats.getStats()) {
      builder.append("<b>")
          .append(entry.getName())
          .append(":</b> ")
          .append(entry.getValue())
          .append("<br/>");
    }
    writer.write(builder.toString());
  }
}
