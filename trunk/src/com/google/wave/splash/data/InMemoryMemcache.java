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

import com.google.inject.Singleton;
import com.google.inject.internal.MapMaker;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
@Singleton
public class InMemoryMemcache implements Memcache {
  private final ConcurrentMap<String, Object> cache;

  public InMemoryMemcache() {
    this.cache = new MapMaker().makeMap();
  }

  @Override
  public void store(String key, Object value) {
    cache.put(key, value);
  }

  @Override
  public void storeAll(Map<String, Object> values) {
    cache.putAll(values);
  }

  @Override @SuppressWarnings("unchecked")
  public <V> V retrieve(String key) {
    return (V) cache.get(key);
  }

  @Override
  public boolean remove(String key) {
    return null != cache.remove(key);
  }

  @Override
  public String toString() {
    return cache.toString();
  }

  @Override
  public void flush() {
    cache.clear();
  }
}
