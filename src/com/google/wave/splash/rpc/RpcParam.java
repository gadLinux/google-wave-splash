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

import com.google.common.collect.Maps;

import java.util.Collections;
import java.util.Map;

/**
 * Represents an argument sent via RPC.
 *
 * @author David Byttow
 */
public class RpcParam {
  /**
   * Constructs a map from a list of parameters.
   */
  public static Map<String, Object> toMap(RpcParam... params) {
    Map<String, Object> map;
    if (params.length > 0) {
      map = Maps.newHashMapWithExpectedSize(params.length);
      for (RpcParam p : params) {
        map.put(p.getName(), p.getValue());
      }
    } else {
      map = Collections.emptyMap();
    }
    return map;
  }

  private final String name;
  private final Object value;

  public static RpcParam of(String name, Object value) {
    return new RpcParam(name, value);
  }

  private RpcParam(String name, Object value) {
    this.name = name;
    this.value = value;
  }

  public String getName() {
    return name;
  }

  public Object getValue() {
    return value;
  }
}
