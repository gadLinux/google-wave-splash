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
import com.google.wave.splash.text.Markup;
import com.google.wave.splash.web.stats.Timed;
import com.google.wave.splash.web.stats.Timing;

import org.waveprotocol.wave.model.id.WaveId;

import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

/**
 * This handler serves waves via ajax, it tries to be fast rather than live.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
@Singleton
public class WaveOpenHandler implements RpcHandler  {
  private final WaveletUpdateRpc waveletUpdateRpc;
  private final Timing timing;

  private final Logger log = Logger.getLogger(WaveOpenHandler.class.getName());

  @Inject
  public WaveOpenHandler(WaveletUpdateRpc waveletUpdateRpc, Timing timing) {
    this.waveletUpdateRpc = waveletUpdateRpc;
    this.timing = timing;
  }

  @Override
  public void call(Params params, List<ClientAction> actions) {
    String waveIdAsString = params.get("waveId");
    int page = Integer.parseInt(params.get("page"));

    // TODO(dhanji): When we support more options, turn this into an "options" object.
    String showHeaderAsString = params.get("showHeader");
    boolean showHeader = Strings.isNullOrEmpty(showHeaderAsString)
        ? true : Boolean.valueOf(showHeaderAsString);

    // Timed fetch of wavelet from backends.
    long waveFetchTime = System.currentTimeMillis();
    // There is a security hole here -- as we do no ACL checking on the wave
    // itself, it should be cached by user as well.
    loadWaveletInto(waveIdAsString, page, actions, showHeader);
    waveFetchTime = System.currentTimeMillis() - waveFetchTime;

    actions.add(Markup.measure("Wave Open", waveFetchTime));
    timing.record("Wave Open", waveFetchTime);
  }

  @Timed
  void loadWaveletInto(String waveIdAsString, int page, Collection<ClientAction> actions,
      boolean showHeader) {
    if (Strings.isNullOrEmpty(waveIdAsString)) {
      log.fine("wave-open RPC: Client did not provide a wave id. No waves are open.");
    } else {
      WaveId waveId = WaveId.deserialise(waveIdAsString);
      actions.addAll(waveletUpdateRpc.smartFetch(waveId,
          FeedRpc.getConversationWaveletId(waveId.getDomain()), page
      ));
    }
  }
}
