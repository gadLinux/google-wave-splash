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

import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.wave.splash.rpc.ClientAction;
import com.google.wave.splash.rpc.FeedRpc;
import com.google.wave.splash.text.Markup;
import com.google.wave.splash.web.stats.Timing;
import com.google.wave.splash.wprime.Index;

import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

/**
 * This is an ajax responder that performs searches and is geared for instant
 * results. So we prefer cached data over live data every time. If we deliver
 * stale results, the Async responder will "catch the user up" with live data.
 *
 * This is essentially the WPrime servlet.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
@Singleton
public class SearchHandler implements RpcHandler {
  private final FeedRpc feedRpc;
  private final Timing timing;

  private final Logger log = Logger.getLogger(SearchHandler.class.getName());

  @Inject
  public SearchHandler(FeedRpc feedRpc, Timing timing) {
    this.feedRpc = feedRpc;
    this.timing = timing;
  }

  @Override
  public void call(Params params, List<ClientAction> actions) {
    // If no query was specified, use the default.
    String query = params.get("query");
    if (Strings.isNullOrEmpty(query)) {
      query = Index.allQuery();
    }

    int startAt = Integer.parseInt(params.get("startAt"));
    int numResults = Integer.parseInt(params.get("numResults"));
    long currentFeedTime = Long.parseLong(params.get("currentFeedTime"));

    loadFeedInto(actions, query, startAt, numResults, currentFeedTime);
  }

  private void loadFeedInto(Collection<ClientAction> actions, String query, int startAt,
                            int numResults, long currentFeedTime) {
    log.info("Performing feed search..."  + query);
    long searchTime = System.currentTimeMillis();
    ClientAction feedAction = feedRpc.updateFeedPage(currentFeedTime, query, startAt, numResults);
    searchTime = System.currentTimeMillis() - searchTime;

    if (searchTime > 0) {
      actions.add(Markup.measure("Search", searchTime));
      timing.record("Search", searchTime);
    }

    // Null feed action means there were no new additions to the feed.
    if (null != feedAction) {
      actions.add(feedAction);
    }
  }

}
