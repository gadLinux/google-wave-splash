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
package com.google.wave.splash;

/**
 * A flags-style options object, which is for ripple-wide options.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public interface Options {
  /**
   * True if all templates are reloaded upon request, for refresh dev mode.
   */
  boolean alwaysReloadTemplates();

  /**
   * Number of characters that is considered one page in a paging set, breaks
   * up the wave into pages along this guide.
   */
  int charsPerPage();

  /**
   * @return true if Ripple should render header buttons.
   */
  boolean enableHeaderButtons();

  /**
   * @return true if we should use playful, fake avatars, otherwise the system
   *     will use regular defaults.
   */
  boolean enableFakeAvatars();

  /**
   * @return true if access to the full client is enabled.
   */
  boolean enableFullClient();

  /**
   * @return true if access to the mobile client is enabled.
   */
  boolean enableMobileClient();

  /**
   * @return true if Ripple should fetch profiles remotely.
   */
  boolean enableProfileFetching();

  /**
   * Returns whether we're in production mode or not. Generally in production
   * mode templates are compressed and scripts are minified. In development by
   * contrast, templates are reloaded every request.
   */
  boolean productionMode();

  /**
   * @return true if Ripple should not support writing or editing of waves.
   */
  boolean readOnly();

  /**
   * @return true if we allow a login directly from wave.
   */
  boolean showLoginLinkInEmbed();

  /**
   * @return whether or not to time methods marked @Timed
   */
  boolean useTimers();

  /**
   * @return true if we should not make use of threads and other appengine
   * compat.
   */
  boolean enableAppengineMode();
}
