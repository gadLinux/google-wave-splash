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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.wave.splash.data.Memcache;
import com.google.wave.splash.data.ProfileStore;
import com.google.wave.splash.text.Markup;
import com.google.wave.splash.web.stats.Stat;
import com.google.wave.splash.web.stats.Stats;
import com.google.wave.splash.web.stats.Timed;
import com.google.wave.splash.web.template.WaveRenderer;
import com.google.wave.api.Blip;
import com.google.wave.api.ParticipantProfile;
import com.google.wave.api.Participants;
import com.google.wave.api.Wavelet;

import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Fetches a wavelet, diffs it, transforms it to html wrapped inside a json packet
 *  and returns the result as a client action.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
@Singleton
public class WaveletUpdateRpc {
  private final static Logger log = Logger.getLogger(WaveletUpdateRpc.class.getName());

  @Stat(name = "wavelet-fetch-cache-misses",
      help = "Number of cache misses when fetching wavelets.")
  private static volatile int waveletFetchCacheMisses;

  @Stat(name = "wavelet-fetch-cache-hits",
      help = "Number of cache hits when fetching wavelets.")
  private static volatile int waveletFetchCacheHits;

  static {
    Stats.trackClass(WaveletUpdateRpc.class);
  }

  private final Memcache memcache;

  private final ProfileStore profileStore;
  private final WaveRenderer waveRenderer;
  private final FetchProfilesRpc fetchProfilesRpc;

  public static final long LATEST_VERSION = -1L;
  private final Provider<RemoteWaveService> waveServiceProvider;

  @Inject
  public WaveletUpdateRpc(Memcache memcache, ProfileStore profileStore, 
      WaveRenderer waveRenderer, FetchProfilesRpc fetchProfilesRpc,
      Provider<RemoteWaveService> waveServiceProvider) {
    this.memcache = memcache;

    this.profileStore = profileStore;
    this.waveRenderer = waveRenderer;
    this.fetchProfilesRpc = fetchProfilesRpc;
    this.waveServiceProvider = waveServiceProvider;
  }

  /**
   * Uses cache first, only falls back to fetch if needed. Fast.
   */
  @Timed(threshold = 300)
  public Collection<ClientAction> smartFetch(WaveId waveId, WaveletId waveletId, int page) {
    String waveIdAsString = waveId.getId();

    // Try to fetch latest available version in cache.
    Wavelet snapshot = memcache.retrieve(waveIdAsString);

    if (null == snapshot) {
      // Cache miss, goto full fetch
      log.fine("Cache miss for " + waveIdAsString);
      ++waveletFetchCacheMisses;
      return fetch(waveId, waveletId);
    } else {
      ++waveletFetchCacheHits;
    }

    log.fine("Smart fetch succeeded for " + waveIdAsString);
    return renderWave(snapshot, page);
  }

  /**
   * Complete fetch, ignores cache and doesn't render. Intended to update the cache
   * withe the latest version.
   */
  @Timed
  public Collection<ClientAction> prefetch(WaveId waveId, WaveletId waveletId) {
    return difference(waveId, waveletId, true, true, false, LATEST_VERSION);
  }

  /**
   * Complete fetch for latest wave, ignores cache.
   */
  public Collection<ClientAction> fetch(WaveId waveId, WaveletId waveletId) {
    return difference(waveId, waveletId, true, true, true, LATEST_VERSION);
  }

  /**
   * Complete fetch, Fetch latest wave first, compares with cache
   *  and returns diffs only.
   */
  public Collection<ClientAction> fetchDiff(WaveId waveId, WaveletId waveletId,
      long clientWaveVersion) {
    return difference(waveId, waveletId, false, false, true, clientWaveVersion);
  }

