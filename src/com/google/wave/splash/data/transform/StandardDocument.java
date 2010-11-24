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
package com.google.wave.splash.data.transform;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import java.util.List;

/**
 * A standard document is one that contains the entire version history
 * of ops from the canonical starting point of a document. In other words
 * it should be used as the canonical representation of a document in
 * memory.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
class StandardDocument extends Document {
  private final StringBuilder document = new StringBuilder();

  private final List<Op> opStream = Lists.newArrayList();

  public StandardDocument(String document) {
    apply(Op.insert(0, document, 0));
  }

  @Override public List<Op> ops() {
    return opStream;
  }

  @Override public int version() {
    return opStream.size();
  }

  @Override public void apply(Op op) {
    Preconditions.checkArgument(op.applyAt <= version(), "Can't apply future ops");

    if (version() > op.applyAt) {
      op = op.transform(opStream.subList(op.applyAt, version()));
    }

    // We need to make sure we're deleting what we want to delete.
    op.applyTo(document);
    opStream.add(op);
  }

  @Override public String toString() {
    return document.toString();
  }
}
