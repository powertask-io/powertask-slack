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
package io.powertask.slack.camunda;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.powertask.slack.FormField;
import io.powertask.slack.formfields.BooleanField;
import io.powertask.slack.formfields.DateField;
import io.powertask.slack.formfields.EnumField;
import io.powertask.slack.formfields.EnumField.EnumValue;
import io.powertask.slack.formfields.ImmutableBooleanField;
import io.powertask.slack.formfields.ImmutableDateField;
import io.powertask.slack.formfields.ImmutableEnumField;
import io.powertask.slack.formfields.ImmutableEnumValue;
import io.powertask.slack.formfields.ImmutableLongField;
import io.powertask.slack.formfields.ImmutableStringField;
import io.powertask.slack.formfields.LongField;
import io.powertask.slack.formfields.StringField;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.camunda.bpm.engine.form.FormFieldValidationConstraint;
import org.camunda.bpm.engine.impl.form.FormFieldImpl;
import org.camunda.bpm.engine.impl.form.FormFieldValidationConstraintImpl;
import org.camunda.bpm.engine.impl.form.type.BooleanFormType;
import org.camunda.bpm.engine.impl.form.type.DateFormType;
import org.camunda.bpm.engine.impl.form.type.EnumFormType;
import org.camunda.bpm.engine.impl.form.type.LongFormType;
import org.camunda.bpm.engine.impl.form.type.StringFormType;
import org.camunda.bpm.engine.variable.impl.value.PrimitiveTypeValueImpl;
import org.camunda.bpm.engine.variable.impl.value.PrimitiveTypeValueImpl.StringValueImpl;
import org.junit.jupiter.api.Test;

class FormFieldMapperTest {
  private FormFieldImpl getBaseStringField(String id, String label) {
    FormFieldImpl formField = new FormFieldImpl();
    formField.setType(new StringFormType());
    formField.setValue(new PrimitiveTypeValueImpl.StringValueImpl(null));
    formField.setId(id);
    formField.setLabel(label);
    return formField;
  }

  @Test
  void mapBasicStringField() {
    String id = "1234";
    String label = "The Field";

    FormFieldImpl formField = getBaseStringField(id, label);

    FormField<?> field = FormFieldMapper.map(formField);
    assertTrue(field instanceof StringField);
    assertEquals(
        ImmutableStringField.builder().id(id).label(label).required(false).multiline(false).build(),
        field);
  }

  @Test
  void mapFullOptionsStringField() {
    String id = "1234";
    String label = "The Field";
    String value = "the-value";
    String placeholder = "Fill it out...";

    FormFieldImpl formField = getBaseStringField(id, label);
    formField.setValue(new PrimitiveTypeValueImpl.StringValueImpl(value));

    Map<String, String> properties = new HashMap<>();
    properties.put("slack-multiline", "true");
    properties.put("slack-placeholder", placeholder);
    formField.setProperties(properties);

    ArrayList<FormFieldValidationConstraint> constraints = new ArrayList<>();
    constraints.add(new FormFieldValidationConstraintImpl("required", null));
    constraints.add(new FormFieldValidationConstraintImpl("minlength", "10"));
    constraints.add(new FormFieldValidationConstraintImpl("maxlength", "30"));
    formField.setValidationConstraints(constraints);

    FormField<?> field = FormFieldMapper.map(formField);
    assertTrue(field instanceof StringField);
    assertEquals(
        ImmutableStringField.builder()
            .id(id)
            .label(label)
            .value(value)
            .required(true)
            .placeholder(placeholder)
            .multiline(true)
            .minLength(10)
            .maxLength(30)
            .build(),
        field);
  }

  private FormFieldImpl getBaseBooleanField(String id, String label) {
    FormFieldImpl formField = new FormFieldImpl();
    formField.setType(new BooleanFormType());
    formField.setValue(new PrimitiveTypeValueImpl.BooleanValueImpl(null));
    formField.setId(id);
    formField.setLabel(label);
    return formField;
  }

  @Test
  void mapBasicBooleanField() {
    String id = "1234";
    String label = "The Field";

    FormFieldImpl formField = getBaseBooleanField(id, label);
    FormField<?> field = FormFieldMapper.map(formField);

    assertTrue(field instanceof BooleanField);
    assertEquals(
        ImmutableBooleanField.builder().id(id).label(label).required(false).build(), field);
  }

  @Test
  void mapFullOptionsBooleanField() {
    String id = "1234";
    String label = "The Field";
    String trueLabel = "Yep";
    String falseLabel = "Nope";
    boolean value = true;

    FormFieldImpl formField = getBaseBooleanField(id, label);

    formField.setValue(new PrimitiveTypeValueImpl.BooleanValueImpl(value));

    Map<String, String> properties = new HashMap<>();
    properties.put("slack-true-label", trueLabel);
    properties.put("slack-false-label", falseLabel);
    formField.setProperties(properties);

    ArrayList<FormFieldValidationConstraint> constraints = new ArrayList<>();
    constraints.add(new FormFieldValidationConstraintImpl("required", null));
    formField.setValidationConstraints(constraints);

    FormField<?> field = FormFieldMapper.map(formField);
    assertTrue(field instanceof BooleanField);
    assertEquals(
        ImmutableBooleanField.builder()
            .id(id)
            .value(value)
            .label(label)
            .required(true)
            .trueLabel(trueLabel)
            .falseLabel(falseLabel)
            .build(),
        field);
  }

  private FormFieldImpl getBaseLongField(String id, String label) {
    FormFieldImpl formField = new FormFieldImpl();
    formField.setType(new LongFormType());
    formField.setValue(new PrimitiveTypeValueImpl.LongValueImpl(null));
    formField.setId(id);
    formField.setLabel(label);
    return formField;
  }

