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
package com.google.wave.splash.web.stats;

/**
 * Interface for snapshot timing, calls to start and stop must be symmetrical.
 *
 * @author David Byttow
 */
public interface Timing {
  /**
   * Record a single snapshot.
   *
   * @param name the name of the snapshot.
   * @param duration duration of time.
   */
  void record(String name, long duration);

  /**
   * Start the timer by name.
   *
   * @param name the name of this timer.
   */
  void start(String name);

  /**
   * Stop the current timer, this should be symmetrical with start.
   *
   * @param name the name of this timer.
   * @param threshold duration considered "too slow".
   * @param requestUri the URI of the current request or null if not in a
   *     request.
   */
  void stop(String name, int threshold, String requestUri);
}
