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

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.google.wave.api.OperationType;
import com.google.wave.api.Wavelet;
import com.google.wave.splash.data.serialize.FetchWaveletResult;
import com.google.wave.splash.data.serialize.JsonSerializer;
import com.google.wave.splash.web.stats.Timing;

import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;

/**
 * A remote wave service that uses Json-RPC to fetch wavelet data from the
 * Google Wave Data API.
 * 
 * @author anthony@gmail.com (Anthony Baxter)
 */
public class JsonRpcWaveService implements RemoteWaveService {
  
  private final OperationRequestClient requestClient;
  private final JsonSerializer serializer;
  private final Timing timing;

  @Inject
  public JsonRpcWaveService(OperationRequestClient requestClient, JsonSerializer serializer,
      Timing timing) {
    this.requestClient = requestClient;
    this.serializer = serializer;
    this.timing = timing;
  }

  @Override
  public ListenableFuture<Wavelet> fetchWavelet(WaveId waveId, WaveletId waveletId) {
        final long time = System.currentTimeMillis();
    OperationRequestClient.OperationRequestBatch batch = requestClient.newRequestBatch();
    batch.addRobotRequest(OperationType.ROBOT_FETCH_WAVE,
        RpcParam.of("waveId", waveId.serialise()),
        RpcParam.of("waveletId", waveletId.serialise()));
    final ListenableFuture<String> future = batch.sendAsync();
    return Futures.compose(future, new Function<String, Wavelet>() {
      @Override
      public Wavelet apply(String json) {
        timing.record("wave.robot.fetchWave", System.currentTimeMillis() - time);
        FetchWaveletResult result = serializer.parseFetchWaveletResult(json);
        return result != null ? result.getWavelet() : null;
      }
    });
  }
}