  @Test
  void mapBasicLongField() {
    String id = "1234";
    String label = "The Field";

    FormFieldImpl formField = getBaseLongField(id, label);
    FormField<?> field = FormFieldMapper.map(formField);

    assertTrue(field instanceof LongField);
    assertEquals(ImmutableLongField.builder().id(id).label(label).required(false).build(), field);
  }

  @Test
  void mapFullOptionsLongField() {
    String id = "1234";
    String label = "The Field";
    Long value = 713L;
    String placeholder = "Enter your long...";

    FormFieldImpl formField = getBaseLongField(id, label);
    formField.setValue(new PrimitiveTypeValueImpl.LongValueImpl(value));
    Map<String, String> properties = new HashMap<>();
    properties.put("slack-placeholder", placeholder);
    formField.setProperties(properties);

    formField.setValidationConstraints(
        Arrays.asList(
            new FormFieldValidationConstraintImpl("required", null),
            new FormFieldValidationConstraintImpl("min", "5"),
            new FormFieldValidationConstraintImpl(
                "max", "11"))); // This is an exclusive value in Camunda.

    FormField<?> field = FormFieldMapper.map(formField);

    assertTrue(field instanceof LongField);
    assertEquals(
        ImmutableLongField.builder()
            .id(id)
            .value(Optional.of(value))
            .label(label)
            .required(true)
            .placeholder(placeholder)
            .min(5)
            .max(10)
            .build(),
        field);
  }

  static final String camundaDatePattern = "dd/MM/yyyy";

  private FormFieldImpl getBaseDateField(String id, String label) {
    FormFieldImpl formField = new FormFieldImpl();
    formField.setType(new DateFormType(camundaDatePattern));
    formField.setValue(new StringValueImpl(null));
    formField.setId(id);
    formField.setLabel(label);
    return formField;
  }

  @Test
  void mapBasicDateField() {
    String id = "1234";
    String label = "The Field";

    FormFieldImpl formField = getBaseDateField(id, label);

    FormField<?> field = FormFieldMapper.map(formField);
    assertTrue(field instanceof DateField);
    assertEquals(ImmutableDateField.builder().id(id).label(label).required(false).build(), field);
  }

  @Test
  void mapFullOptionsDateField() {
    String id = "1234";
    String label = "The Field";
    String placeholder = "placeholder";
    LocalDate value = LocalDate.of(2010, 3, 21);

    FormFieldImpl formField = getBaseDateField(id, label);
    formField.setValue(new StringValueImpl("21/03/2010"));

    Map<String, String> properties = new HashMap<>();
    properties.put("slack-placeholder", placeholder);
    formField.setProperties(properties);

    ArrayList<FormFieldValidationConstraint> constraints = new ArrayList<>();
    constraints.add(new FormFieldValidationConstraintImpl("required", null));
    formField.setValidationConstraints(constraints);

    FormField<?> field = FormFieldMapper.map(formField);
    assertTrue(field instanceof DateField);
    assertEquals(
        ImmutableDateField.builder()
            .id(id)
            .label(label)
            .required(true)
            .value(value)
            .placeholder(placeholder)
            .build(),
        field);
  }

  private FormFieldImpl getBaseEnumField(String id, String label) {
    FormFieldImpl formField = new FormFieldImpl();
    Map<String, String> values = new HashMap<>();
    values.put("one", "One");
    values.put("two", "Two");
    formField.setType(new EnumFormType(values));
    formField.setValue(new PrimitiveTypeValueImpl.StringValueImpl(null));
    formField.setId(id);
    formField.setLabel(label);
    return formField;
  }

  @Test
  void mapBasicEnumField() {
    String id = "1234";
    String label = "The Field";
    FormFieldImpl formField = getBaseEnumField(id, label);

    FormField<?> field = FormFieldMapper.map(formField);
    assertTrue(field instanceof EnumField);

    Map<String, EnumValue> expectedValues = new HashMap<>();
    expectedValues.put("one", ImmutableEnumValue.builder().text("One").build());
    expectedValues.put("two", ImmutableEnumValue.builder().text("Two").build());

    assertEquals(
        ImmutableEnumField.builder()
            .id(id)
            .label(label)
            .required(false)
            .values(expectedValues)
            .build(),
        field);
  }

  @Test
  void mapFullOptionsEnumField() {
    String id = "1234";
    String label = "The Field";

    FormFieldImpl formField = getBaseEnumField(id, label);
    formField.setValue(new PrimitiveTypeValueImpl.StringValueImpl("two"));

    Map<String, String> properties = new HashMap<>();
    properties.put("slack-description-one", "First Description");
    properties.put("slack-description-two", "Second Description");
    formField.setProperties(properties);

    ArrayList<FormFieldValidationConstraint> constraints = new ArrayList<>();
    constraints.add(new FormFieldValidationConstraintImpl("required", null));
    formField.setValidationConstraints(constraints);

    FormField<?> field = FormFieldMapper.map(formField);
    assertTrue(field instanceof EnumField);

    Map<String, EnumValue> expectedValues = new HashMap<>();
    expectedValues.put(
        "one", ImmutableEnumValue.builder().text("One").description("First Description").build());
    expectedValues.put(
        "two", ImmutableEnumValue.builder().text("Two").description("Second Description").build());

    assertEquals(
        ImmutableEnumField.builder()
            .id(id)
            .label(label)
            .required(true)
            .value(Optional.of("two"))
            .values(expectedValues)
            .build(),
        field);
  }
}
