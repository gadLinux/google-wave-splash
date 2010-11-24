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
import com.google.wave.splash.auth.SessionContext;

import net.oauth.OAuthAccessor;

/**
 * Implements the {@link SessionContext} interface backed by OAuth.
 *
 * @author David Byttow
 */
public class OAuthSessionContext implements SessionContext {
  private final OAuthAccessor accessor;

  public OAuthSessionContext(OAuthAccessor accessor) {
    this.accessor = accessor;
  }

  @Override
  public boolean isAuthenticated() {
    return accessor.accessToken != null;
  }

  @Override
  public String getSessionKey() {
    Preconditions.checkState(isAuthenticated(), "not authenticated");
    // TODO: Use an id provided by the profile server instead.
    return String.valueOf(accessor.accessToken.hashCode());
  }

  @Override
  public String getUserAddress() {
    if (!isAuthenticated()) {
      return "public@a.gwave.com";
    }
    // TODO: Get this info properly.
    return "nobody@googlewave.com";
  }

  public OAuthAccessor getAccessor() {
    return accessor;
  }
}
