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

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A document comprised of an original base string and a timeline of
 * mutation operations to that base string.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 * @author limpbizkit@gmail.com (Jesse Wilson)
 */
public abstract class Document {

  /**
   * Applies a single op to this document.
   *
   * @param op The op to apply, whose apply version must be less
   * than or equal to the version of this document.
   */
  public abstract void apply(Op op);

  /**
   * A convenience method for creating and applying an insert op.
   */
  public void insert(int index, String what, int applyAt) {
    apply(StandardDocument.Op.insert(index, what, applyAt));
  }

  /**
   * Creates a view over the given document as of the specified
   * version.
   * @param version A version that must be lower or equal to the
   * current version of this document.
   *
   * @return A mutable view over this document. All changes are applied
   * to both documents.
   */
  public Document asOf(int version) {
    return new ViewDocument(this, version);
  }

  /**
   * Applies a sequence of ops to this document.
   * @param ops a List of ops in order of increasing version
   */
  public void applyAll(List<Op> ops) {
    for (Op op : ops) {
      apply(op);
    }
  }

  /**
   * @return The current version of the document.
   */
  public abstract int version();

  /**
   * @return A String representation of the document in its current
   * state, with all ops applied.
   */
  @Override public abstract String toString();

  /**
   * @return The entire history of operations against the base string of this
   * document.
   */
  public abstract List<Op> ops();

  /**
   * An enumeration of the types of mutation to the base string that
   * documents support.
   */
  public static enum OpKind { INSERT, DELETE }

  /**
   * A mutation of the document, where to apply it and the version at
   * which it is to be applied. A list of ops, when applied in order
   * can take you from one version to a later version of a document,
   * representing multiple sources of edits to it.
   */
  public static class Op {
    private static AtomicLong nextOpId = new AtomicLong();
    final long id;
    final OpKind kind;
    final int index;
    final String what;
    final int applyAt;

    private Op(long id, OpKind kind, int index, String what, int version) {
      this.id = id;
      this.kind = kind;
      this.index = index;
      this.what = what;
      this.applyAt = version;
    }

    /**
     * Applies this op against the provided document, mutating it
     * appropriately. Any transformations to fast-forward this op
     * to the documents must already have been performed.
     *
     * @param document The mutable string to apply this op to.
     */
    public void applyTo(StringBuilder document) {
      switch (kind) {
        case INSERT:
          document.insert(index, what);
          break;
        case DELETE:
          if (what.equals(document.substring(index, index + what.length()))) {
            document.delete(index, index + what.length());
          } else {
            System.out.println("Non-matching delete op");
          }
          break;
      }
    }

    @Override
    public String toString() {
      return "Op{" +
          "kind=" + kind +
          ", index=" + index +
          ", what='" + what + '\'' +
          ", applyAt=" + applyAt +
          '}';
    }

    /**
     * Transforms this op against a timeline of provided ops, ensuring that
     * the application of this op will result in a consistent, synchronous
     * state by all appliers.
     *
     * @param ops A history of document ops that have already been applied to
     * a base document, against whom to transform this op
     * @return The transformed edition of this op
     */
    public Op transform(List<StandardDocument.Op> ops) {
      if (ops.isEmpty()) {
        return this;
      }

      int offset = 0;
      int version = 0;
      for (StandardDocument.Op op : ops) {
        if ((index + offset) < op.index) {
          continue; // TODO: offset differently for deletes
        }

        offset += op.what.length() * signum(op);
        version = op.applyAt;
      }
      return new Op(id, kind, index + offset, what, version + 1);
    }

    /**
     * Returns the signum of the op (insert or delete).
     */
    private static int signum(Op op) {
      return (OpKind.INSERT.equals(op.kind) ? 1 : -1);
    }

    /**
     * The same as {@linkplain #transform(Document.Op)}
     * except that it transforms against a single op.
     */
    public StandardDocument.Op transform(Op op) {
      return transform(ImmutableList.of(op));
    }

    /**
     * A factory method for producing an insertion op against this document.
     */
    public static StandardDocument.Op insert(int index, String what, int applyAt) {
      return new Op(nextOpId.getAndIncrement(), OpKind.INSERT, index, what, applyAt);
    }


    /**
     * A factory method for producing a deletion op against this document.
     */
    public static StandardDocument.Op delete(int index, String what, int applyAt) {
      return new Op(nextOpId.getAndIncrement(), OpKind.DELETE, index, what, applyAt);
    }

    /**
     * Applies the given version to this op. Used internally during the fast-forward
     * process.
     */
    public Op withVersion(int version) {
      return new Op(id, kind, index, what, version);
    }

    /**
     * @return a unique id for this op across all ops against this document.
     */
    public long getId() {
      return id;
    }
  }
}
