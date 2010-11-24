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
package com.google.wave.splash.text;

import com.google.wave.api.Annotations;
import com.google.wave.api.Element;
import com.google.wave.api.Line;
import com.google.wave.api.Wavelet;
import com.google.wave.splash.text.ContentUnrenderer.UnrenderedBlip;
import com.google.wave.splash.web.template.WaveRenderer;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import static org.mockito.Mockito.mock;


/**
 * Turns a HTML element (rendered initially by ContentRenderer.renderHTML) back into a wave
 * document, with elements and attached annotations.
 *
 * @author anthonybaxter@gmail.com (Anthony Baxter)
 * TODO(anthonybaxter): find a better name for this class.
 */
public class ContentUnrendererTest  extends TestCase {

  private static String BLIP_ID = "b+blip";
  private static String PARENT_BLIP_ID = "b+parent";
  private static String THREAD_ID = "b+thread";

  private GadgetRenderer mockGadgetRenderer;
  private WaveRenderer mockWaveRenderer;
  private ContentRenderer renderer;
  private Annotations emptyAnnotations;
  private Wavelet mockWavelet;
  private TreeMap<Integer, Element> elements;
  private List<String> emptyContributors;
  private ContentUnrenderer unrenderer;


  @Override
  protected void setUp() throws Exception {
    super.setUp();

    mockGadgetRenderer = mock(GadgetRenderer.class);
    mockWaveRenderer = mock(WaveRenderer.class);
    renderer = new ContentRenderer(mockGadgetRenderer, mockWaveRenderer);
    unrenderer = new ContentUnrenderer();
    emptyAnnotations = new Annotations();
    elements = new TreeMap<Integer, Element>();
    emptyContributors = new ArrayList<String>();

    mockWavelet = mock(Wavelet.class);
  }

  public void testRoundTripSimple() {
    String input = "SomeText";
    elements.put(0, new Line());
    elements.put(4, new Line());

    String rendered = renderer.renderHtml(input, emptyAnnotations, elements, emptyContributors);
    assertEquals("<p>Some</p><p>Text", rendered);

    UnrenderedBlip output = unrenderer.unrender(rendered);

    // newlines aren't important, apart from the first one.
    assertTrue(output.contents.startsWith("\n"));
    assertEquals("SomeText", output.contents);
    assertEquals(elements, output.elements);

  }
}
