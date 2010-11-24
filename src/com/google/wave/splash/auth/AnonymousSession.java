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
package com.google.wave.splash.auth;

import com.google.inject.Singleton;

import javax.servlet.http.HttpServletRequest;

/**
 * Implements the {@link SessionContext} as an anonymous user.
 *
 * @author (David Byttow)
 */
@Singleton
public class AnonymousSession implements SessionContext {
  static final String PUBLIC_ACCOUNT = "public@a.gwave.com";

  private static final String ANONYMOUS_ACCESS_PARAM = "anon_access";

  /**
   * @return true if the given request should be anonymous.
   */
  public static boolean isAnonymousRequest(HttpServletRequest req) {
    return "true".equals(req.getParameter(ANONYMOUS_ACCESS_PARAM));
  }

  @Override
  public boolean isAuthenticated() {
    return false;
  }

  @Override
  public String getSessionKey() {
    return PUBLIC_ACCOUNT;
  }

  @Override
  public String getUserAddress() {
    return PUBLIC_ACCOUNT;
  }
}
