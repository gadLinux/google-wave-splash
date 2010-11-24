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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.wave.splash.web.stats.Timed;
import com.google.wave.api.Blip;
import com.google.wave.api.BlipData;
import com.google.wave.api.BlipThread;
import com.google.wave.api.FetchProfilesResult;
import com.google.wave.api.OperationQueue;
import com.google.wave.api.ParticipantProfile;
import com.google.wave.api.SearchResult;
import com.google.wave.api.Wavelet;
import com.google.wave.api.SearchResult.Digest;
import com.google.wave.api.impl.WaveletData;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Monolithic deserializer for various rpc calls.
 *
 * @author David Byttow
 */
@Singleton
public class JsonSerializer {
  private static final Logger LOG = Logger.getLogger(JsonSerializer.class.getName());
  private static final Type DIGEST_LIST_TYPE =
      new TypeToken<List<SearchResult.Digest>>(){}.getType();

  private final Gson gson;
  private final JsonParser parser;

  @Inject
  public JsonSerializer(Gson gson, JsonParser parser) {
    this.gson = gson;
    this.parser = parser;
  }

  /**
   * Parses a string of json from a wave.robot.search rpc into a search result
   * with a set of digests.
   *
   * @param query the query used to get the search results.
   * @param json the json to parse.
   * @return a result object, which may not contain results.
   */
  public SearchResult parseSearchResult(String query, String json) {
    JsonObject dataObject = getDataObject(json);
    SearchResult result = new SearchResult(query);
    if (dataObject == null) {
      LOG.warning("Search returned empty result: " + query);
      return result;
    }
    if (dataObject != null) {
      JsonArray digestElements =
        dataObject.getAsJsonObject("searchResults").getAsJsonArray("digests");
      List<Digest> digests = gson.fromJson(digestElements, DIGEST_LIST_TYPE);
      for (Digest digest : digests) {
        result.addDigest(digest);
      }
    }
    return result;
  }

  /**
   * Parses a string of json from a wave.robot.fetchWave into a result.
   *
   * @param json the json to parse.
   * @return the result object, which may not contain a wavelet.
   */
  @Timed
  public FetchWaveletResult parseFetchWaveletResult(String json) {
    JsonObject dataObject = getDataObject(json);
    if (dataObject == null || !dataObject.has("waveletData")) {
      LOG.warning("Fetch returned empty result.");
      return FetchWaveletResult.emptyResult();
    }

    JsonObject waveletDataJson = dataObject.getAsJsonObject("waveletData");
    OperationQueue operationQueue = new OperationQueue();
    WaveletData waveletData = gson.fromJson(waveletDataJson, WaveletData.class);

    Map<String, Blip> blips = Maps.newHashMap();
    HashMap<String, BlipThread> threads = Maps.newHashMap();
    Wavelet wavelet = Wavelet.deserialize(operationQueue, blips, threads, waveletData);

    JsonObject threadMap = dataObject.getAsJsonObject("threads");
    if (null != threadMap) {
      for (Map.Entry<String, JsonElement> entries : threadMap.entrySet()) {
        JsonObject threadElement = entries.getValue().getAsJsonObject();
        int location = threadElement.get("location").getAsInt();

        List<String> blipIds = Lists.newArrayList();
        for (JsonElement blipId : threadElement.get("blipIds").getAsJsonArray()) {
          blipIds.add(blipId.getAsString());
        }

        BlipThread thread = new BlipThread(entries.getKey(), location, blipIds, blips);
        threads.put(thread.getId(), thread);
      }
    }

    JsonObject blipMap = dataObject.getAsJsonObject("blips");
    for (Map.Entry<String, JsonElement> entries : blipMap.entrySet()) {
      BlipData blipData = gson.fromJson(entries.getValue(), BlipData.class);
      Blip blip = Blip.deserialize(operationQueue, wavelet, blipData);
      blips.put(blipData.getBlipId(), blip);
    }

    return new FetchWaveletResult(wavelet);
  }

  @Timed
  public FetchProfilesResult parseFetchProfilesResult(String json) {
    JsonObject dataObject = getDataObject(json);
    if (dataObject == null || !dataObject.has("fetchProfilesResult")) {
      LOG.warning("Fetch profiles returned empty result.");
      return new FetchProfilesResult(ImmutableList.<ParticipantProfile>of());
    }

    // TODO: Parse the result when this service works.
    return new FetchProfilesResult(ImmutableList.<ParticipantProfile>of());
  }

  private JsonObject getDataObject(String json) {
    if (json == null) {
      return null;
    }
    JsonArray array = parser.parse(json).getAsJsonArray();
    if (array.size() == 0) {
      return null;
    }
    return array.get(0).getAsJsonObject().getAsJsonObject("data");
  }
}
