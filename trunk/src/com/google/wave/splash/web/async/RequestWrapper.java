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
package com.google.wave.splash.web.async;

import javax.servlet.http.HttpServletRequest;
import java.util.Set;

/**
 * Wraps an HttpServletRequest to provide params. This is done so that we do
 * not have to do getParameter(name)
 *
 * @author David Byttow
 */
public class RequestWrapper implements RpcHandler.Params {
  private final HttpServletRequest request;

  RequestWrapper(HttpServletRequest request) {
    this.request = request;
  }

  @Override
  public String get(String name) {
    return request.getParameter(name);
  }

  @SuppressWarnings({"cast", "unchecked"})
  @Override
  public Set<String> nameSet() {
    return (Set<String>) request.getParameterMap().keySet();
  }

  /**
   * Returns a wrapped request. Simple factory method.
   */
  public static RpcHandler.Params wrap(HttpServletRequest request) {
    return new RequestWrapper(request);
  }
}
