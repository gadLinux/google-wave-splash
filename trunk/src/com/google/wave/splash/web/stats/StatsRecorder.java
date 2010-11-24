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

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.MapMaker;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

/**
 * Collects and renders global and request-based stats.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 * @author David Byttow
 */
@Singleton
class StatsRecorder {
  private static int MAX_REQUESTS = 100;

  /**
   * Represents a profiled request.
   */
  static class ProfiledRequest {
    String uri;
    TimerTree tree;

    ProfiledRequest(String uri, TimerTree tree) {
      this.uri = uri;
      this.tree = tree;
    }
  }

  private final StatsRenderer renderer;
  private volatile int queueSize = 0;
  private final Queue<ProfiledRequest> profiledRequests =
      new ConcurrentLinkedQueue<ProfiledRequest>();

  private final ConcurrentMap<String, Measurement> measurements =
      new MapMaker().makeComputingMap(new Function<String, Measurement>() {
        @Override
        public Measurement apply(String key) {
          return new Measurement();
        }
      });


  @Inject
  StatsRecorder(StatsRenderer renderer) {
    this.renderer = renderer;
  }

  /**
   * Records a single incident of measure and duration in millis.
   */
  void record(String name, int duration) {
    measurements.get(name).sample(duration);
  }

  /**
   * Records a single incident of measure and duration in millis with threshold.
   */
  void record(String name, int duration, int threshold) {
    Measurement m = measurements.get(name);
    m.sample(duration);
    m.setThreshold(threshold);
  }

  /**
   * Records an http request call tree.
   */
  void recordRequest(TimerTree tree, String requestURI) {
    if (profiledRequests.offer(new ProfiledRequest(requestURI, tree))) {
      // NOTE(dhanji): volatile incr is not atomic, but writes are, so this is *good enough*
      queueSize++;
    }


    // Remove items from the queue while there are items to remove.
    if (queueSize > MAX_REQUESTS) {
      profiledRequests.poll();
      // NOTE(dhanji): volatile decr is not atomic, but writes are, so this is *good enough*
      queueSize--;
    }
  }

  @Override
  public String toString() {
    return renderer.renderHtml(ImmutableMap.copyOf(measurements),
        ImmutableList.copyOf(profiledRequests));
  }
}
