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

import static io.powertask.slack.FunctionOps.wrapExceptions;

import com.slack.api.model.block.element.BlockElement;
import com.slack.api.model.block.element.DatePickerElement;
import com.slack.api.model.view.ViewState;
import io.powertask.slack.usertasks.renderers.FieldInformation;
import io.vavr.control.Either;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.Optional;
import org.camunda.bpm.engine.form.FormField;

public class DateFieldRenderer extends AbstractFieldRenderer {
  // Two libraries, two stringly typed dates ¯\_(ツ)_/¯
  private static final SimpleDateFormat CAMUNDA_DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");
  private static final SimpleDateFormat SLACK_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

  private static final String FIELD_SUFFIX = "_date";

  public DateFieldRenderer(FormField formField) {
    super(formField);
  }

  @Override
  protected BlockElement renderElement() {
    return DatePickerElement.builder()
        .placeholder(
            FieldInformation.getPlainTextProperty(formField, PROPERTY_SLACK_PLACEHOLDER)
                .orElse(null))
        .initialDate(getInitialDateString())
        .actionId(formField.getId() + FIELD_SUFFIX)
        .build();
  }

  // The Form Value of a date is always a string, even if the process variable is a Date.
  private String getInitialDateString() {
    return Optional.ofNullable((String) formField.getValue().getValue())
        .map(wrapExceptions(CAMUNDA_DATE_FORMAT::parse))
        .map(SLACK_DATE_FORMAT::format)
        .orElse(null);
  }

  @Override
  public Either<String, Optional<Object>> extractValue(
      FormField formField, Map<String, ViewState.Value> viewState) {
    String dateString = viewState.get(formField.getId() + FIELD_SUFFIX).getSelectedDate();
    if (dateString == null) {
      return Either.right(Optional.empty());
    } else {
      try {
        return Either.right(
            Optional.of(SLACK_DATE_FORMAT.parse(dateString)).map(CAMUNDA_DATE_FORMAT::format));
      } catch (ParseException e) {
        return Either.left("Failed to parse date");
      }
    }
  }
}
