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

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * Implementation of an http request-scoped timer.
 *
 * @author David Byttow
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
@Singleton
class ThreadLocalTiming implements Timing {
  private final StatsRecorder statsRecorder;
  private final Provider<TimerTree> localTree;

  @Inject
  ThreadLocalTiming(StatsRecorder statsRecorder, Provider<TimerTree> localTree) {
    this.statsRecorder = statsRecorder;
    this.localTree = localTree;
  }

  @Override
  public void record(String name, long duration) {
    statsRecorder.record(name, (int) duration);
  }

  @Override
  public void start(String name) {
    localTree.get().push(name);
  }

  @Override
  public void stop(String name, int threshold, String requestUri) {
    TimerTree tree = localTree.get();
    TimerTree.Node node = tree.pop(name);
    statsRecorder.record(name, node.getMeasurement().getLastDelta(), threshold);

    if (!tree.isTiming()) {
      if (requestUri != null) {
        statsRecorder.recordRequest(tree, requestUri);
      }
    }
  }
}
