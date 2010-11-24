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
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.wave.splash.auth.SessionContext;
import com.google.wave.splash.rpc.StartupRpc;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

/**
 * Used to set up new sessions after they've successfully authenticated.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
@Singleton
class ProtocolVersionFilter implements Filter {
  private final StartupRpc startupRpc;
  private final Provider<SessionContext> sessionContext;

  @Inject
  public ProtocolVersionFilter(
      StartupRpc startupRpc, Provider<SessionContext> sessionContext) {
    this.startupRpc = startupRpc;
    this.sessionContext = sessionContext;
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
  }

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
      FilterChain filterChain) throws IOException, ServletException {

    HttpServletRequest request = (HttpServletRequest) servletRequest;

    // Prepend op telling the backends the correct protocol version.
    startupRpc.startup(sessionContext.get());

    // Proceed down the filter chain...
    filterChain.doFilter(request, servletResponse);
  }

  @Override
  public void destroy() {
  }
}
