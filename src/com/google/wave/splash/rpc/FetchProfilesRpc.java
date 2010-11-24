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
import com.google.wave.splash.Options;
import com.google.wave.splash.RequestScopeExecutor;
import com.google.wave.splash.data.ProfileStore;
import com.google.wave.splash.data.serialize.JsonSerializer;
import com.google.wave.splash.rpc.OperationRequestClient.OperationRequestBatch;
import com.google.wave.api.FetchProfilesRequest;
import com.google.wave.api.FetchProfilesResult;
import com.google.wave.api.OperationType;
import com.google.wave.api.ParticipantProfile;
import com.google.wave.api.JsonRpcConstant.ParamsProperty;

import java.util.Collection;
import java.util.logging.Logger;

/**
 * Fetches profiles for users.
 *
 * @author David Byttow
 */
@Singleton
public class FetchProfilesRpc {
  private final static Logger LOG = Logger.getLogger(FetchProfilesRpc.class.getName());

  private final ProfileStore profileStore;
  private final OperationRequestClient requestClient;
  private final JsonSerializer serializer;
  private final RequestScopeExecutor jobQueue;
  private final Options options;

  @Inject
  public FetchProfilesRpc(ProfileStore profileStore, OperationRequestClient requestClient,
      JsonSerializer serializer, RequestScopeExecutor jobQueue, Options options) {
    this.profileStore = profileStore;
    this.requestClient = requestClient;
    this.serializer = serializer;
    this.jobQueue = jobQueue;
    this.options = options;
  }

  public void fetchProfiles(final Collection<String> participantIds) {
    if (!options.enableProfileFetching()) {
      return;
    }

    if (participantIds.isEmpty()) {
      return;
    }

    // TODO: Split this up to check the cache rather than
    // continually querying the remote servers.

    // Fetch asynchronously and push back into the store.
    jobQueue.submit(new Runnable() {
      @Override
      public void run() {
        OperationRequestBatch batch = requestClient.newRequestBatch();
        batch.addRobotRequest(OperationType.ROBOT_FETCH_PROFILES,
            RpcParam.of(ParamsProperty.FETCH_PROFILES_REQUEST.key(),
                new FetchProfilesRequest(ImmutableList.copyOf(participantIds))));
        String results = RpcUtil.getSafely(batch.sendAsync());
        FetchProfilesResult result = serializer.parseFetchProfilesResult(results);
        for (ParticipantProfile profile : result.getProfiles()) {
          profileStore.putProfile(profile.getAddress(), profile);
        }
      }
    });
  }
}
