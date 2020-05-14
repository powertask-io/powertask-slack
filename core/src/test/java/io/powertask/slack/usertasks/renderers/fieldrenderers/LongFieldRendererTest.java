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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.camunda.bpm.engine.form.FormFieldValidationConstraint;
import org.camunda.bpm.engine.impl.form.FormFieldImpl;
import org.camunda.bpm.engine.impl.form.FormFieldValidationConstraintImpl;
import org.camunda.bpm.engine.impl.form.type.LongFormType;
import org.camunda.bpm.engine.variable.impl.value.PrimitiveTypeValueImpl;
import org.junit.jupiter.api.Test;

class LongFieldRendererTest {

  private FormFieldImpl getBaseField(String id) {
    FormFieldImpl formField = new FormFieldImpl();
    formField.setType(new LongFormType());
    formField.setValue(new PrimitiveTypeValueImpl.LongValueImpl(null));
    formField.setId(id);
    return formField;
  }

  @Test
  void renderBasicElement() {
    String id = "1234";

    FormFieldImpl formField = getBaseField(id);

    BlockElement renderedElement = new LongFieldRenderer(formField).renderElement();

    BlockElement expectedElement = PlainTextInputElement.builder().actionId(id + "_long").build();

    assertEquals(expectedElement, renderedElement);
  }

  @Test
  void renderFullOptionsElement() {
    String id = "1234";
    Long value = 713L;
    String placeholder = "Enter your long...";

    FormFieldImpl formField = getBaseField(id);

    formField.setValue(new PrimitiveTypeValueImpl.LongValueImpl(value));

    Map<String, String> properties = new HashMap<>();
    properties.put("slack-placeholder", placeholder);
    formField.setProperties(properties);

    BlockElement renderedElement = new LongFieldRenderer(formField).renderElement();

    BlockElement expectedElement =
        PlainTextInputElement.builder()
            .initialValue(value.toString())
            .placeholder(plainText(placeholder))
            .actionId(id + "_long")
            .build();

    assertEquals(expectedElement, renderedElement);
  }

  @Test
  void extractValue() {
    String id = "1234";
    FormFieldImpl formField = getBaseField(id);

    ViewState.Value value = new ViewState.Value();
    value.setValue("1234");

    Map<String, ViewState.Value> fields = Collections.singletonMap("1234_long", value);

    assertEquals(
        Either.right(Optional.of(1234L)),
        new LongFieldRenderer(formField).extractValue(formField, fields));
  }

  @Test
  void extractValueMinMaxConstraint() {
    String id = "1234";
    FormFieldImpl formField = getBaseField(id);

    List<FormFieldValidationConstraint> constraints = new ArrayList<>();
    constraints.add(new FormFieldValidationConstraintImpl("min", "10"));
    constraints.add(new FormFieldValidationConstraintImpl("max", "101"));
    formField.setValidationConstraints(constraints);

    // Value is below 'min' constraint
    ViewState.Value valueMin = new ViewState.Value();
    valueMin.setValue("5");
    Map<String, ViewState.Value> fieldsMin = Collections.singletonMap("1234_long", valueMin);

    assertEquals(
        Either.left("Minimum value is 10"),
        new LongFieldRenderer(formField).extractValue(formField, fieldsMin));

    // Value is above 'max' constraint
    ViewState.Value valueMax = new ViewState.Value();
    valueMax.setValue("500");
    Map<String, ViewState.Value> fieldsMax = Collections.singletonMap("1234_long", valueMax);

    assertEquals(
        // The configured max is exclusive, but we report an inclusive value
        Either.left("Maximum value is 100"),
        new LongFieldRenderer(formField).extractValue(formField, fieldsMax));
  }

  @Test
  public void maxValueIsExclusive() {
    String id = "1234";
    FormFieldImpl formField = getBaseField(id);

    List<FormFieldValidationConstraint> constraints = new ArrayList<>();
    constraints.add(new FormFieldValidationConstraintImpl("max", "11"));
    formField.setValidationConstraints(constraints);

    // Value is above 'max' constraint
    ViewState.Value value = new ViewState.Value();
    value.setValue("11");
    Map<String, ViewState.Value> fields = Collections.singletonMap("1234_long", value);

    assertEquals(
        Either.left("Maximum value is 10"),
        new LongFieldRenderer(formField).extractValue(formField, fields));
  }
}
