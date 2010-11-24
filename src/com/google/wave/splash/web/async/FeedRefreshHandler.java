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
import com.google.wave.splash.web.stats.Timed;
import com.google.wave.splash.wprime.Index;

import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

/**
 * This is an ajax responder that refreshes the feed by fetching everything newer
 * than the newest lastModified sent by the client.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
@Singleton
public class FeedRefreshHandler implements RpcHandler {
  private final FeedRpc feedRpc;

  private final Logger log = Logger.getLogger(FeedRefreshHandler.class.getName());

  @Inject
  public FeedRefreshHandler(FeedRpc feedRpc) {
    this.feedRpc = feedRpc;
  }

  @Override
  public void call(Params params, List<ClientAction> actions) {
    // If no query was specified, use the default.
    String query = params.get("query");
    if (Strings.isNullOrEmpty(query)) {
      query = Index.allQuery();
    }

    long currentFeedTime = Integer.parseInt(params.get("currentFeedTime"));
    loadFeedInto(actions, query, currentFeedTime);
  }

  @Timed
  private void loadFeedInto(Collection<ClientAction> actions, String query, long currentFeedTime) {
    log.info("Refreshing feed..."  + query);
    long searchTime = System.currentTimeMillis();
    ClientAction feedAction = feedRpc.diffFeed(query, currentFeedTime);
    searchTime = System.currentTimeMillis() - searchTime;

    if (searchTime > 0) {
      actions.add(Markup.measure("Search", searchTime));
    }

    // Null feed action means there were no new additions to the feed.
    if (null != feedAction) {
      actions.add(feedAction);
    }
  }
}
