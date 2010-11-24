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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.wave.splash.Options;
import com.google.wave.api.ParticipantProfile;

/**
 * Generates placeholder profiles for unloaded participants.
 *
 * @author David Byttow
 */
@Singleton
class FakeProfileGenerator {
  private enum Policy {
    RANDOM_AVATARS,
    ROUND_ROBIN_DEFAULTS,
  }

  private static final int NUM_DEFAULTS = 6;
  private static final int NUM_AVATARS = 7;
  private static final String FALLBACK_IMAGE = "/images/user1.png";

  private final Policy policy;

  private int count = 0;

  @Inject
  FakeProfileGenerator(Options options) {
    this.policy = options.enableFakeAvatars()
        ? Policy.RANDOM_AVATARS
        : Policy.ROUND_ROBIN_DEFAULTS;
  }

  /**
   * Generates a placeholder profile.
   */
  ParticipantProfile generateProfile(String participantId) {
    String key = getKey(participantId);

    String name = participantId.substring(0, participantId.indexOf('@'));
    return new ParticipantProfile(name, getImageUrl(participantId), "");
  }

  private String getImageUrl(String participantId) {
    return policy == Policy.ROUND_ROBIN_DEFAULTS ?
        getFacelessDefault() : getFakeAvatar(participantId);
  }

  private String getFakeAvatar(String participantId) {
    int numLeft = NUM_AVATARS - count;
    if (numLeft == 0) {
      return FALLBACK_IMAGE;
    }

    int hash = participantId.hashCode();
    if (hash < 0) {
      hash = -hash;
    }
    int index = count + (hash % numLeft);
    ++count;
    return "/images/avatar" + (index + 1) + ".png";
  }

  private String getFacelessDefault() {
    int index = count++ % NUM_DEFAULTS;
    return "/images/user" + (index + 1) + ".png";
  }

  private String getKey(String participantId) {
    return "p/" + participantId;
  }
}
