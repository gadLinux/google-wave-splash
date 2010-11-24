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
package com.google.wave.splash.rpc;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.wave.api.Wavelet;

import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;

/**
 * An interface to fetch wavelets from a remote wave server.
 *
 * @author anthonybaxter@gmail.com (Anthony Baxter)
 */
public interface RemoteWaveService {

  /**
   * Fetches a wavelet from a remote wave server.
   * 
   * @return a Future which wraps the wavelet.
   */
  ListenableFuture<Wavelet> fetchWavelet(WaveId waveId, WaveletId waveletId);

}
