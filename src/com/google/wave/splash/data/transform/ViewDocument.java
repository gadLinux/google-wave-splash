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

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;

/**
 * An ancestor branch of a canonical document. View documents are used
 * to apply a series of edits against a historical version of the document.
 * It works by first creating a virtual branch of the document at a given
 * historical version, then each application of an op is transformed against
 * the original document's op history resulting in a change to both the original
 * and the view document. Once completed, the view can be discarded.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 * @author limpbizkit@gmail.com (Jesse Wilson)
 */
final class ViewDocument extends Document {
  private final Document upstream;
  private final int forkVersion;
  private final List<Op> localOps = Lists.newArrayList();
  private final StringBuilder document = new StringBuilder();

  ViewDocument(Document upstream, int forkVersion) {
    this.upstream = upstream;
    this.forkVersion = forkVersion;

    for (Op op : upstream.ops().subList(0, forkVersion)) {
      op.applyTo(document);
    }
  }

  @Override public void apply(Op op) {
    if (version() > op.applyAt) {
      op = op.transform(ops().subList(op.applyAt, version()));
    }
    op.applyTo(document);
    localOps.add(op);

    // now apply the op upstream

    /*
       walk thru original ops and local ops.
       if equal: step
       if not equal: transform all local ops after it
                     that includes my new op!
       finally apply the new op to the original
     */

    List<Op> upstreamOps = upstream.ops().subList(forkVersion, upstream.version());

    int localOpIndex = 0;
    int upstreamOpIndex = 0;
    while (localOpIndex < localOps.size() && upstreamOpIndex < upstreamOps.size()) {
      Op localOp = localOps.get(localOpIndex);
      Op upstreamOp = upstreamOps.get(upstreamOpIndex);
      if (localOp.getId() != upstreamOp.getId()) {
        op = op.transform(upstreamOp);
      } else {
        localOpIndex++;
      }
      upstreamOpIndex++;
    }

    Op opForUpstream = op.withVersion(upstreamOpIndex + 1);
    upstream.apply(opForUpstream);
  }

  @Override public List<Op> ops() {
    List<Op> result = new ArrayList<Op>();
    result.addAll(upstream.ops().subList(0, forkVersion));
    result.addAll(localOps);
    return result;
  }

  @Override public int version() {
    return forkVersion + localOps.size();
  }

  @Override public String toString() {
    return document.toString();
  }
}
