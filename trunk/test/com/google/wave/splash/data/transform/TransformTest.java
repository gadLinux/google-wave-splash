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

import com.google.wave.splash.text.DiffMatchPatch;

import junit.framework.TestCase;

import java.util.LinkedList;
import java.util.List;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 * @author limpbizkit@gmail.com (Jesse Wilson)
 */
public class TransformTest extends TestCase {

  public final void testSimpleInsertion() {
    Document document = new StandardDocument("hello");

    document.apply(StandardDocument.Op.insert(5, " world", document.version()));
    document.apply(StandardDocument.Op.insert(5, " small", document.version()));

    assertEquals("hello small world", document.toString());
    System.out.println(document);
  }

  public final void testInsertionBackVersion() {
    Document document = new StandardDocument("hello");

    document.apply(StandardDocument.Op.insert(5, " world", document.version()));
    document.apply(StandardDocument.Op.insert(5, " small", document.version() - 1));

    assertEquals("hello world small", document.toString());
    System.out.println(document);
  }

  public final void testSimpleDeletion() {
    Document document = new StandardDocument("hello small world");

    document.apply(StandardDocument.Op.delete(5, " small", document.version()));

    assertEquals("hello world", document.toString());
    System.out.println(document);
  }

  public final void disabled_testSimpleMutation() {
    Document document = new StandardDocument("hello small world");

    document.apply(StandardDocument.Op.insert(5, " tiny", document.version()));
    document.apply(StandardDocument.Op.delete(5, " small", document.version() - 1));

    assertEquals("hello tiny world", document.toString());

    // Now test the balancing stream, where the ops are applied in the
    // opposite order:
    document = new StandardDocument("hello small world");

    document.apply(StandardDocument.Op.delete(5, " small", document.version()));
    document.apply(StandardDocument.Op.insert(5, " tiny", document.version() - 1));

    assertEquals("hello tiny world", document.toString());
  }

  public final void testEasyDiff() {
    String v1 = "You don't know what you are talking about";
    String v2 = "You really know what you are talking about!";
    LinkedList<DiffMatchPatch.Diff> diffs = new DiffMatchPatch().diff_main(v1, v2);
    System.out.println(diffs);

    Document doc1 = new StandardDocument(v1);
    List<StandardDocument.Op> opList = Ops.diff(doc1, new StandardDocument(v2));

    System.out.println(opList);
    doc1.applyAll(opList);
    System.out.println(doc1);

  }

  public final void testWeirdDiff() {
    String v1 = "You don't know what you are talking about.";
    String v2 = "I really know what he is talking about!";
    LinkedList<DiffMatchPatch.Diff> diffs = new DiffMatchPatch().diff_main(v1, v2);
    System.out.println(diffs);

    Document doc1 = new StandardDocument(v1);
    List<StandardDocument.Op> opList = Ops.diff(doc1, new StandardDocument(v2));

    System.out.println(opList);
    doc1.applyAll(opList);
    System.out.println(doc1);
  }

  public void testAbc() {
    Document a = new StandardDocument("I'm feeling fat and sassy");
    Document b = new StandardDocument("I'm feeling chubby and sassy");
    Document c = new StandardDocument("I'm feeling fat and sexy");

    List<StandardDocument.Op> toB = Ops.diff(a, b);
    List<StandardDocument.Op> toC = Ops.diff(a, c);

    a.applyAll(toB);
    a.applyAll(toC);

    assertEquals("I'm feeling chubby and sexy", a.toString());
  }

  public void testView() {
    Document ace = new StandardDocument("ACE");
    ace.insert(1, "B", ace.version());
    Document ace2 = ace.asOf(ace.version() - 1);
    ace2.insert(2, "D", ace2.version());
    assertEquals("ABCDE", ace.toString());
    assertEquals("ACDE", ace2.toString());
  }

  public void testView2() {
    Document ace = new StandardDocument("ACE");
    ace.insert(1, "BBBB", ace.version());
    Document ace2 = ace.asOf(ace.version() - 1);
    ace2.insert(2, "DDDD", ace2.version());

    ace.insert(5, "CCC", ace.version());
    ace2.insert(0, "AAA", ace2.version());
    ace2.insert(ace2.toString().length(), "FFFF", ace2.version());

    assertEquals("AAAABBBBCCCCDDDDEFFFF", ace.toString());
    assertEquals("AAAACDDDDEFFFF", ace2.toString());
  }
}
