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
package com.google.wave.splash.wprime;

import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import com.google.inject.servlet.SessionScoped;
import com.google.wave.api.SearchResult.Digest;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * This is an edge-index, that tokenizes the "All" search for a user, then answers
 * queries by returning matching wave ids in the all search. This provides an extremely
 * fast initial response by searching the title and snippet of digests in the "all"
 * search. Eventually we may change this to be a proper prefix-tree index.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
@SessionScoped
public class Index {
  /** Represents a query token for all digests. */
  private static final String ALL_QUERY = "with:me";

  private static final Logger LOG = Logger.getLogger(Index.class.getName());

  private final Multimap<String, String> tokenToWaves = Multimaps.newSetMultimap(
      Maps.<String, Collection<String>>newHashMap(),
      new Supplier<Set<String>>() {
        @Override
        public Set<String> get() {
          return Sets.newHashSet();
        }
      });

  private final Map<String, Digest> corpus = Maps.newHashMap();

  /**
   * Build an index of the given digests.
   *
   * @param digests the collection of digests.
   */
  public void index(List<Digest> digests) {
    // First update corpus.
    update(digests);

    for (Digest digest : digests) {
      tokenize(digest.getTitle(), digest.getWaveId());
      tokenize(digest.getSnippet(), digest.getWaveId());
    }
  }

  /**
   * Perform a search.
   *
   * @param token the token to search for.
   * @return the collection of matching digests.
   */
  public Collection<Digest> search(String token) {
    token = token.toLowerCase();

    // return everything in cache.
    if (isAllQuery(token)) {
      return corpus.values();
    }

    Collection<String> resultIds = tokenToWaves.get(token);
    List<Digest> results = Lists.newArrayListWithExpectedSize(resultIds.size());
    for (String resultId : resultIds) {
      results.add(corpus.get(resultId));
    }

    return results;
  }

  /**
   * Update the corpus with this list of digests.
   *
   * @param digests the list of digests to add to the corpus.
   */
  public void update(List<Digest> digests) {
    LOG.info("Updating current corpus: " + digests.size());
    for (Digest digest : digests) {
      corpus.put(digest.getWaveId(), digest);
    }
  }

  /**
   * @return true if the corpus has been updated.
   */
  public boolean isReady() {
    return !corpus.isEmpty();
  }

  private void tokenize(String string, String waveId) {
    String[] tokens = string.split("[ ,;:\\.]+");

    for (String token : tokens) {
      // normalize.
      token = token.toLowerCase();
      tokenToWaves.put(token, waveId);
    }
  }

  /**
   * @param query Any raw query token.
   * @return True if this will execute the all query.
   */
  public boolean isAllQuery(String query) {
    return ALL_QUERY.equals(query);
  }

  /**
   * @return The query token representing the all query.
   */
  public static String allQuery() {
    return ALL_QUERY;
  }
}
