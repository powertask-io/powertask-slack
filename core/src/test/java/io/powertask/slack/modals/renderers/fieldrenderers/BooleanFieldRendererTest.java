/*
 * Copyright © 2020 Lunatech Labs B.V. and/or licensed to Lunatech Labs B.V. under
 * one or more contributor license agreements. Lunatech licenses this file to you
 * under the Apache License, Version 2.0; you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package io.powertask.slack.modals.renderers.fieldrenderers;

import static com.slack.api.model.block.composition.BlockCompositions.markdownText;
import static com.slack.api.model.block.composition.BlockCompositions.option;
import static com.slack.api.model.block.composition.BlockCompositions.plainText;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.slack.api.model.block.element.BlockElement;
import com.slack.api.model.block.element.RadioButtonsElement;
import com.slack.api.model.view.ViewState;
import com.slack.api.model.view.ViewState.SelectedOption;
import io.powertask.slack.formfields.BooleanField;
import io.powertask.slack.formfields.ImmutableBooleanField;
import io.vavr.control.Either;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class BooleanFieldRendererTest {

  private ImmutableBooleanField.Builder getBaseField(String id) {
    return ImmutableBooleanField.builder().id(id).label(id).required(true);
  }

  @Test
  void renderBasicElement() {
    String id = "1234";

    BooleanField formField = getBaseField(id).build();

    BlockElement renderedElement = new BooleanFieldRenderer(formField).renderElement();

    BlockElement expectedElement =
        RadioButtonsElement.builder()
            .actionId("1234_boolean")
            .options(
                Arrays.asList(option(plainText("Yes"), "true"), option(plainText("No"), "false")))
            .build();

    assertEquals(expectedElement, renderedElement);
  }

  @Test
  void renderFullOptionsElement() {
    String id = "1234";
    boolean value = true;

    BooleanField formField =
        getBaseField(id).value(true).trueLabel("Yep").falseLabel("Nope").build();

    BlockElement renderedElement = new BooleanFieldRenderer(formField).renderElement();

    BlockElement expectedElement =
        RadioButtonsElement.builder()
            .actionId("1234_boolean")
            .initialOption(option(markdownText("Yep"), "true"))
            .options(
                Arrays.asList(
                    option(markdownText("Yep"), "true"), option(markdownText("Nope"), "false")))
            .build();

    assertEquals(expectedElement, renderedElement);
  }

  @Test
  void extractValue() {
    String id = "1234";
    BooleanField formField = getBaseField(id).build();

    SelectedOption selectedOption = new SelectedOption();
    selectedOption.setValue("true");

    ViewState.Value value = new ViewState.Value();
    value.setSelectedOption(selectedOption);

    Map<String, ViewState.Value> fields = Collections.singletonMap("1234_boolean", value);

    assertEquals(
        Either.right(Optional.of("true")),
        new BooleanFieldRenderer(formField).extractValue(fields));
  }
}
