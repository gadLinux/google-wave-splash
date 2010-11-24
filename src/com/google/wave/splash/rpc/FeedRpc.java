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

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.internal.ImmutableMap;
import com.google.wave.splash.Options;
import com.google.wave.splash.RequestScopeExecutor;
import com.google.wave.splash.auth.SessionContext;
import com.google.wave.splash.data.Memcache;
import com.google.wave.splash.data.serialize.JsonSerializer;
import com.google.wave.splash.web.stats.Timed;
import com.google.wave.splash.web.template.Templates;
import com.google.wave.splash.wprime.Index;
import com.google.wave.api.OperationType;
import com.google.wave.api.SearchResult;
import com.google.wave.api.SearchResult.Digest;

import org.waveprotocol.wave.model.id.IdUtil;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;

import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

/**
 * Handles querying and caching feeds from the backend servers via rpc.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 * @author David Byttow
 */
@Singleton
public class FeedRpc {
  private static final Logger LOG = Logger.getLogger(FeedRpc.class.getName());

  private final Provider<SessionContext> sessionProvider;
  private final Templates templates;
  private final Memcache memcache;
  private final RequestScopeExecutor jobQueue;
  private final WaveletUpdateRpc waveletUpdateRpc;
  private final OperationRequestClient requestClient;
  private final JsonSerializer serializer;
  private final Provider<Index> indexProvider;

  private final boolean enableAppengineMode;

  @Inject
  public FeedRpc(Provider<SessionContext> sessionProvider, Templates templates, Memcache memcache,
      RequestScopeExecutor jobQueue, WaveletUpdateRpc waveletUpdateRpc,
      OperationRequestClient requestClient, JsonSerializer serializer,
      Provider<Index> indexProvider, Options options) {
    this.sessionProvider = sessionProvider;
    this.templates = templates;
    this.memcache = memcache;
    this.jobQueue = jobQueue;
    this.waveletUpdateRpc = waveletUpdateRpc;
    this.requestClient = requestClient;
    this.serializer = serializer;
    this.indexProvider = indexProvider;

    this.enableAppengineMode = options.enableAppengineMode();
  }

  /**
   * Prefetches waves and stores them in memcache for the given list of feed
   * items.
   *
   * @param feed A list of feed items (digests).
   */
  public void prefetchAsync(List<SearchResult.Digest> feed) {
    for (SearchResult.Digest digest : feed) {
      final WaveId id = WaveId.deserialise(digest.getWaveId());
      jobQueue.submit(new Runnable() {
        @Override
        public void run() {
          LOG.info("Asynchronous prefetch: " + id);
          // Ignore the result, it is cached for us in memcache anyway.
          waveletUpdateRpc.prefetch(id, getConversationWaveletId(id.getDomain()));
        }
      });
    }
  }

  /**
   * Gets the conversational wavelet id for the given domain.
   *
   * @param domain the domain for this wave.
   * @return the conversational wavelet id.
   */
  public static WaveletId getConversationWaveletId(String domain) {
    return new WaveletId(domain, IdUtil.CONVERSATION_ROOT_WAVELET);
  }

  /**
   * Returns an RPC result that contains all the feed items starting from
   * the last update the client had (or a max of 10 history items, whichever
   * is smaller).
   *
   * @param from timestamp of the last feed item to start from, exclusive. If this value
   *   is 0L, then we must always fetch a new feed from the wave backends.
   * @param query the search query
   * @param startAt the index from which to start fetching results, inclusive
   * @param numResults The maximum number of results to return
   */
  public ClientAction updateFeedPage(long from, String query, int startAt, int numResults) {
    Index index = indexProvider.get();
    if (index.isReady() && !index.isAllQuery(query)) {
      return render(index.search(query));
    }

    // Cache for later.
    String feedKey = generateFeedKey(query);
    List<SearchResult.Digest> feed = memcache.retrieve(feedKey);

    if (null == feed || from == 0L) {
      feed = query(query, index, feedKey, feed, startAt, numResults);
    } else {
      // If something is already in memcache, use that first, then stream results.
      memcache.remove(feedKey);
    }

    // If it's still null, then the remote search itself failed...
    if (feed == null) {
      return null;
    }

    if (!enableAppengineMode) {
      prefetchAsync(feed);
    }
    return render(feed);
  }

  @Timed(threshold = 100)
  private ClientAction render(Collection<Digest> feed) {
    String filledFeed = templates.process(Templates.FEED_TEMPLATE, ImmutableMap.of("feed", feed));
    return new ClientAction("update-feed").html(filledFeed);
  }

  @Timed(threshold = 1500)
  private List<Digest> query(String query, Index index,
                             String feedKey, List<Digest> oldFeed,
                             int startAt, int numResults) {
    String json = search(query, startAt, numResults);
    SearchResult searchResult = serializer.parseSearchResult(query, json);
    if (searchResult == null) {
      return null;
    }

    List<SearchResult.Digest> feed = searchResult.getDigests();

    // Store in local index.
    if (index.isAllQuery(query)) {
      index.index(feed);
    }

    memcache.store(feedKey, feed);
    return feed;
  }

  @Timed(threshold = 1000)
  private String search(String query, int startAt, int numResults) {
    OperationRequestClient.OperationRequestBatch batch = requestClient.newRequestBatch();
    batch.addRobotRequest(OperationType.ROBOT_SEARCH,
        RpcParam.of("query", query),
        RpcParam.of("index", Integer.toString(startAt)),
        RpcParam.of("numResults", Integer.toString(numResults)));
    return RpcUtil.getSafely(batch.sendAsync());
  }

  /**
   * Fetches a fresh feed, diffs it with the last cached feed for this user/search
   * and computes a set of feed client actions to send back.
   *
   * @param query The query to fetch from the wave backends
   * @param from Most recent lastModified time of the waves in the client's current feed
   * @return A diff of client actions, one action with the new feed, and additional actions
   * to delete any moved feed items.
   */
  public ClientAction diffFeed(String query, long from) {
    // TODO(dhanji): on hold for now.
    return null;
  }

  private final String generateFeedKey(String query) {
    return "feed/" + sessionProvider.get().getSessionKey() + "/" + query;
  }
}
