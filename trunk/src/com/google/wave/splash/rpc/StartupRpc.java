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

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.wave.api.JsonRpcConstant.ParamsProperty;
import com.google.wave.api.OperationType;
import com.google.wave.api.ParticipantProfile;
import com.google.wave.api.ProtocolVersion;
import com.google.wave.splash.auth.SessionContext;

/**
 * Rpc that should be called at startup when rpcs are meant to be made on
 * behalf of a new user.
 *
 * @author David Byttow
 */
@Singleton
public class StartupRpc {
  private final OperationRequestClient requestClient;

  @Inject
  public StartupRpc(OperationRequestClient requestClient) {
    this.requestClient = requestClient;
  }

  public ParticipantProfile startup(SessionContext session) {
    Preconditions.checkState(ProtocolVersion.DEFAULT.isGreaterThanOrEqual(ProtocolVersion.V2_2),
        "Robot Protocol version must be at least 0.22 or higher");

    OperationRequestClient.OperationRequestBatch batch = requestClient.newRequestBatch();
    batch.addRobotRequest(OperationType.ROBOT_NOTIFY,
        RpcParam.of(ParamsProperty.PROTOCOL_VERSION.key(),
            ProtocolVersion.DEFAULT.getVersionString()),
        RpcParam.of(ParamsProperty.CAPABILITIES_HASH.key(), ""));
    if (session.isAuthenticated()) {
      // TODO: Fetch the user profile.
    } else {
      // Anonymous user.
    }

    // TODO: Make this completely asynchronous.
    batch.apply();
    return new ParticipantProfile("nobody", "", "");
  }
}