  private Collection<ClientAction> difference(WaveId waveId, WaveletId waveletId,
      boolean ignoreDiff, boolean showHeader, boolean render, long clientWaveVersion) {
    Wavelet wavelet = RpcUtil.getSafely(fetchWavelet(waveId, waveletId));
    if (wavelet == null) {
      if (!render) {
        return ImmutableList.of();
      }
      return ImmutableList.of(waveRenderer.renderNotFound().waveId(waveId.serialise()));
    }

    // Fetch the profiles for this wave.
    fetchProfiles(wavelet);

    // We should use the fresh version if either the flag is set or the client
    // version is bogus (LATEST_VERSION)
    ignoreDiff = ignoreDiff || (LATEST_VERSION == clientWaveVersion);

    String waveIdAsString = waveId.getId();
    String waveKey;
    if (ignoreDiff) {
      waveKey = waveIdAsString;
    } else {
      waveKey = computeWaveKey(waveIdAsString, clientWaveVersion);
    }
    Wavelet oldWavelet = memcache.retrieve(waveKey);

    // If nothing changed since our last cache entry, do nothing.
    if (!ignoreDiff && isNewer(wavelet, oldWavelet)) {
      return ImmutableList.of();
    }

    // Update the latest version in the cache for this wave id, and also
    // cache the wave@version.
    Map<String, Object> waves = ImmutableMap.<String, Object>of(
        computeWaveKey(waveIdAsString, wavelet.getLastModifiedTime()), wavelet,
        waveIdAsString, wavelet);
    memcache.storeAll(waves);

    if (!render) {
      // This is just a prefetch, so don't render anything below.
      return ImmutableList.of();
    }

    if (!ignoreDiff) {
      return renderDiff(oldWavelet, wavelet);
    }
    // We do not want to diff, so just statically render the new wavelet.
    return renderWave(wavelet, 0);
  }

  @Timed(threshold = 200)
  private void fetchProfiles(Wavelet wavelet) {
    fetchProfilesRpc.fetchProfiles(wavelet.getParticipants());
  }

  @Timed(threshold = 300)
  Collection<ClientAction> renderDiff(Wavelet oldWavelet, Wavelet wavelet) {
    List<ClientAction> actions = Lists.newArrayList();
    // TODO(dhanji): cache the profiles, we don't need to keep fetching them.
    ClientAction headerAction = waveRenderer.renderHeader(loadProfiles(wavelet.getParticipants()));
    String waveIdAsString = wavelet.getWaveId().serialise();
    headerAction.waveId(waveIdAsString);
    actions.add(headerAction);

    // Generate delete-blip actions by diffing with memory store.
    if (oldWavelet != null) {
      computeDeletedBlips(wavelet, oldWavelet, actions);
    }
    computeAddedBlips(wavelet, oldWavelet, wavelet.getRootBlip(), actions);

    // Move the client's wave version forward.
    actions.add(new ClientAction("update-wave-version")
        .waveId(waveIdAsString)
        .version(wavelet.getLastModifiedTime())
    );

    return actions;
  }

  @Timed
  Collection<ClientAction> renderWave(Wavelet wavelet, int page) {
    ImmutableList.Builder<ClientAction> builder = ImmutableList.builder();
    String waveIdAsString = wavelet.getWaveId().serialise();
    builder.add(waveRenderer.render(wavelet, page).waveId(waveIdAsString));
    // TODO(dhanji): cache the profiles, we don't need to keep fetching them.
    ClientAction headerAction =
        waveRenderer.renderHeader(loadProfiles(wavelet.getParticipants()));
    headerAction.waveId(waveIdAsString);
    builder.add(headerAction);
    return builder.build();
  }

  @Timed
  List<ParticipantProfile> loadProfiles(Participants participants) {
    ImmutableList.Builder<ParticipantProfile> result = ImmutableList.builder();
    Map<String, ParticipantProfile> profiles = profileStore.getProfiles(participants);
    for (String address : participants) {
      result.add(profiles.get(address));
    }
    return result.build();
  }

  @Timed(threshold = 300)
  ListenableFuture<Wavelet> fetchWavelet(WaveId waveId, WaveletId waveletId) {
    return waveServiceProvider.get().fetchWavelet(waveId, waveletId);
  }

