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
import com.google.wave.splash.rpc.BlipEditRpc;
import com.google.wave.splash.rpc.ClientAction;
import com.google.wave.splash.text.Markup;
import com.google.wave.splash.web.stats.Timing;

import org.waveprotocol.wave.model.id.WaveId;

import java.util.List;
import java.util.logging.Logger;

/**
 * Handles any client side edits of a wave, including new replies, new waves, etc., if
 * there are any.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 * @author David Byttow
 */
@Singleton
public class EditHandler implements RpcHandler  {
  private final BlipEditRpc blipUpdateRpc;
  private final Timing timing;

  private final Logger log = Logger.getLogger(EditHandler.class.getName());

  @Inject
  public EditHandler(BlipEditRpc blipUpdateRpc, Timing stats) {
    this.blipUpdateRpc = blipUpdateRpc;
    this.timing = stats;
  }

  @Override
  public void call(Params params, List<ClientAction> responses) {
    String waveIdAsString = params.get("waveId");
    if (Strings.isNullOrEmpty(waveIdAsString)) {
      log.fine("Edit RPC: client did not provide a wave id. No waves are open.");
      return;
    }

    WaveId waveId = WaveId.deserialise(waveIdAsString);
    checkForEdits(params, waveId);
  }

  private void checkForEdits(Params params, WaveId waveId) {
    // Check for edits.
    for (String name : params.nameSet()) {
      // TODO: Add helpers to parse and pull out blip ids for
      // these internal ops.
      if (name.startsWith("editblip_")) {
        String blipId = Markup.toBlipId(name.substring(9));
        String content = params.get(name);
        if (content == null) {
          continue;
        }
        long blipUpdateTime = System.currentTimeMillis();
        blipUpdateRpc.applyEdit(waveId, blipId, content);
        timing.record("blip.applyEdit", System.currentTimeMillis() - blipUpdateTime);

      } else if (name.startsWith("newblip_")) {
        String parentId = Markup.toBlipId(name.substring(4));
        String content = params.get(name);
        if (content == null) {
          continue;
        }
        long blipReplyTime = System.currentTimeMillis();
        blipUpdateRpc.newReply(waveId, parentId, content);
        timing.record("blip.newReply", System.currentTimeMillis() - blipReplyTime);
      }
    }
  }
}
