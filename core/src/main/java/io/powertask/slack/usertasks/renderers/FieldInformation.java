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

import com.slack.api.model.block.composition.BlockCompositions;
import com.slack.api.model.block.composition.PlainTextObject;
import java.util.Optional;
import org.camunda.bpm.engine.form.FormField;

public class FieldInformation {

  static Optional<Optional<String>> getConstraint(FormField formField, String name) {
    return formField.getValidationConstraints().stream()
        .filter(
            formFieldValidationConstraint -> formFieldValidationConstraint.getName().equals(name))
        .map(
            formFieldValidationConstraint ->
                Optional.ofNullable((String) formFieldValidationConstraint.getConfiguration()))
        .findFirst();
  }

  // Contrary to our own 'slack-' prefixed boolean properties, we don't check the config for true or
  // false,
  // merely the existence of the constraint is sufficient. This aligns with Camunda's Task List.
  public static boolean hasConstraint(FormField formField, String name) {
    return getConstraint(formField, name).isPresent();
  }

  // TODO, document throwing behaviour
  public static Optional<Long> getLongConstraint(FormField formField, String name) {
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

  static Optional<String> getStringProperty(FormField formField, String name) {
    if (!formField.getProperties().containsKey(name)) {
      return Optional.empty();
    } else {
      String value = formField.getProperties().get(name);
      if (value == null) {
        throw new RuntimeException("Property " + name + " is null!");
      } else {
        return Optional.of(value);
      }
    }
  }

  public static Optional<PlainTextObject> getPlainTextProperty(FormField formField, String name) {
    return getStringProperty(formField, name).map(BlockCompositions::plainText);
  }

  public static Optional<Boolean> getBooleanProperty(FormField formField, String name) {
    return getStringProperty(formField, name).map(value -> value.equals("true"));
  }
}
