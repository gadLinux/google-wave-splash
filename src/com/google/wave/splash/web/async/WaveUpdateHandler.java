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
package com.google.wave.splash.web.async;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.wave.splash.rpc.ClientAction;
import com.google.wave.splash.rpc.FeedRpc;
import com.google.wave.splash.rpc.WaveletUpdateRpc;
import com.google.wave.splash.web.stats.Timed;
import com.google.wave.splash.web.stats.Timing;

import org.waveprotocol.wave.model.id.WaveId;

import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

/**
 * This is the background update handler. The client sends us a batch of rpcs and we
 * return a batch of responses (not necessarily 1:1). For example, the client may tell
 * us add a blip, and we tell it delete a blip, in reply. This also loads new items
 * for the feed. Note that this is a slower responding servlet as it always tries to
 * get fresh data, rather than respond immediately.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
@Singleton
public class WaveUpdateHandler implements RpcHandler  {
  private final WaveletUpdateRpc waveletUpdateRpc;
  private final Timing timing;

  private final Logger log = Logger.getLogger(WaveUpdateHandler.class.getName());

  @Inject
  public WaveUpdateHandler(WaveletUpdateRpc waveletUpdateRpc, Timing timing) {
    this.waveletUpdateRpc = waveletUpdateRpc;
    this.timing = timing;
  }

  @Override
  public void call(Params params, List<ClientAction> actions) {
    String waveIdAsString = params.get("waveId");
    if (Strings.isNullOrEmpty(waveIdAsString)) {
      log.fine("Update RPC: client did not provide a wave id. No waves are open.");
      return;
    }
    WaveId waveId = WaveId.deserialise(waveIdAsString);

    long clientWaveVersion = WaveletUpdateRpc.LATEST_VERSION;
    String waveVersionAsString = params.get("waveVersion");
    if (Strings.isNullOrEmpty(waveVersionAsString)) {
      log.fine("Client did not provide a wave version during update. Assuming full fetch");
    } else {
      clientWaveVersion = Long.valueOf(waveVersionAsString);
    }

    // Timed fetch of wavelet from backends.
    diffWaveletInto(waveId, clientWaveVersion, actions);
  }

  @Timed
  void diffWaveletInto(WaveId waveId, long clientWaveVersion, Collection<ClientAction> actions) {
    actions.addAll(waveletUpdateRpc.fetchDiff(waveId,
        FeedRpc.getConversationWaveletId(waveId.getDomain()), clientWaveVersion));
  }
}
