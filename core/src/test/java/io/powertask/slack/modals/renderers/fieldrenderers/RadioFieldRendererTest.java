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

import static com.slack.api.model.block.composition.BlockCompositions.option;
import static com.slack.api.model.block.composition.BlockCompositions.plainText;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.slack.api.model.block.element.BlockElement;
import com.slack.api.model.block.element.RadioButtonsElement;
import com.slack.api.model.view.ViewState;
import com.slack.api.model.view.ViewState.SelectedOption;
import io.powertask.slack.formfields.EnumField;
import io.powertask.slack.formfields.EnumField.EnumValue;
import io.powertask.slack.formfields.ImmutableEnumField;
import io.powertask.slack.formfields.ImmutableEnumValue;
import io.vavr.control.Either;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RadioFieldRendererTest {

  private ImmutableEnumField.Builder getBaseField(String id) {
    Map<String, EnumValue> values = new HashMap<>();
    values.put("one", ImmutableEnumValue.builder().text("One").build());
    values.put("two", ImmutableEnumValue.builder().text("Two").build());

    return ImmutableEnumField.builder().id(id).label(id).required(true).values(values);
  }

  @Test
  void renderBasicElement() {
    String id = "1234";

    EnumField formField = getBaseField(id).build();

    BlockElement renderedElement = new RadioFieldRenderer(formField).renderElement();

    BlockElement expectedElement =
        RadioButtonsElement.builder()
            .actionId("1234_enum")
            .options(
                Arrays.asList(option(plainText("One"), "one"), option(plainText("Two"), "two")))
            .build();

    assertEquals(expectedElement, renderedElement);
  }

  @Test
  void renderFullOptionsElement() {
    String id = "1234";

    EnumField formField = getBaseField(id).value("two").build();

    BlockElement renderedElement = new RadioFieldRenderer(formField).renderElement();

    BlockElement expectedElement =
        RadioButtonsElement.builder()
            .actionId("1234_enum")
            .initialOption(option(plainText("Two"), "two"))
            .options(
                Arrays.asList(option(plainText("One"), "one"), option(plainText("Two"), "two")))
            .build();

    assertEquals(expectedElement, renderedElement);
  }

  @Test
  void extractValue() {
    String id = "1234";
    EnumField formField = getBaseField(id).build();

    SelectedOption selectedOption = new SelectedOption();
    selectedOption.setValue("two");

    ViewState.Value value = new ViewState.Value();
    value.setSelectedOption(selectedOption);

    Map<String, ViewState.Value> fields = Collections.singletonMap("1234_enum", value);

    assertEquals(
        Either.right(Optional.of("two")), new RadioFieldRenderer(formField).extractValue(fields));
  }
}
