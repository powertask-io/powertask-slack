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
import org.camunda.bpm.engine.variable.value.LongValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LongFieldRenderer extends AbstractFieldRenderer {
  private static final Logger logger = LoggerFactory.getLogger(LongFieldRenderer.class);

  private static final String FIELD_SUFFIX = "_long";
  private static final String CONSTRAINT_MIN = "min";
  // Be aware! In the Camunda Task List, this is an exclusive constraint. We mimic that.
  private static final String CONSTRAINT_MAX = "max";

  public LongFieldRenderer(FormField formField) {
    super(formField);
  }

  @Override
  protected BlockElement renderElement() {
    Long value = ((LongValue) formField.getValue()).getValue();

    return PlainTextInputElement.builder()
        .initialValue(Optional.ofNullable(value).map(Object::toString).orElse(null))
        .placeholder(
            FieldInformation.getPlainTextProperty(formField, PROPERTY_SLACK_PLACEHOLDER)
                .orElse(null))
        .actionId(formField.getId() + FIELD_SUFFIX)
        .build();
  }

  @Override
  public Either<String, Optional<Object>> extractValue(
      FormField formField, Map<String, ViewState.Value> viewState) {
    String value = viewState.get(formField.getId() + FIELD_SUFFIX).getValue();
    if (value == null) {
      return Either.right(Optional.empty());
    } else {
      try {
        Long longValue = Long.valueOf(value);
        Optional<Long> minConstraint =
            FieldInformation.getLongConstraint(formField, CONSTRAINT_MIN);
        if (minConstraint.isPresent() && longValue < minConstraint.get()) {
          return Either.left("Minimum value is " + minConstraint.get());
        }

        Optional<Long> maxConstraint =
            FieldInformation.getLongConstraint(formField, CONSTRAINT_MAX);
        if (maxConstraint.isPresent() && longValue >= maxConstraint.get()) {
          return Either.left("Maximum value is " + (maxConstraint.get() - 1));
        }

        return Either.right(Optional.of(longValue));
      } catch (NumberFormatException e) {
        return Either.left("Invalid number");
      }
    }
  }
}
