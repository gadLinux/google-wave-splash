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

import com.google.wave.splash.rpc.ClientAction;

import java.util.List;
import java.util.Set;

/**
 * Interface for ajax-based, asynchronous rpcs that fetch data and result in a
 * list of {@link com.google.wave.splash.rpc.ClientAction}s.
 *
 * @author David Byttow
 */
public interface RpcHandler {
  /**
   * Simple interface for providing parameters.
   *
   * @author David Byttow
   */
  interface Params {
    /**
     * @return the value of the given key.
     */
    String get(String name);

    /**
     * @return set of all parameter names.
     */
    Set<String> nameSet();
  }

  /**
   * Services a given request with the specified parameters. Implementors
   * should optionally fill in {@link com.google.wave.splash.rpc.ClientAction}
   * responses for the client.
   *
   * @param params the parameters passed with the rpc.
   * @param responses output list of responses.
   */
  void call(Params params, List<ClientAction> responses);
}
