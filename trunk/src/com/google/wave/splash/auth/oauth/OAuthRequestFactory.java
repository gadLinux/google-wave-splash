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

import com.google.common.base.Preconditions;
import com.google.common.io.CharStreams;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.google.wave.splash.auth.SessionContext;
import com.google.wave.splash.rpc.json.RequestFactory;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;
import net.oauth.client.OAuthClient;
import net.oauth.http.HttpClient;
import net.oauth.http.HttpMessage;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Creates properly signed JsonRequests.
 *
 * @author David Byttow
 */
class OAuthRequestFactory implements RequestFactory {
  private Logger LOG = Logger.getLogger(OAuthRequestFactory.class.getName());

  private final Provider<SessionContext> sessionProvider;
  private final OAuthClient client;
  private final String rpcEndpointUrl;
  private final HttpClient httpClient;
  private final String oauthKey;
  private final String oauthSecret;

  @Inject
  OAuthRequestFactory(Provider<SessionContext> sessionProvider, OAuthClient client,
      @Named("rpcEndpointUrl") String rpcEndpointUrl, HttpClient httpClient,
      @Named("splash.oauth.key") String oauthKey,
      @Named("splash.oauth.secret") String oauthSecret) {
    this.sessionProvider = sessionProvider;
    this.client = client;
    this.rpcEndpointUrl = rpcEndpointUrl;
    this.httpClient = httpClient;
    this.oauthKey = oauthKey;
    this.oauthSecret = oauthSecret;
  }

  @Override
  public ListenableFuture<String> makeSignedRequest(String body) {
    SessionContext session = sessionProvider.get();
    InputStream bodyStream;
    try {
      bodyStream = new ByteArrayInputStream(body.getBytes("UTF-8"));
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException(e);
    }

    String result = null;
    try {
      InputStream output;
      if (session.isAuthenticated()) {
        output = getSignedResult(session, bodyStream);
      } else {
        output = getUnsignedResult(body, bodyStream);
      }
      result = CharStreams.toString(new InputStreamReader(output, HttpMessage.DEFAULT_CHARSET));
    } catch (URISyntaxException e) {
      LOG.warning(e.toString());
    } catch (OAuthException e) {
      LOG.warning(e.toString());
    } catch (IOException e) {
      LOG.severe(e.toString());
    }
    return Futures.immediateFuture(result);
  }

  private InputStream getSignedResult(SessionContext session, InputStream bodyStream)
      throws OAuthException, IOException, URISyntaxException {
    Preconditions.checkState(session instanceof OAuthSessionContext, "not an oauth session");
    OAuthSessionContext oauthSessionContext = (OAuthSessionContext) session;
    OAuthAccessor accessor = oauthSessionContext.getAccessor();
    OAuthMessage message = accessor.newRequestMessage("POST", rpcEndpointUrl, null, bodyStream);
    message.getHeaders().add(new Entry(HttpMessage.CONTENT_TYPE, "application/json"));
    message.getHeaders().add(new Entry("oauth_version", "1.0"));
    return client.invoke(message, net.oauth.ParameterStyle.BODY).getBodyAsStream();
  }

  private InputStream getUnsignedResult(String body, InputStream bodyStream)
      throws IOException, URISyntaxException, OAuthException {
    URL url = new URL(createOAuthUrlString(body, rpcEndpointUrl, oauthKey, oauthSecret));
    HttpMessage request = new HttpMessage("POST", url, bodyStream);
    request.headers.add(new Entry(HttpMessage.CONTENT_TYPE, "application/json"));
    request.headers.add(new Entry("oauth_version", "1.0"));
    return httpClient.execute(request, Collections.<String, Object>emptyMap()).getBody();
  }

  /**
   * Creates a URL that contains the necessary OAuth query parameters for the
   * given JSON string.
   */
  private static String createOAuthUrlString(String jsonBody, String rpcServerUrl,
      String consumerKey, String consumerSecret)
      throws IOException, URISyntaxException, OAuthException {
    OAuthMessage message = new OAuthMessage("POST", rpcServerUrl,
        Collections.<Map.Entry<String, String>>emptyList());

    // Compute the hash of the body.
    byte[] rawBody = jsonBody.getBytes(HttpMessage.DEFAULT_CHARSET);
    byte[] hash = DigestUtils.sha(rawBody);
    byte[] encodedHash = Base64.encodeBase64(hash);
    message.addParameter("oauth_body_hash", new String(encodedHash, HttpMessage.DEFAULT_CHARSET));

    // Add other parameters.
    OAuthConsumer consumer = new OAuthConsumer(null, "google.com" + ":" + consumerKey,
        consumerSecret, null);
    consumer.setProperty(OAuth.OAUTH_SIGNATURE_METHOD, OAuth.HMAC_SHA1);
    OAuthAccessor accessor = new OAuthAccessor(consumer);
    message.addRequiredParameters(accessor);

    // Construct the resulting URL.
    StringBuilder sb = new StringBuilder(rpcServerUrl);
    char connector = '?';
    for (Map.Entry<String, String> p : message.getParameters()) {
      if (!p.getValue().equals(jsonBody)) {
        sb.append(connector);
        sb.append(URLEncoder.encode(p.getKey(), HttpMessage.DEFAULT_CHARSET));
        sb.append('=');
        sb.append(URLEncoder.encode(p.getValue(), HttpMessage.DEFAULT_CHARSET));
        connector = '&';
      }
    }
    return sb.toString();
  }

  private static class Entry implements Map.Entry<String, String> {
    private final String key;
    private String value;

    Entry(String key, String value) {
      this.key = key;
      this.value = value;
    }

    @Override
    public String getKey() {
      return key;
    }

    @Override
    public String getValue() {
      return value;
    }

    @Override
    public String setValue(String value) {
      this.value = value;
      return value;
    }
  }
}
