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
package com.google.wave.splash.data;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.wave.api.ParticipantProfile;

import java.util.Collection;
import java.util.Map;

/**
 * Implements a profile store.
 *
 * @author David Byttow
 */
@Singleton
public class ProfileStore {
  private final Memcache memcache;
  private final FakeProfileGenerator profileGenerator;

  @Inject
  ProfileStore(Memcache memcache, FakeProfileGenerator profileGenerator) {
    this.memcache = memcache;
    this.profileGenerator = profileGenerator;
  }

  /**
   * Retrieves participant profiles from a collection of participant ids.
   *
   * @param participantIds the participants to retrieve.
   * @return map of participant profiles.
   */
  public Map<String, ParticipantProfile> getProfiles(Collection<String> participantIds) {
    Map<String, ParticipantProfile> profiles =
        Maps.newHashMapWithExpectedSize(participantIds.size());
    for (String id : participantIds) {
      profiles.put(id, getProfile(id));
    }
    return profiles;
  }

  /**
   * Stores profile information about a given participant by id.
   */
  public void putProfile(String participantId, ParticipantProfile profile) {
    memcache.store(getKey(participantId), profile);
  }

  private ParticipantProfile getProfile(String participantId) {
    String key = getKey(participantId);

    ParticipantProfile profile = memcache.retrieve(key);
    if (profile != null) {
      return profile;
    }

    return profileGenerator.generateProfile(participantId);
  }

  private static String getKey(String participantId) {
    return "p/" + participantId;
  }
}
