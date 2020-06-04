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

import io.powertask.slack.FormField;
import io.powertask.slack.formfields.EnumField;
import io.powertask.slack.formfields.EnumField.EnumValue;
import io.powertask.slack.formfields.ImmutableBooleanField;
import io.powertask.slack.formfields.ImmutableDateField;
import io.powertask.slack.formfields.ImmutableEnumField;
import io.powertask.slack.formfields.ImmutableEnumValue;
import io.powertask.slack.formfields.ImmutableLongField;
import io.powertask.slack.formfields.ImmutableStringField;
import io.powertask.slack.formfields.StringField;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.camunda.bpm.engine.impl.form.type.BooleanFormType;
import org.camunda.bpm.engine.impl.form.type.DateFormType;
import org.camunda.bpm.engine.impl.form.type.EnumFormType;
import org.camunda.bpm.engine.impl.form.type.LongFormType;
import org.camunda.bpm.engine.impl.form.type.StringFormType;
import org.camunda.bpm.engine.variable.value.BooleanValue;
import org.camunda.bpm.engine.variable.value.LongValue;
import org.camunda.bpm.engine.variable.value.StringValue;

public class FormFieldMapper {
  public static final DateTimeFormatter CAMUNDA_DATE_FORMATTER =
      DateTimeFormatter.ofPattern("dd/MM/yyyy");

  private static final String CONSTRAINT_REQUIRED = "required";
  private static final String CONSTRAINT_MINLENGTH = "minlength";
  private static final String CONSTRAINT_MAXLENGTH = "maxlength";
  private static final String CONSTRAINT_MIN = "min";
  private static final String CONSTRAINT_MAX = "max";

  private static final String PROPERTY_SLACK_HINT = "slack-hint";
  private static final String PROPERTY_SLACK_PLACEHOLDER = "slack-placeholder";
  private static final String PROPERTY_SLACK_MULTILINE = "slack-multiline";
  private static final String PROPERTY_SLACK_DESCRIPTION_PREFIX = "slack-description-";
  private static final String PROPERTY_SLACK_TRUE_LABEL = "slack-true-label";
  private static final String PROPERTY_SLACK_FALSE_LABEL = "slack-false-label";

  private FormFieldMapper() {}

  public static FormField<?> map(org.camunda.bpm.engine.form.FormField formField) {

    if (formField.getType() instanceof StringFormType) {
      return buildStringField(formField);
    } else if (formField.getType() instanceof EnumFormType) {
      return buildEnumField(formField);
    } else if (formField.getType() instanceof LongFormType) {
      return buildLongField(formField);
    } else if (formField.getType() instanceof BooleanFormType) {
      return buildBooleanField(formField);
    } else if (formField.getType() instanceof DateFormType) {
      return buildDateField(formField);
    } else {
      throw new RuntimeException("Unknown FormField type: " + formField.getType());
    }
  }

  public static StringField buildStringField(org.camunda.bpm.engine.form.FormField formField) {
    return ImmutableStringField.builder()
        .id(formField.getId())
        .label(formField.getLabel())
        .value(Optional.ofNullable(((StringValue) formField.getValue()).getValue()))
        .required(hasConstraint(formField, CONSTRAINT_REQUIRED))
        .hint(getStringProperty(formField, PROPERTY_SLACK_HINT))
        .placeholder(getStringProperty(formField, PROPERTY_SLACK_PLACEHOLDER))
        .multiline(getBooleanProperty(formField, PROPERTY_SLACK_MULTILINE).orElse(false))
        .minLength(getConstraintValue(formField, CONSTRAINT_MINLENGTH).map(Integer::valueOf))
        .maxLength(getConstraintValue(formField, CONSTRAINT_MAXLENGTH).map(Integer::valueOf))
        .build();
  }

  public static EnumField buildEnumField(org.camunda.bpm.engine.form.FormField formField) {
    Map<String, EnumValue> values = new HashMap<>();

    ((EnumFormType) formField.getType())
        .getValues()
        .forEach(
            (key, value) -> {
              Optional<String> description =
                  getStringProperty(formField, PROPERTY_SLACK_DESCRIPTION_PREFIX + key);
              values.put(
                  key, ImmutableEnumValue.builder().text(value).description(description).build());
            });

    return ImmutableEnumField.builder()
        .id(formField.getId())
        .label(formField.getLabel())
        .value(Optional.ofNullable(((StringValue) formField.getValue()).getValue()))
        .required(hasConstraint(formField, CONSTRAINT_REQUIRED))
        .hint(getStringProperty(formField, PROPERTY_SLACK_HINT))
        .values(values)
        .build();
  }

