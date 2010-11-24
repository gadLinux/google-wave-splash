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
package com.google.wave.splash.data.serialize;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.JsonParser;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.wave.splash.web.Browser;
import com.google.wave.api.SearchResult.Digest;
import com.google.wave.api.impl.GsonFactory;

import java.lang.reflect.Type;
import java.util.ArrayList;

/**
 * Guice bindings for serialization classes.
 *
 * @author David Byttow
 */
public class SerializeModule extends AbstractModule {
  @Override
  protected void configure() {
  }

  @Provides @Singleton
  Gson provideGson() {
    GsonFactory factory = new GsonFactory();
    factory.registerTypeAdapter(Digest.class, new DigestInstanceCreator());
    return factory.create();
  }

  @Provides @Singleton @Browser
  Gson provideBrowserGson() {
    return new GsonBuilder().registerTypeAdapter(Digest.class, new DigestInstanceCreator())
        .disableHtmlEscaping()
        .create();
  }

  @Provides @Singleton
  JsonSerializer provideJsonSerializer(Gson gson) {
    return new JsonSerializer(gson, new JsonParser());
  }

  private static class DigestInstanceCreator implements InstanceCreator<Digest> {
    @Override
    public Digest createInstance(Type type) {
      return new Digest("Untitled", "", "", new ArrayList<String>(), 0, 0, 0);
    }
  }
}
