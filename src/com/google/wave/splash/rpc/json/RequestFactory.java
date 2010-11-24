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

/**
 * Interface for creating json-based requests.
 *
 * @author David Byttow
 */
public interface RequestFactory {
  /**
   * Create a new signed request.
   *
   * @param body the post body.
   * @return the future result.
   */
  ListenableFuture<String> makeSignedRequest(String body);
}
