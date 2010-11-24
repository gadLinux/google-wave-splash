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
package com.google.wave.splash.data.serialize;

import com.google.wave.api.Wavelet;

/**
 * Represents the result of fetching a wavelet.
 *
 * @author David Byttow
 */
public class FetchWaveletResult {
  private static final FetchWaveletResult EMPTY = new FetchWaveletResult(null);

  static FetchWaveletResult emptyResult() {
    return EMPTY;
  }

  private final Wavelet wavelet;

  FetchWaveletResult(Wavelet wavelet) {
    this.wavelet = wavelet;
  }

  public boolean hasWavelet() {
    return wavelet != null;
  }

  public Wavelet getWavelet() {
    return wavelet;
  }
}
