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

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.wave.splash.data.Memcache;
import com.google.wave.api.Blip;
import com.google.wave.api.OperationType;
import com.google.wave.api.Wavelet;
import com.google.wave.api.JsonRpcConstant.ParamsProperty;
import com.google.wave.api.OperationRequest.Parameter;
import com.google.wave.api.impl.DocumentModifyAction;
import com.google.wave.api.impl.DocumentModifyAction.ModifyHow;
import com.google.wave.splash.text.ContentUnrenderer;
import com.google.wave.splash.text.ContentUnrenderer.UnrenderedBlip;

import org.waveprotocol.wave.model.id.WaveId;

import java.util.logging.Logger;

/**
 * Rpc class for replying to and editing blips.
 *
 * @author David Byttow
 */
@Singleton
public class BlipEditRpc {
  private static Logger LOG = Logger.getLogger(BlipEditRpc.class.getName());

  private final OperationRequestClient requestClient;
  private final Memcache memcache;

  @Inject
  public BlipEditRpc(OperationRequestClient requestClient, Memcache memcache) {
    this.requestClient = requestClient;
    this.memcache = memcache;
  }

  public void newReply(WaveId waveId, String parentId, String content) {
    Wavelet wavelet = memcache.retrieve(waveId.getId());
    if (wavelet == null) {
      // update failed, wavelet has been evicted?
      return;
    }

    Blip parent = wavelet.getBlip(parentId);
    // TODO(dhanji): Fix this so the defensive code isn't needed.
    if (parent == null) {
      return;
    }
    Blip newBlip = parent.reply();
    newBlip.appendMarkup(content);

    OperationRequestClient.OperationRequestBatch batch = requestClient.newRequestBatch();
    batch.addWaveBasedRequest(OperationType.BLIP_CREATE_CHILD, waveId.serialise(),
        newBlip.getWaveletId().serialise(), parent.getBlipId(),
        Parameter.of(ParamsProperty.BLIP_DATA, newBlip.serialize()));

    batch.addWaveBasedRequest(OperationType.DOCUMENT_APPEND_MARKUP,
        waveId.serialise(), newBlip.getWaveletId().serialise(), newBlip.getBlipId(),
        Parameter.of(ParamsProperty.CONTENT, content));

    batch.apply();
  }

  /**
   * Updates the given blip with some content (complete replace).
   */
  public void applyEdit(WaveId waveId, String blipId, String content) {
    // TODO: Clean this up -- this whole dance of workers is
    // because we cannot modify or add ops to the current wavelets that we have.
    Wavelet wavelet = memcache.retrieve(waveId.getId());
    if (wavelet == null) {
      // update failed, wavelet has been evicted?
      return;
    }
    UnrenderedBlip actualContent = new ContentUnrenderer().unrender(content);
    // Fix our in-memory state first.
    Blip blip = wavelet.getBlip(blipId);
    blip.all().delete();
    blip.appendMarkup(content);

    // TODO(anthonybaxter): This needs to be completely rewritten. Wheeeeee.

    // Now just replace the blip's contents with the new contents.
    OperationRequestClient.OperationRequestBatch batch = requestClient.newRequestBatch();
    batch.addWaveBasedRequest(OperationType.DOCUMENT_MODIFY, waveId.serialise(),
        blip.getWaveletId().serialise(), blip.getBlipId(),
        Parameter.of(ParamsProperty.MODIFY_ACTION,
            new DocumentModifyAction(ModifyHow.REPLACE, ImmutableList.of(actualContent.contents),
            null, null, null, false)));

    batch.apply();
    
  }
}
