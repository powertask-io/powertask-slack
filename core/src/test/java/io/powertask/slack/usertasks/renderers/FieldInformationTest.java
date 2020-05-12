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
package io.powertask.slack.usertasks.renderers;

import static com.slack.api.model.block.composition.BlockCompositions.plainText;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.camunda.bpm.engine.form.FormField;
import org.camunda.bpm.engine.form.FormFieldValidationConstraint;
import org.camunda.bpm.engine.impl.form.FormFieldImpl;
import org.camunda.bpm.engine.impl.form.FormFieldValidationConstraintImpl;
import org.junit.jupiter.api.Test;

public class FieldInformationTest {

  private FormField formFieldWithConstraints() {
    ArrayList<FormFieldValidationConstraint> constraints = new ArrayList<>();
    constraints.add(new FormFieldValidationConstraintImpl("one", "true"));
    constraints.add(new FormFieldValidationConstraintImpl("two", "false"));
    constraints.add(new FormFieldValidationConstraintImpl("three", null));
    constraints.add(new FormFieldValidationConstraintImpl("long", "22"));

    FormFieldImpl formField = new FormFieldImpl();
    formField.setValidationConstraints(constraints);
    return formField;
  }

  @Test
  public void getConstraint() {
    FormField formField = formFieldWithConstraints();

    assertEquals(
        Optional.of(Optional.of("true")), FieldInformation.getConstraint(formField, "one"));
    assertEquals(Optional.of(Optional.empty()), FieldInformation.getConstraint(formField, "three"));
    assertEquals(Optional.empty(), FieldInformation.getConstraint(formField, "four"));
  }

  @Test
  public void hasConstraint() {
    FormField formField = formFieldWithConstraints();

    assertTrue(FieldInformation.hasConstraint(formField, "one"));
    assertTrue(
        FieldInformation.hasConstraint(
            formField, "two")); // Might be counter-intuitive, but similar to Camunda's task list.
    assertTrue(FieldInformation.hasConstraint(formField, "three"));
    assertFalse(FieldInformation.hasConstraint(formField, "four"));
  }

  private FormField formFieldWithProperties() {
    Map<String, String> properties = new HashMap<>();
    properties.put("one", "true");
    properties.put("two", null);

    FormFieldImpl formField = new FormFieldImpl();
    formField.setProperties(properties);
    return formField;
  }

  @Test
  void getStringProperty() {
    FormField formField = formFieldWithProperties();
    assertEquals(Optional.of("true"), FieldInformation.getStringProperty(formField, "one"));
    assertEquals(Optional.empty(), FieldInformation.getStringProperty(formField, "three"));
    assertThrows(
        RuntimeException.class, () -> FieldInformation.getStringProperty(formField, "two"));
  }

  @Test
  void getPlainTextProperty() {
    FormField formField = formFieldWithProperties();

    assertEquals(
        Optional.of(plainText("true")), FieldInformation.getPlainTextProperty(formField, "one"));
    assertEquals(Optional.empty(), FieldInformation.getPlainTextProperty(formField, "three"));
    assertThrows(
        RuntimeException.class, () -> FieldInformation.getPlainTextProperty(formField, "two"));
  }

  @Test
  void getBooleanProperty() {
    FormField formField = formFieldWithProperties();

    assertEquals(Optional.of(true), FieldInformation.getBooleanProperty(formField, "one"));
    assertEquals(Optional.empty(), FieldInformation.getBooleanProperty(formField, "three"));
    assertThrows(
        RuntimeException.class, () -> FieldInformation.getBooleanProperty(formField, "two"));
  }
}
