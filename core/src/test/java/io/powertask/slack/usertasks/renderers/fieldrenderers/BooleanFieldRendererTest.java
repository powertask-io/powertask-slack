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
package io.powertask.slack.usertasks.renderers.fieldrenderers;

import static com.slack.api.model.block.composition.BlockCompositions.option;
import static com.slack.api.model.block.composition.BlockCompositions.plainText;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.slack.api.model.block.element.BlockElement;
import com.slack.api.model.block.element.RadioButtonsElement;
import com.slack.api.model.view.ViewState;
import com.slack.api.model.view.ViewState.SelectedOption;
import io.vavr.control.Either;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.camunda.bpm.engine.impl.form.FormFieldImpl;
import org.camunda.bpm.engine.impl.form.type.BooleanFormType;
import org.camunda.bpm.engine.variable.impl.value.PrimitiveTypeValueImpl;
import org.junit.jupiter.api.Test;

class BooleanFieldRendererTest {

  private FormFieldImpl getBaseField(String id) {
    FormFieldImpl formField = new FormFieldImpl();
    formField.setType(new BooleanFormType());
    formField.setValue(new PrimitiveTypeValueImpl.BooleanValueImpl(null));
    formField.setId(id);
    return formField;
  }

  @Test
  void renderBasicElement() {
    String id = "1234";

    FormFieldImpl formField = getBaseField(id);

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
    Boolean value = true;

    FormFieldImpl formField = getBaseField(id);

    formField.setValue(new PrimitiveTypeValueImpl.BooleanValueImpl(value));

    Map<String, String> properties = new HashMap<>();
    properties.put("slack-true-label", "Yep");
    properties.put("slack-false-label", "Nope");
    formField.setProperties(properties);

    BlockElement renderedElement = new BooleanFieldRenderer(formField).renderElement();

    BlockElement expectedElement =
        RadioButtonsElement.builder()
            .actionId("1234_boolean")
            .initialOption(option(plainText("Yep"), "true"))
            .options(
                Arrays.asList(option(plainText("Yep"), "true"), option(plainText("Nope"), "false")))
            .build();

    assertEquals(expectedElement, renderedElement);
  }

  @Test
  void extractValue() {
    String id = "1234";
    FormFieldImpl formField = getBaseField(id);

    SelectedOption selectedOption = new SelectedOption();
    selectedOption.setValue("true");

    ViewState.Value value = new ViewState.Value();
    value.setSelectedOption(selectedOption);

    Map<String, ViewState.Value> fields = Collections.singletonMap("1234_boolean", value);

    assertEquals(
        Either.right(Optional.of("true")),
        new BooleanFieldRenderer(formField).extractValue(formField, fields));
  }
}
