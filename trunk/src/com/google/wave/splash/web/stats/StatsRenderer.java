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

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.wave.splash.Options;
import com.google.wave.splash.web.stats.StatsRecorder.ProfiledRequest;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Renderer for stats.
 *
 * @author David Byttow
 */
@Singleton
public class StatsRenderer {
  private final Options options;

  @Inject
  StatsRenderer(Options options) {
    this.options = options;
  }

  private static final Comparator<String> CASE_INSENSITIVE_COMPARATOR = new Comparator<String>() {
    @Override
    public int compare(String first, String second) {
      if (second.startsWith(first)) {
        return -1;
      } else if (first.startsWith(second)) {
        return 1;
      }
      return second.toLowerCase().compareTo(first.toLowerCase());
    }
  };

  public String renderHtml(Map<String, Measurement> measurements,
      List<ProfiledRequest> profiles) {
    StringBuilder builder = new StringBuilder();
    if (!options.productionMode()) {
      builder.append("<div style=\"color: red\"><b>Production mode OFF</b>"
          + "(timing may be skewed)</div>");
    }
    return builder.append("<div style=\"font-family: monospace; font-size: 0.9em; padding:4px\">")
        .append("<b>Global</b> (count, average, low, high, total)<br/>")
        .append(renderGlobalStats(measurements))
        .append("<br/>")
        .append("<b>Requests</b> (count, average, low, high, total)<br/>")
        .append(renderRequestStats(profiles))
        .append("</div>")
        .toString();
  }

  private String renderRequestStats(List<ProfiledRequest> profiles) {
    StringBuilder builder = new StringBuilder();
    for (int i = profiles.size() - 1; i > 0; --i) {
      ProfiledRequest profile = profiles.get(i);
      for (TimerTree.Node child : profile.tree.getRoot().getChildren()) {
        builder.append(renderNode(child, 0));
      }
    }
    return builder.toString();
  }

  private String renderNode(TimerTree.Node node, int spaces) {
    StringBuilder builder = new StringBuilder();
    Measurement measurement = node.getMeasurement();
    builder.append("<span style=\"")
        .append(getMeasurementStyle(measurement))
        .append("\"><pre style=\"display:inline\">")
        .append(getSpaces(spaces))
        .append("</pre>")
        .append(node.getName())
        .append(" ")
        .append(node.getMeasurement())
        .append("</span><br/>");
    for (TimerTree.Node child : node.getChildren()) {
      builder.append(renderNode(child, spaces + 2));
    }
    return builder.toString();
  }

  private String renderGlobalStats(Map<String, Measurement> measurements) {
    StringBuilder builder = new StringBuilder();

    Map<String, Measurement> sortedMeasurements = Maps.newTreeMap(CASE_INSENSITIVE_COMPARATOR);
    sortedMeasurements.putAll(measurements);

    for (Map.Entry<String, Measurement> entry : sortedMeasurements.entrySet()) {
      builder.append("<span style=\"")
          .append(getMeasurementStyle(entry.getValue()))
          .append("\">")
          .append(entry.getKey())
          .append(" ")
          .append(entry.getValue())
          .append("</span><br/>");
    }

    return builder.toString();
  }

  private static String getSpaces(int spaces) {
    char[] array = new char[spaces];
    Arrays.fill(array, ' ');
    return new String(array);
  }

  private static String getMeasurementStyle(Measurement m) {
    if (m.getThreshold() == 0) {
      return "";
    }
    if (m.getAverage() >= m.getThreshold()) {
      return "color: red;";
    } else if (m.getHigh() > m.getThreshold() * 0.5) {
      return "color: orange;";
    }
    return "";
  }
}
