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
package com.google.wave.splash;

import com.google.common.collect.Lists;
import com.google.inject.Scopes;
import com.google.inject.servlet.ServletModule;
import com.google.inject.servlet.SessionScoped;
import com.google.wave.splash.auth.oauth.OAuthModule;
import com.google.wave.splash.data.serialize.SerializeModule;
import com.google.wave.splash.rpc.RemoteWaveService;
import com.google.wave.splash.rpc.WaveServiceAdapter;
import com.google.wave.splash.rpc.json.RpcMethods;
import com.google.wave.splash.web.WebServletModule;
import com.google.wave.splash.web.stats.StatsModule;
import com.google.wave.api.OperationType;

import org.mvel2.MVEL;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Top-level module for Splash server.
 *
 * @author dhanji@gmail.com (Dhanji Prasanna)
 * @author David Byttow
 */
class SplashModule extends ServletModule {
  private static int THREAD_POOL_SIZE = 10;

  static class OsfeRpcMethods implements RpcMethods {
    private final List<String> methodList;

    OsfeRpcMethods() {
      // OSFE requires all methods to begin with the service name, "wave".
      this.methodList = Lists.newArrayListWithCapacity(OperationType.values().length);
      for (OperationType type : OperationType.values()) {
        methodList.add("wave." + type.method());
      }
    }

    @Override
    public String getMethodName(OperationType operationType) {
      return methodList.get(operationType.ordinal());
    }
  }

  @Override
  protected void configureServlets() {
    // Note: this is the default development options, but will be different in LFE.
    Options options = loadOptions();
    bind(Options.class).toInstance(options);

    install(new WebServletModule(options));
    install(new OAuthModule());
    install(new StatsModule(options));
    install(new SerializeModule());

    // Prefetch thread pool, job queue.
    if (options.enableAppengineMode()) {
      bind(ExecutorService.class).toInstance(new DummyExecutorService());
    } else {
      bind(ExecutorService.class).toInstance(Executors.newFixedThreadPool(THREAD_POOL_SIZE));
    }
    bind(RpcMethods.class).to(OsfeRpcMethods.class).in(Scopes.SINGLETON);

    bind(RemoteWaveService.class).toProvider(WaveServiceAdapter.WaveServiceProvider.class)
        .in(PortableRequestScoped.class);

    // We customize the request scope to allow the cross-thread handoff
    // of session context.
    PortableRequestScope portableRequestScope = new PortableRequestScope();
    bind(PortableRequestScope.class).toInstance(portableRequestScope);
    bindScope(PortableRequestScoped.class, portableRequestScope);
  }

  /**
   * loads options from options.properties and binds them behind the Options interface.
   */
  private static Options loadOptions() {
    final ResourceBundle properties =
        ResourceBundle.getBundle(Options.class.getPackage().getName() + ".options");

    return (Options)
        Proxy.newProxyInstance(SplashModule.class.getClassLoader(), new Class<?>[]{Options.class},
            new InvocationHandler() {
              @Override
              public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
                return MVEL.eval(properties.getString(method.getName()));
              }
            }
        );
  }

  /**
   * An executor service that does no work. This is under in environments where
   * starting threads is prohibited (appengine).
   */
  private static class DummyExecutorService extends AbstractExecutorService {

    @Override
    public void shutdown() {
    }

    @Override
    public List<Runnable> shutdownNow() {
      return null;
    }

    @Override
    public boolean isShutdown() {
      return false;
    }

    @Override
    public boolean isTerminated() {
      return false;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit)
        throws InterruptedException {
      return false;
    }

    @Override
    public void execute(Runnable command) {
    }
  }
}
