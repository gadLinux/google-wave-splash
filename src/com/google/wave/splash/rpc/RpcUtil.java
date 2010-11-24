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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Rpc utility for unwrapping RPC future results.
 *
 * @author David Byttow
 */
public class RpcUtil {
  private static Logger LOG = Logger.getLogger(RpcUtil.class.getName());

  /**
   * Returns a future result while encapsulating exception handling.
   *
   * @return the result or null if an exception was caught.
   */
  public static <T> T getSafely(Future<T> future) {
    try {
      return future.get();
    } catch (InterruptedException e) {
      LOG.log(Level.SEVERE, "Failed to fetch JsonRPC results", e);
    } catch (ExecutionException e) {
      LOG.log(Level.SEVERE, "Failed to fetch JsonRPC results", e);
    }
    return null;
  }
}
