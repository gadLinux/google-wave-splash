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

import com.google.common.io.Files;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.google.inject.servlet.ServletModule;
import com.google.wave.splash.PortableRequestScoped;
import com.google.wave.splash.auth.AnonymousSession;
import com.google.wave.splash.auth.SessionContext;
import com.google.wave.splash.auth.UrlConnectionHttpClient;
import com.google.wave.splash.rpc.json.RequestFactory;

import net.oauth.OAuthServiceProvider;
import net.oauth.client.OAuthClient;
import net.oauth.http.HttpClient;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Properties;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Guice bindings for OAuth-related classes.
 *
 * @author David Byttow
 */
public class OAuthModule extends ServletModule {
  private static final String OS_HANDLER = "http://www-opensocial.googleusercontent.com/api/rpc";
  private static final String SCOPE = "http://wave.googleusercontent.com/api/rpc";

  private static final String REQUEST_URL = "https://www.google.com/accounts/OAuthGetRequestToken";
  private static final String AUTH_URL = "https://www.google.com/accounts/OAuthAuthorizeToken";
  private static final String ACCESS_URL = "https://www.google.com/accounts/OAuthGetAccessToken";

  /**
   * Simple logout servlet.
   */
  @Singleton
  static class LogoutServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws IOException {
      OAuthUtil.invalidateSession(resp);

      // TODO: Implement something less lame.
      resp.setHeader("Content-Type", "text/html");
      resp.getWriter().println("<h3>You've been logged out.</h3>");
      resp.getWriter().flush();
      resp.getWriter().close();
    }
  }

  @Override
  protected void configureServlets() {
    filter("/*").through(OAuthFilter.class);
    serve("/logout").with(LogoutServlet.class);

    bind(HttpClient.class).to(UrlConnectionHttpClient.class).in(Scopes.SINGLETON);
    bind(RequestFactory.class).to(OAuthRequestFactory.class).in(Scopes.SINGLETON);

    // Read Oauth credentials from properties file.
    bindCredentials();
  }

  private void bindCredentials() {
    File file = new File(System.getProperty("splash.credentials.properties"));
    Properties credentials = new Properties();
    try {
      credentials.load(Files.newReader(file, Charset.defaultCharset()));
    } catch (IOException e) {
      addError(e);
    }
    Names.bindProperties(binder(), credentials);
  }

  @Provides @Singleton
  OAuthClient provideOAuthClient() {
    return new OAuthClient(new UrlConnectionHttpClient());
  }

  @Provides @Singleton
  OAuthServiceProvider provideServiceProvider() throws UnsupportedEncodingException {
    OAuthServiceProvider provider;
    provider = new OAuthServiceProvider(
        REQUEST_URL + "?scope=" + URLEncoder.encode(SCOPE, "utf-8"), AUTH_URL, ACCESS_URL);
    return provider;
  }

  @Provides @PortableRequestScoped
  SessionContext provideSessionContext(OAuthServiceProvider provider, HttpServletRequest req,
      AnonymousSession anonymousSession) {
    // TODO: To avoid this in multiple places, the toplevel
    // WebServlet module should provide a filter the first checks for anonymous
    // requests and avoids entering this or the AuthFilter code at all.
    if (OAuthUtil.isAuthenticated(req)) {
      return new OAuthSessionContext(OAuthUtil.createAccessorFromCookies(provider, req));
    }
    return anonymousSession;
  }

  @Provides @Named("rpcEndpointUrl") String provideRpcEndpointUrl() {
    return OS_HANDLER;
  }
}
