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

import com.slack.api.model.block.element.BlockElement;
import com.slack.api.model.block.element.PlainTextInputElement;
import com.slack.api.model.view.ViewState;
import io.powertask.slack.FieldInformation;
import io.vavr.control.Either;
import java.util.Map;
import java.util.Optional;
import org.camunda.bpm.engine.form.FormField;
import org.camunda.bpm.engine.variable.value.StringValue;

public class StringFieldRenderer extends AbstractFieldRenderer {

  private static final String FIELD_SUFFIX = "_text";
  private static final String PROPERTY_SLACK_MULTILINE = "slack-multiline";
  private static final String CONSTRAINT_MINLENGTH = "minlength";
  private static final String CONSTRAINT_MAXLENGTH = "maxlength";

  public StringFieldRenderer(FormField formField) {
    super(formField);
  }

  @Override
  protected BlockElement renderElement() {
    return PlainTextInputElement.builder()
        .initialValue(((StringValue) formField.getValue()).getValue())
        .multiline(
            FieldInformation.getBooleanProperty(formField, PROPERTY_SLACK_MULTILINE).orElse(false))
        .minLength(
            FieldInformation.getLongConstraint(formField, CONSTRAINT_MINLENGTH)
                // TODO: The int can overflow...
                .map(Long::intValue)
                .orElse(null))
        .maxLength(
            FieldInformation.getLongConstraint(formField, CONSTRAINT_MAXLENGTH)
                .map(Long::intValue)
                .orElse(null))
        .placeholder(
            FieldInformation.getPlainTextProperty(formField, PROPERTY_SLACK_PLACEHOLDER)
                .orElse(null))
        .actionId(formField.getId() + FIELD_SUFFIX)
        .build();
  }

  @Override
  public Either<String, Optional<Object>> extractValue(
      FormField formField, Map<String, ViewState.Value> viewState) {
    return Either.right(
        Optional.ofNullable(viewState.get(formField.getId() + FIELD_SUFFIX).getValue()));
  }
}