  /**
   * Generates add actions for each blip in the given wavelet. The JS client determines
   * whether or not an add op should update-in-place.
   */
  private void computeAddedBlips(Wavelet wavelet, Wavelet oldWavelet, Blip rootBlip,
      List<ClientAction> actions) {
    boolean shouldAdd = true;

    // Only add new blip if it's newer!
    if (null != oldWavelet) {
      Blip oldRoot = oldWavelet.getRootBlip();
      shouldAdd = (null == oldRoot) || isNewer(rootBlip, oldRoot);
    }

    if (shouldAdd) {
      ClientAction rootAction = toAction(rootBlip, null, wavelet.getTitle());
      actions.add(rootAction);
    }
    addChildren(rootBlip, oldWavelet, actions);
  }

  /**
   * Diffs wavelet with old wavelet and adds delete blip actions for missing blips
   * to the given list of client actions. This way we can generate a delete events for
   * any two arbitrary versions of a wavelet.
   */
  private void computeDeletedBlips(Wavelet wavelet, Wavelet oldWavelet,
      List<ClientAction> actions) {
    Set<String> blips = wavelet.getBlips().keySet();

    for (Map.Entry<String, Blip> blipSnapshot : oldWavelet.getBlips().entrySet()) {
      if (!blips.contains(blipSnapshot.getKey())) {
        // This blip was deleted.
        actions.add(new ClientAction("delete-blip")
            .blipId('#' + Markup.toDomId(blipSnapshot.getKey()))
        );
      }
    }
  }

  /**
   * Creates a key for storing this wave based on the last modified time of the wavelet
   * and the wave id.
   */
  private static String computeWaveKey(String waveIdAsString, long version) {
    return waveIdAsString + "/" + version;
  }

  /**
   * Returns true if toTest is newer than (not same age as) fromCache.
   */
  private static boolean isNewer(Blip toTest, Blip fromCache) {
    return toTest.getLastModifiedTime() > fromCache.getLastModifiedTime();
  }

  /**
   * @return True if {@code wavelet} is newer than, or exactly the same
   *  age as {@code oldWavelet}.
   */
  private static boolean isNewer(Wavelet wavelet, Wavelet oldWavelet) {
    return oldWavelet != null
        && oldWavelet.getLastModifiedTime() >= wavelet.getLastModifiedTime();
  }

  private void addChildren(Blip blip, Wavelet oldWavelet, List<ClientAction> actions) {
    List<Blip> children = blip.getChildBlips();
    int numberOfChildren = children.size();
    for (int i = 0; i < numberOfChildren; i++) {
      Blip child = children.get(i);
      boolean shouldAdd = true;

      // Check if this blip is really newer.
      if (null != oldWavelet) {
        Blip oldBlip = oldWavelet.getBlip(child.getBlipId());
        shouldAdd = (null == oldBlip) || isNewer(child, oldBlip);
      }

      if (shouldAdd) {
        ClientAction action = toAction(child, blip.getBlipId(), "");

        // all subsequent children are rendered indented. This is in keeping with
        // our funky flattened blip tree scheme.
        if (i > 0) {
          action.indent();
        }

        actions.add(action);
      }

      // recursively...
      addChildren(child, oldWavelet, actions);
    }
  }

  /**
   * Takes a blip, a parent id and generates an add blip action for the client.
   * If title is specified, this is a root blip.
   */
  public ClientAction toAction(Blip blipData, String parent, String title) {
    String renderedBlip = waveRenderer.toHtml(blipData, title);

    // NOTE: new blip ids should not be selectors!!!
    ClientAction action = new ClientAction("add-blip")
        .blipId(Markup.toDomId(blipData.getBlipId()))
        .html(renderedBlip)
        .version(blipData.getLastModifiedTime());

    if (null != parent) {
      action.parent("#" + Markup.toDomId(parent));
    }
    return action;
  }
}
