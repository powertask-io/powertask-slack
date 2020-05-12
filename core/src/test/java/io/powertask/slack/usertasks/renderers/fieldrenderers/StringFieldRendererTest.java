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

import static com.slack.api.model.block.composition.BlockCompositions.plainText;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.slack.api.model.block.element.BlockElement;
import com.slack.api.model.block.element.PlainTextInputElement;
import com.slack.api.model.view.ViewState;
import io.vavr.control.Either;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.camunda.bpm.engine.form.FormFieldValidationConstraint;
import org.camunda.bpm.engine.impl.form.FormFieldImpl;
import org.camunda.bpm.engine.impl.form.FormFieldValidationConstraintImpl;
import org.camunda.bpm.engine.impl.form.type.StringFormType;
import org.camunda.bpm.engine.variable.impl.value.PrimitiveTypeValueImpl;
import org.junit.jupiter.api.Test;

class StringFieldRendererTest {

  private FormFieldImpl getBaseField(String id) {
    FormFieldImpl formField = new FormFieldImpl();
    formField.setType(new StringFormType());
    formField.setValue(new PrimitiveTypeValueImpl.StringValueImpl(null));
    formField.setId(id);
    return formField;
  }

  @Test
  void renderBasicElement() {
    String id = "1234";

    FormFieldImpl formField = getBaseField(id);

    BlockElement renderedElement =
        new io.powertask.slack.usertasks.renderers.fieldrenderers.StringFieldRenderer(formField)
            .renderElement();

    BlockElement expectedElement =
        PlainTextInputElement.builder().multiline(false).actionId(id + "_text").build();

    assertEquals(expectedElement, renderedElement);
  }

  @Test
  void renderFullOptionsElement() {
    String id = "1234";
    String value = "the-value";
    String placeholder = "Fill it out...";

    FormFieldImpl formField = getBaseField(id);
    formField.setValue(new PrimitiveTypeValueImpl.StringValueImpl(value));

    Map<String, String> properties = new HashMap<>();
    properties.put("slack-multiline", "true");
    properties.put("slack-placeholder", placeholder);
    formField.setProperties(properties);

    ArrayList<FormFieldValidationConstraint> constraints = new ArrayList<>();
    constraints.add(new FormFieldValidationConstraintImpl("minlength", "10"));
    constraints.add(new FormFieldValidationConstraintImpl("maxlength", "30"));
    formField.setValidationConstraints(constraints);

    BlockElement renderedElement = new StringFieldRenderer(formField).renderElement();

    BlockElement expectedElement =
        PlainTextInputElement.builder()
            .initialValue(value)
            .multiline(true)
            .minLength(10)
            .maxLength(30)
            .placeholder(plainText(placeholder))
            .actionId(id + "_text")
            .build();

    assertEquals(expectedElement, renderedElement);
  }

  @Test
  void extractValue() {
    String id = "1234";
    FormFieldImpl formField = getBaseField(id);

    ViewState.Value value = new ViewState.Value();
    value.setValue("Hello, there!");

    Map<String, ViewState.Value> fields = Collections.singletonMap("1234_text", value);

    assertEquals(
        Either.right(Optional.of("Hello, there!")),
        new StringFieldRenderer(formField).extractValue(formField, fields));
  }
}
