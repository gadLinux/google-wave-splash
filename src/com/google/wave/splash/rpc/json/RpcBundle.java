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
package com.google.wave.splash.rpc.json;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.wave.api.JsonRpcResponse;
import com.google.wave.api.impl.GsonFactory;
import com.google.wave.splash.rpc.RpcParam;
import com.google.wave.splash.rpc.RpcUtil;
import com.google.wave.api.OperationRequest;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class used to handle building requests sent to Wave.
 *
 * @author David Byttow
 */
public class RpcBundle {
  private static final Logger LOG = Logger.getLogger(RpcBundle.class.getName());

  private static final Type REQUEST_LIST_TYPE = new TypeToken<List<Object>>(){}.getType();

  @SuppressWarnings("unused")
  private static class BasicRequest {
    private final String id;
    private final String method;
    private final Map<String, Object> params;

    BasicRequest(String id, String method, Map<String, Object> params) {
      this.id = id;
      this.method = method;
      this.params = params;
    }
  }

  private final RpcClient client;
  private final Gson gson;
  private final List<Object> operationList;

  static RpcBundle ofOneRequest(RpcClient client, Gson gson,
      String method, RpcParam... params) {
    return new RpcBundle(client, gson,
        ImmutableList.<Object>of(new BasicRequest("op", method, RpcParam.toMap(params))));
  }

  RpcBundle(RpcClient client, Gson gson) {
    this.client = client;
    this.gson = gson;
    this.operationList = Lists.newArrayList();
  }

  private RpcBundle(RpcClient client, Gson gson,
      List<Object> operationList) {
    this.client = client;
    this.gson = gson;
    this.operationList = operationList;
  }

  /**
   * Adds a new rpc request to the queue.
   * @param method the rpc method to invoke.
   * @param params list of arguments for the method.
   */
  public void addRequest(String method, RpcParam... params) {
    String id = "op" + (operationList.size() + 1);
    operationList.add(new BasicRequest(id, method, RpcParam.toMap(params)));
  }

  /**
   * Adds a new OperationRequest to the queue.
   */
  public void addRequest(OperationRequest request) {
    operationList.add(request);
  }

  /**
   * Send the operations synchronously.
   */
  public void send() {
    String responseString = RpcUtil.getSafely(sendAsync());
    if (LOG.isLoggable(Level.FINE)) {
      LOG.fine("responseString from RpcBundle.send: " + responseString);
    }

    if (null == responseString) {
      LOG.severe("send() RPC appears to have failed--no JSON reply.");
      return;
    }

    List<JsonRpcResponse> responses;
    if (responseString.startsWith("[")) {
      responses = gson.fromJson(responseString, GsonFactory.JSON_RPC_RESPONSE_LIST_TYPE);
    } else {
      responses = ImmutableList.of(gson.fromJson(responseString, JsonRpcResponse.class));
    }
    for (JsonRpcResponse response : responses) {
      if (response.isError()) {
        LOG.severe("RpcBundle.send got an error response for operation " + response.getId() + ": "
            + response.getErrorMessage() );

      }
    }
  }

  /**
   * Sends the operations asynchronously.
   * @return the future result.
   */
  public ListenableFuture<String> sendAsync() {
    String json = gson.toJson(operationList, REQUEST_LIST_TYPE);
    return client.makeRawRequest(json);
  }

  /**
   * @return size of the bundle.
   */
  public int size() {
    return operationList.size();
  }
}
