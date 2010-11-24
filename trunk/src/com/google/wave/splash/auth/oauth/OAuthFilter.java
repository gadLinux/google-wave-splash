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
package com.google.wave.splash.auth.oauth;

import com.google.common.base.Predicate;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.wave.splash.auth.AnonymousSession;
import com.google.wave.splash.web.stats.Stat;
import com.google.wave.splash.web.stats.Stats;

import net.oauth.OAuthException;
import net.oauth.OAuthServiceProvider;
import net.oauth.client.OAuthClient;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Filters all incoming requests to ensure that they are valid OAuth-authorized
 * requests.
 *
 * @author David Byttow
 */
@Singleton
class OAuthFilter implements Filter {
  private static final String LOGOUT_URI = "/logout";

  @Stat(name = "oauth-login-failures", help = "Number of oauth exceptions")
  private static volatile int oauthLoginFailures;

  static {
    Stats.trackClass(OAuthFilter.class);
  }

  private final OAuthServiceProvider provider;
  private final OAuthClient client;
  private final Predicate<? super HttpServletRequest> authPredicate;

  @Inject
  OAuthFilter(OAuthServiceProvider provider, OAuthClient client,
      @Named("authPredicate") Predicate<? super HttpServletRequest> authPredicate) {
    this.provider = provider;
    this.client = client;
    this.authPredicate = authPredicate;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse resp = (HttpServletResponse) response;

    if (AnonymousSession.isAnonymousRequest(req)) {
      OAuthUtil.invalidateSession(resp);
    }

    if (!requiresAuthentication(req)) {
      chain.doFilter(req, resp);
      return;
    }

    try {
      OAuthUtil.authenticate(provider, client, req, resp);
     } catch (OAuthException e) {
      // It's not yet clear what to do here, perhaps a login failed warning
      // like Gmail does.
      ++oauthLoginFailures;
      OAuthUtil.invalidateSession(resp);
      throw new ServletException(e);
    }
  }

  private boolean requiresAuthentication(HttpServletRequest req) {
    if (OAuthUtil.isAuthenticated(req)) {
      return false;
    }

    if (!authPredicate.apply(req)) {
      // Anonymous request, skip authentication step.
      // TODO: Invalidate the session?
      return false;
    }

    String uri = req.getRequestURI();
    if (uri.equals(LOGOUT_URI)) {
      return false;
    }

    return true;
  }

  @Override
  public void init(FilterConfig config) {
  }

  @Override
  public void destroy() {
  }
}
