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
import com.slack.api.model.block.element.DatePickerElement;
import com.slack.api.model.view.ViewState;
import io.powertask.slack.formfields.DateField;
import io.vavr.control.Either;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Optional;

public class DateFieldRenderer extends AbstractFieldRenderer<LocalDate> {
  private static final String FIELD_SUFFIX = "_date";
  private final DateField dateField;

  public DateFieldRenderer(DateField formField) {
    super(formField);
    dateField = formField;
  }

  @Override
  protected BlockElement renderElement() {
    return DatePickerElement.builder()
        .placeholder(dateField.placeholder().map(BlockCompositions::plainText).orElse(null))
        .initialDate(dateField.value().map(LocalDate::toString).orElse(null))
        .actionId(formField.id() + FIELD_SUFFIX)
        .build();
  }

  @Override
  public Either<String, Optional<Object>> extractValue(Map<String, ViewState.Value> viewState) {
    String dateString = viewState.get(formField.id() + FIELD_SUFFIX).getSelectedDate();
    if (dateString == null) {
      return Either.right(Optional.empty());
    } else {
      try {
        return Either.right(Optional.of(LocalDate.parse(dateString)));
      } catch (DateTimeParseException e) {
        return Either.left("Failed to parse date");
      }
    }
  }
}
