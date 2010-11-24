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

import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.wave.splash.rpc.RpcParam;

/**
 * Class for bundling for making JSON-RPC calls to wave servers.
 *
 * @author David Byttow
 */
@Singleton
public class RpcClient {
  private final RequestFactory factory;
  private final Gson gson;

  @Inject
  public RpcClient(RequestFactory factory, Gson gson) {
    this.factory = factory;
    this.gson = gson;
  }

  /**
   * Makes a raw JSON request.
   *
   * @param json the json to send.
   * @return the future result.
   */
  ListenableFuture<String> makeRawRequest(String json) {
    return factory.makeSignedRequest(json);
  }

  /**
   * Makes a JSON request.
   *
   * @param method name of the method to invoke.
   * @param params the params to pass with the method.
   * @return the future result.
   */
  public ListenableFuture<String> makeRequest(String method, RpcParam... params) {
    return RpcBundle.ofOneRequest(this, gson, method, params).sendAsync();
  }

  /**
   * Creates a new JsonRpc bundle.
   */
  public RpcBundle newBundle() {
    return new RpcBundle(this, gson);
  }
}
