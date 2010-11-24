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

import com.google.inject.ImplementedBy;

import java.util.Map;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
@ImplementedBy(InMemoryMemcache.class)
public interface Memcache {
  /**
   * Stores given object associated with given key.
   */
  void store(String key, Object value);

   /**
   * Stores all given objects at associated keys. Useful
   * for batching store operations.
   */
  void storeAll(Map<String, Object> values);

  /**
   * Retrieves a previously stored object by its key. If no object is
   *  associated with the given key, null is returned.
   */
  <V> V retrieve(String key);

  /**
   * Removes a previously stored object by its key. If no object is
   *  associated with the given key, nothing happens.
   *
   * @return True if a key was successfully removed
   */
  boolean remove(String key);

  /**
   * @return A String representation of the contents of the cache.
   * May not be accurate at time of snapshot.
   */
  String toString();

  /**
   * Evicts (removes) all cache contents. Not all implementors need to
   * provide this function.
   */
  void flush();
}
