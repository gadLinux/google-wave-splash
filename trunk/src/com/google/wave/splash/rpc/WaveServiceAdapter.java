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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.google.wave.api.WaveService;
import com.google.wave.api.Wavelet;
import com.google.wave.splash.auth.SessionContext;
import com.google.wave.splash.auth.oauth.OAuthSessionContext;

import net.oauth.OAuthAccessor;

import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;

import java.io.IOException;

/**
 * Fetches wavelets using the Google Wave Robot API {@code WaveService} interface.
 * 
 * @author anthonybaxter@gmail.com (Anthony Baxter)
 */
public class WaveServiceAdapter implements RemoteWaveService {

  private final WaveService waveService;
  private final String rpcEndpoint;

  public WaveServiceAdapter(WaveService waveService, String rpcEndpoint) {
    this.waveService = waveService;
    this.rpcEndpoint = rpcEndpoint;
  }

  @Override
  public ListenableFuture<Wavelet> fetchWavelet(WaveId waveId, WaveletId waveletId) {
    try {
      return Futures.immediateFuture(waveService.fetchWavelet(waveId, waveletId, rpcEndpoint));
    } catch (IOException e) {
      return Futures.immediateFailedFuture(e);
    }
  }

  /**
   * Factory for creating WaveService objects.
   */
  public static class WaveServiceProvider implements Provider<RemoteWaveService> {

    private final Provider<SessionContext> sessionContext;
    private final String rpcEndpoint;

    @Inject
    public WaveServiceProvider(Provider<SessionContext> sessionContext,
        @Named("rpcEndpointUrl") String rpcEndpoint) {
      this.sessionContext = sessionContext;
      this.rpcEndpoint = rpcEndpoint;
    }

    @Override
    public RemoteWaveService get() {
      OAuthSessionContext oAuthSessionContext = (OAuthSessionContext) sessionContext.get();
      OAuthAccessor accessor = oAuthSessionContext.getAccessor();
      WaveService waveService = new WaveService();
      waveService.setupOAuth(accessor, rpcEndpoint);
      return new WaveServiceAdapter(waveService, rpcEndpoint);
    }
  }
}
