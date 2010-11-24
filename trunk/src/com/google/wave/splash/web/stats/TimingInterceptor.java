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
package com.google.wave.splash.web.stats;

import com.google.common.base.Function;
import com.google.common.collect.MapMaker;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentMap;

/**
 * Intercepts method calls that have a {@link Timed} annotation.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 * @author David Byttow
 */
@Singleton
class TimingInterceptor implements MethodInterceptor {

  @Inject private final Timing timing = null;

  private ConcurrentMap<Method, String> nameCache = new MapMaker().makeComputingMap(
      new Function<Method, String>() {
        @Override
        public String apply(Method method) {
          return method.getDeclaringClass().getSimpleName();
        }
      });

  @Override
  public Object invoke(MethodInvocation methodInvocation) throws Throwable {
    Method method = methodInvocation.getMethod();
    Timed timed = method.getAnnotation(Timed.class);
    String name = timed.value();
    if (name.isEmpty()) {
      name = nameCache.get(methodInvocation.getMethod());
    }
    try {
      timing.start(name);
      return methodInvocation.proceed();
    } finally {
      timing.stop(name, timed.threshold(), null);
    }
  }
}
