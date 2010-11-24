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
import com.google.wave.splash.text.DiffMatchPatch;

import java.util.LinkedList;
import java.util.List;

/**
 * A utility class for performing differencing between sets of documents.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
class Ops {
  private static final DiffMatchPatch diffMatchPatch = new DiffMatchPatch();

  /**
   * Given two arbitrary Documents, generates an op stream
   * that represents the diff between them. I.e. applying the
   * op stream to doc1 will produce doc2
   */
  public static List<StandardDocument.Op> diff(Document doc1, Document doc2) {
    LinkedList<DiffMatchPatch.Diff> diffs =
        diffMatchPatch.diff_main(doc1.toString(), doc2.toString());

    List<StandardDocument.Op> ops = Lists.newArrayList();
    // For each insert or delete, generate an insert or delete op.
    int index = 0, version = doc1.version();
    for (DiffMatchPatch.Diff diff : diffs) {
      int piece = diff.text.length();

      if (!diff.operation.equals(DiffMatchPatch.Operation.EQUAL)) {
        StandardDocument.OpKind kind = diff.operation.equals(DiffMatchPatch.Operation.INSERT)
            ? StandardDocument.OpKind.INSERT
            : StandardDocument.OpKind.DELETE;

        if (kind == StandardDocument.OpKind.DELETE) {
          piece = 0;
        }

        Document.Op op = kind == Document.OpKind.INSERT
            ? Document.Op.insert(index, diff.text, version)
            : Document.Op.delete(index, diff.text, version);
        ops.add(op);
        version++;
      }

      index += piece;
    }

    return ops;
  }
}
