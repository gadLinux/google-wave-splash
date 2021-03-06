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

import com.google.wave.splash.web.Cookies;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;
import net.oauth.OAuthServiceProvider;
import net.oauth.client.OAuthClient;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Helper for handling OAuth-related tasks.
 *
 * @author David Byttow
 */
public class OAuthUtil {
  static final String AUTH_URI = "/auth";
  private static final String CONSUMER_KEY = "anonymous";
  private static final String CONSUMER_SECRET = "anonymous";
  private static final String EXCHANGE_TOKEN_KEY = "oauth_token";

  private static final String PREFIX = "AUTH";
  private static final String CONTINUE_URL = PREFIX + ".cu";
  private static final String REQUEST_TOKEN = PREFIX + ".rt";
  private static final String ACCESS_TOKEN = PREFIX + ".at";
  private static final String TOKEN_SECRET = PREFIX + ".st";

  /**
   * Invalidates the current OAuth session.
   */
  static void invalidateSession(HttpServletResponse resp) {
    Cookies.expireCookie(resp, ACCESS_TOKEN, null);
    Cookies.expireCookie(resp, REQUEST_TOKEN, null);
    Cookies.expireCookie(resp, TOKEN_SECRET, null);
    Cookies.expireCookie(resp, CONTINUE_URL, null);
  }

  /**
   * @return true if it appears that the user is authenticated.
   */
  static boolean isAuthenticated(HttpServletRequest req) {
    return Cookies.getCookie(req, ACCESS_TOKEN) != null
        && Cookies.getCookie(req, TOKEN_SECRET) != null
        && Cookies.getCookie(req, REQUEST_TOKEN) != null;
  }

  /**
   * @return the stored continue url for authorization flow.
   */
  static String getContinueUrl(HttpServletRequest req) {
    return Cookies.getCookie(req, CONTINUE_URL);
  }

  /**
   * Creates and returns an accessor based on the user's cookies.
   */
  static OAuthAccessor createAccessorFromCookies(OAuthServiceProvider service,
      HttpServletRequest req) {
    OAuthAccessor accessor = new OAuthAccessor(
        new OAuthConsumer(createAccessTokenCallbackUrl(req), CONSUMER_KEY, CONSUMER_SECRET,
            service));
    accessor.requestToken = Cookies.getCookie(req, REQUEST_TOKEN);
    accessor.accessToken = Cookies.getCookie(req, ACCESS_TOKEN);
    accessor.tokenSecret = Cookies.getCookie(req, TOKEN_SECRET);
    return accessor;
  }

  /**
   * Handles the entire OAuth flow, in the end, this results in a redirect
   * back to the original request URL.
   *
   * @throws OAuthException if a problem occured with authentication.
   */
  static void authenticate(OAuthServiceProvider provider, OAuthClient client,
      HttpServletRequest req, HttpServletResponse resp) throws OAuthException {
    OAuthAccessor accessor = OAuthUtil.createAccessorFromCookies(provider, req);

    try {
      // 3-Legged OAuth.
      // Step one, redirect to /auth URI.
      if (!req.getRequestURI().equals(OAuthUtil.AUTH_URI)) {
        Cookies.addSessionCookie(resp, CONTINUE_URL, req.getRequestURL().toString(),
            req.getContextPath());
          resp.sendRedirect(OAuthUtil.AUTH_URI);
      } else {
        // Not fully authenticated, check for presence of an exchange token.
        String exchangeToken = req.getParameter(EXCHANGE_TOKEN_KEY);
        if (exchangeToken == null) {
          // Step two, get an exchange token.
          OAuthUtil.redirectForExchange(accessor, client, req, resp);
        } else {
          // Step three, exchange it for an access token.
          OAuthUtil.exchangeForAccessToken(accessor, client, req, resp, exchangeToken);
        }
      }
    } catch (IOException e) {
      throw new OAuthException(e);
    }
  }

  /**
   * Obtains a request token and redirects the user to the authorization
   * URL of the OAuth service provider given.
   *
   * @throws OAuthException if there was an error or failure in getting a
   *     request
   */
  private static void redirectForExchange(OAuthAccessor accessor, OAuthClient client,
      HttpServletRequest req, HttpServletResponse resp) throws IOException, OAuthException {
    try {
      client.getRequestToken(accessor);
    } catch (URISyntaxException e) {
      throw new OAuthException(e);
    }

    Cookies.addPersistentCookie(resp, REQUEST_TOKEN, accessor.requestToken, req.getContextPath());
    Cookies.addPersistentCookie(resp, TOKEN_SECRET, accessor.tokenSecret, req.getContextPath());

    resp.sendRedirect(createAuthorizationUrl(accessor, req));
  }

  /**
   * Exchanges a received exchange token for a persistent access token and
   * redirects back to the continue URL.
   *
   * @throws OAuthException if retrieving the the access token fails.
   */
  private static void exchangeForAccessToken(OAuthAccessor accessor, OAuthClient client,
      HttpServletRequest req, HttpServletResponse resp, String exchangeToken)
      throws IOException, OAuthException {
    OAuthMessage msg;
    try {
      msg = client.getAccessToken(accessor, "GET", OAuth.newList("oauth_token", exchangeToken));
    } catch (URISyntaxException e) {
      throw new OAuthException(e);
    }

    accessor.accessToken = msg.getParameter("oauth_token");
    accessor.tokenSecret = msg.getParameter("oauth_token_secret");
    Cookies.addPersistentCookie(resp, ACCESS_TOKEN, accessor.accessToken, req.getContextPath());
    Cookies.addPersistentCookie(resp, TOKEN_SECRET, accessor.tokenSecret, req.getContextPath());
    resp.sendRedirect(OAuthUtil.getContinueUrl(req));
  }

  private static String createAuthorizationUrl(OAuthAccessor accessor, HttpServletRequest req) {
    return URI.create(accessor.consumer.serviceProvider.userAuthorizationURL +
        "?oauth_token=" + accessor.requestToken +
        "&oauth_callback=" + accessor.consumer.callbackURL +
        "&hd=default").toString();
  }

  private static String createAccessTokenCallbackUrl(HttpServletRequest req) {
    URI uri = URI.create(req.getRequestURL().toString());
    String url = uri.getScheme() + "://" + uri.getAuthority() + AUTH_URI;
    return url;
  }
}
