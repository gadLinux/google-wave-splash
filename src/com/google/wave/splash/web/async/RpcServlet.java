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

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.wave.splash.rpc.ClientAction;
import com.google.wave.splash.rpc.Rpc;
import com.google.wave.splash.web.Browser;
import com.google.wave.splash.web.stats.Timing;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This is an ajax responder that handles rpcs by handing them off to an
 * internal list of {@link RpcHandler}s.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 * @author David Byttow
 */
@Singleton
public class RpcServlet extends HttpServlet {
  private final Map<String, RpcHandler> handlers;
  private final Gson gson;
  private final Timing timing;
  private final Logger log = Logger.getLogger(RpcServlet.class.getName());

  @Inject
  public RpcServlet(@Rpc Map<String, RpcHandler> handlers, @Browser Gson gson, Timing timing) {
    this.handlers = handlers;
    this.gson = gson;
    this.timing = timing;
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    List<ClientAction> actions = Lists.newArrayList();
    RpcHandler.Params params = new RequestWrapper(request);
    String rpcs[] = params.get("rpc").split("[,]+");

    if (rpcs.length == 0) {
      String message = "Invalid rpc argument" + params.get("rpc");
      response.sendError(400, message);
      log.info(message);
    }

    // Multiplex the called rpcs.
    for (String rpc : rpcs) {
      RpcHandler handler = handlers.get(rpc);
      if (handler == null) {
        log.warning("Unknown RPC: " + rpc);
        continue;
      }

      // Time taken for each RPC.
      long start = System.currentTimeMillis();
      handler.call(params, actions);
      timing.record(rpc, System.currentTimeMillis() - start);
    }

    long start = System.currentTimeMillis();
    String json = gson.toJson(actions);
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    response.getWriter().write(json);
    timing.record("gson.toJson", System.currentTimeMillis() - start);

    response.getWriter().flush();
    response.getWriter().close();
  }
}