  public static FormField<?> buildLongField(org.camunda.bpm.engine.form.FormField formField) {
    return ImmutableLongField.builder()
        .id(formField.getId())
        .label(formField.getLabel())
        .value(Optional.ofNullable(((LongValue) formField.getValue()).getValue()))
        .required(hasConstraint(formField, CONSTRAINT_REQUIRED))
        .hint(getStringProperty(formField, PROPERTY_SLACK_HINT))
        .placeholder(getStringProperty(formField, PROPERTY_SLACK_PLACEHOLDER))
        .min(getLongConstraint(formField, CONSTRAINT_MIN))
        .max(
            getLongConstraint(formField, CONSTRAINT_MAX)
                .map(l -> l - 1)) // Camunda's 'max' constraint is exclusive.
        .build();
  }

  public static FormField<?> buildBooleanField(org.camunda.bpm.engine.form.FormField formField) {
    return ImmutableBooleanField.builder()
        .id(formField.getId())
        .label(formField.getLabel())
        .value(Optional.ofNullable(((BooleanValue) formField.getValue()).getValue()))
        .required(hasConstraint(formField, CONSTRAINT_REQUIRED))
        .hint(getStringProperty(formField, PROPERTY_SLACK_HINT))
        .trueLabel(getStringProperty(formField, PROPERTY_SLACK_TRUE_LABEL))
        .falseLabel(getStringProperty(formField, PROPERTY_SLACK_FALSE_LABEL))
        .build();
  }

  public static FormField<?> buildDateField(org.camunda.bpm.engine.form.FormField formField) {
    Optional<LocalDate> value =
        Optional.ofNullable(((StringValue) formField.getValue()).getValue())
            .map(stringDate -> LocalDate.parse(stringDate, CAMUNDA_DATE_FORMATTER));

    return ImmutableDateField.builder()
        .id(formField.getId())
        .label(formField.getLabel())
        .value(value)
        .required(hasConstraint(formField, CONSTRAINT_REQUIRED))
        .hint(getStringProperty(formField, PROPERTY_SLACK_HINT))
        .placeholder(getStringProperty(formField, PROPERTY_SLACK_PLACEHOLDER))
        .build();
  }

  public static Optional<Optional<String>> getConstraint(
      org.camunda.bpm.engine.form.FormField formField, String name) {
    return formField.getValidationConstraints().stream()
        .filter(
            formFieldValidationConstraint -> formFieldValidationConstraint.getName().equals(name))
        .map(
            formFieldValidationConstraint ->
                Optional.ofNullable((String) formFieldValidationConstraint.getConfiguration()))
        .findFirst();
  }

  public static Optional<String> getConstraintValue(
      org.camunda.bpm.engine.form.FormField formField, String name) {
    return getConstraint(formField, name).flatMap(Function.identity());
  }

  // Contrary to our own 'slack-' prefixed boolean properties, we don't check the config for true or
  // false, merely the existence of the constraint is sufficient. This aligns with Camunda's Task
  // List.
  public static boolean hasConstraint(
      org.camunda.bpm.engine.form.FormField formField, String name) {
    return getConstraint(formField, name).isPresent();
  }

  // TODO, document throwing behaviour
  public static Optional<Long> getLongConstraint(
      org.camunda.bpm.engine.form.FormField formField, String name) {
    return getConstraint(formField, name)
        .map(
            value ->
                value
                    .map(Long::valueOf)
                    .orElseThrow(
                        () ->
                            new RuntimeException(
                                "Configuration for constraint " + name + " missing!")));
  }

  public static Optional<String> getStringProperty(
      org.camunda.bpm.engine.form.FormField formField, String name) {
    if (!formField.getProperties().containsKey(name)) {
      return Optional.empty();
    } else {
      return Optional.ofNullable(formField.getProperties().get(name))
          .map(Optional::of) // This looks odd, but we want to throw on null value!
          .orElseThrow(() -> new RuntimeException("Property " + name + " is null!"));
    }
  }

  public static Optional<Boolean> getBooleanProperty(
      org.camunda.bpm.engine.form.FormField formField, String name) {
    return getStringProperty(formField, name).map(value -> value.equals("true"));
  }
}
