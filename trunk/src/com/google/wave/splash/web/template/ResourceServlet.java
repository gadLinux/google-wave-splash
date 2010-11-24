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

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Serves static resources, CSS files, etc., from the ripple Jar directly.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
@Singleton
public class ResourceServlet extends HttpServlet {
  public static final Set<String> STATIC_RESOURCE_TYPES =
      ImmutableSet.of("css", "js", "png", "jpg", "jpeg", "gif", "html");

  private static final Map<String, String> mimeTypes = Maps.newHashMap();

  /**
   * 64k read buffer, only used for determining a file length the very first
   * time it is served.
   */
  private static final int READ_BUFFER = 65535;

  static {
    mimeTypes.put("png", "image/png");
    mimeTypes.put("jpg", "image/jpeg");
    mimeTypes.put("gif", "image/gif");

    mimeTypes.put("css", "text/css");
    mimeTypes.put("js", "text/javascript");
  }

  private final Templates templates;
  private final Map<String, Integer> fileLengths = new MapMaker().makeMap();

  @Inject
  public ResourceServlet(Templates templates) {
    this.templates = templates;
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws IOException {
    // We're only worried about the simple name of the requested file.
    int slashPosition = req.getRequestURI().lastIndexOf("/") + 1;

    String fileName = req.getRequestURI().substring(slashPosition);

    if (Strings.isNullOrEmpty(fileName)) {
      resp.sendError(HttpServletResponse.SC_NOT_FOUND,
          "No such resource found: " + req.getRequestURI()
      );
      return;
    }

    // Read the file and pipe it to the output.
    InputStream resource = templates.openResource(fileName);
    if (resource == null) {
      resp.sendError(HttpServletResponse.SC_NOT_FOUND,
          "No such resource found: " + req.getRequestURI()
      );
      return;
    }

    setContentLength(resp, fileName);
    setContentType(resp, fileName);

    pipe(resource, resp);
    resp.getOutputStream().flush();
    resp.getOutputStream().close();
  }

  private int determineFileLength(String fileName) throws IOException {
    InputStream input = templates.openResource(fileName);

    int fileLength = 0;
    try {
      byte[] buffer = new byte[READ_BUFFER];
      int bytesRead;
      while ((bytesRead = input.read(buffer)) != -1) {
        fileLength += bytesRead;
      }
    } finally {
      input.close();
    }

    return fileLength;
  }

  private void setContentLength(HttpServletResponse resp, String fileName)
      throws IOException {
    Integer length = fileLengths.get(fileName);

    // Read length from file system if necessary.
    if (null == length) {
      length = determineFileLength(fileName);
      fileLengths.put(fileName, length);
    }

    resp.setContentLength(length);
  }

  private static void setContentType(HttpServletResponse resp, String fileName) throws IOException {
    int lastDot = fileName.lastIndexOf(".");

    if (lastDot == -1) {
      resp.sendError(404, "Cannot serve resources without a filename extension");
      return;
    }

    String extension = fileName.substring(lastDot + 1);
    String mime = mimeTypes.get(extension);

    if (null == mime) {
      resp.sendError(404, "Don't know how to serve that file");
      return;
    }

    resp.setContentType(mime);
  }

  private static void pipe(InputStream input, HttpServletResponse response)
      throws IOException {
    OutputStream output = response.getOutputStream();

    try {
      byte[] buffer = new byte[response.getBufferSize()];
      int bytesRead;
      while ((bytesRead = input.read(buffer)) != -1) {
        output.write(buffer, 0, bytesRead);
      }
    } finally {
      input.close();
    }
  }
}
