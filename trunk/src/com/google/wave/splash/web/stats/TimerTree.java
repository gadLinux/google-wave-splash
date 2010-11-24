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

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.wave.splash.PortableRequestScoped;

import java.util.List;
import java.util.Map;

/**
 * Class that helps build a hierarchical tree of timers.
 *
 * @author David Byttow
 */
@PortableRequestScoped
class TimerTree {
  /**
   * Represents a single timer node.
   */
  static class Node {
    private final String name;
    private final Node parent;
    private final List<Node> children;
    private final Map<String, Node> childMap;
    private final Measurement measurement;

    private long start;

    private Node(Node parent, String name) {
      this.parent = parent;
      this.name = name;
      this.children = Lists.newLinkedList();
      this.childMap = Maps.newHashMap();
      this.measurement = new Measurement();
    }

    private Node newChild(String name) {
      Node node = childMap.get(name);
      if (node == null) {
        node = new Node(this, name);
        children.add(node);
        childMap.put(name, node);
      }
      return node;
    }

    private void start() {
      start = System.currentTimeMillis();
    }

    private void stop() {
      int delta = (int) (System.currentTimeMillis() - start);
      measurement.sample(delta);
    }

    String getName() {
      return name;
    }

    Measurement getMeasurement() {
      return measurement;
    }

    Iterable<Node> getChildren() {
      return children;
    }
  }

  private final Node root;
  private Node current;

  TimerTree() {
    this.root = new Node(null, "");
    this.current = root;
  }

  /**
   * Push a new timer node.
   */
  void push(String name) {
    Node node = current.newChild(name);
    node.start();
    current = node;
  }

  /**
   * Pop the current timer node and return it.
   */
  Node pop(String name) {
    Preconditions.checkArgument(name.equals(current.name),
        "timer mismatch, expected " + current.name + " but given " + name);
    current.stop();
    Node out = current;
    current = current.parent;
    return out;
  }

  /**
   * @return true if the tree been completely sampled.
   */
  boolean isTiming() {
    return current != root;
  }

  Node getRoot() {
    return root;
  }
}
