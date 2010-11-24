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

import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.wave.splash.rpc.json.RpcBundle;
import com.google.wave.splash.rpc.json.RpcClient;
import com.google.wave.splash.rpc.json.RpcMethods;
import com.google.wave.api.OperationRequest;
import com.google.wave.api.OperationType;
import com.google.wave.api.OperationRequest.Parameter;

/**
 * Class that deals at the OperationRequest and OperationType level for sending
 * rpcs.
 *
 * @author David Byttow
 */
@Singleton
public class OperationRequestClient {
  private final RpcClient rpcClient;
  private final RpcMethods methods;

  /**
   * Represents a batch of operation-based requests.
   */
  public static class OperationRequestBatch {
    private final RpcBundle bundle;
    private final RpcMethods methods;

    private OperationRequestBatch(RpcBundle bundle, RpcMethods methods) {
      this.bundle = bundle;
      this.methods = methods;
    }

    /**
     * Adds a new request to the bundle.
     *
     * @param operation the operation type to send.
     * @param params list of params fot he request.
     */
    public void addRobotRequest(OperationType operation, RpcParam... params) {
      String method = methods.getMethodName(operation);
      bundle.addRequest(method, params);
    }

    /**
     * Adds a new wave-based request to the bundle.
     *
     * @param operation the operation type to send.
     * @param params list of params fot he request.
     */
    public void addWaveBasedRequest(OperationType operation, String waveId, String waveletId,
        String blipId, Parameter... params) {
      String method = methods.getMethodName(operation);
      OperationRequest request = new OperationRequest(method, "op" + bundle.size(),
          waveId, waveletId, blipId, params);
      bundle.addRequest(request);
    }


    /**
     * Send the operations synchronously.
     */
    public void apply() {
      bundle.send();
    }

    /**
     * Sends the operations asynchronously.
     * @return the future result.
     */
    public ListenableFuture<String> sendAsync() {
      return bundle.sendAsync();
    }
  }

  @Inject
  public OperationRequestClient(RpcClient rpcClient, RpcMethods methods) {
    this.rpcClient = rpcClient;
    this.methods = methods;
  }

  public OperationRequestBatch newRequestBatch() {
    return new OperationRequestBatch(rpcClient.newBundle(), methods);
  }
}
