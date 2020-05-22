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

import com.slack.api.model.block.composition.BlockCompositions;
import com.slack.api.model.block.element.BlockElement;
import com.slack.api.model.block.element.PlainTextInputElement;
import com.slack.api.model.view.ViewState;
import io.powertask.slack.formfields.LongField;
import io.vavr.control.Either;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LongFieldRenderer extends AbstractFieldRenderer<Long> {
  private static final Logger logger = LoggerFactory.getLogger(LongFieldRenderer.class);

  private static final String FIELD_SUFFIX = "_long";
  private static final String CONSTRAINT_MIN = "min";
  // Be aware! In the Camunda Task List, this is an exclusive constraint. We mimic that.
  private static final String CONSTRAINT_MAX = "max";
  private final LongField longField;

  public LongFieldRenderer(LongField formField) {
    super(formField);
    longField = formField;
  }

  @Override
  protected BlockElement renderElement() {
    return PlainTextInputElement.builder()
        .initialValue(formField.value().map(Object::toString).orElse(null))
        .placeholder(longField.placeholder().map(BlockCompositions::plainText).orElse(null))
        .actionId(formField.id() + FIELD_SUFFIX)
        .build();
  }

  @Override
  public Either<String, Optional<Object>> extractValue(Map<String, ViewState.Value> viewState) {
    String value = viewState.get(formField.id() + FIELD_SUFFIX).getValue();
    if (value == null) {
      return Either.right(Optional.empty());
    } else {
      try {
        Long longValue = Long.valueOf(value);
        if (longField.min().isPresent() && longValue < longField.min().get()) {
          return Either.left("Minimum value is " + longField.min().get());
        }

        if (longField.max().isPresent() && longValue > longField.max().get()) {
          return Either.left("Maximum value is " + longField.max().get());
        }

        return Either.right(Optional.of(longValue));
      } catch (NumberFormatException e) {
        return Either.left("Invalid number");
      }
    }
  }
}
