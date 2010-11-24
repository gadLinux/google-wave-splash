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

import com.google.wave.splash.text.Markup;

/**
 * Represents sampling of measurements.
 *
 * @author David Byttow
 */
class Measurement {
  private int lastDelta;
  private int average;
  private int high;
  private int low;
  private int total;
  private int numSamples;
  private int threshold;

  Measurement() {
    this.low = Integer.MAX_VALUE;
    this.high = 0;
  }

  /**
   * Samples with a new delta.
   *
   * @param delta the duration of the current sample.
   */
  synchronized void sample(int delta) {
    lastDelta = delta;
    ++numSamples;
    low = Math.min(delta, low);
    high = Math.max(delta, high);
    total += delta;
    average = total / numSamples;
  }

  /**
   * @return the last recorded delta.
   */
  int getLastDelta() {
    return lastDelta;
  }

  int getHigh() {
    return high;
  }

  int getAverage() {
    return average;
  }

  /**
   * @return threshold time considered "too slow"
   */
  int getThreshold() {
    return threshold;
  }

  void setThreshold(int threshold) {
    this.threshold = threshold;
  }

  @Override
  public String toString() {
    if (numSamples == 0) {
      return "";
    } else if (numSamples == 1) {
      return String.format("(" + Markup.formatMillis(total) + ")");
    }

    return String.format("(%s, %s, %s, %s, %s)",
        numSamples, Markup.formatMillis(average), Markup.formatMillis(low),
        Markup.formatMillis(high), Markup.formatMillis(total));
  }
}
