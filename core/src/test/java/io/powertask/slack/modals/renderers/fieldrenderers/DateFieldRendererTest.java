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

import static com.slack.api.model.block.composition.BlockCompositions.plainText;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.slack.api.model.block.element.BlockElement;
import com.slack.api.model.block.element.DatePickerElement;
import com.slack.api.model.view.ViewState;
import com.slack.api.model.view.ViewState.SelectedOption;
import io.powertask.slack.formfields.DateField;
import io.powertask.slack.formfields.ImmutableDateField;
import io.vavr.control.Either;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DateFieldRendererTest {

  static String camundaDatePattern = "dd/MM/yyyy";

  private ImmutableDateField.Builder getBaseField(String id) {
    return ImmutableDateField.builder().id(id).label(id).required(true);
  }

  @Test
  void renderBasicElement() {
    String id = "1234";

    DateField formField = getBaseField(id).build();

    BlockElement renderedElement = new DateFieldRenderer(formField).renderElement();

    BlockElement expectedElement =
        DatePickerElement.builder().actionId(formField.id() + "_date").build();

    assertEquals(expectedElement, renderedElement);
  }

  @Test
  void renderFullOptionsElement() {
    String id = "1234";
    String placeholder = "placeholder";
    LocalDate initialDate = LocalDate.of(2010, 3, 21);

    DateField formField = getBaseField(id).value(initialDate).placeholder(placeholder).build();

    BlockElement renderedElement = new DateFieldRenderer(formField).renderElement();

    BlockElement expectedElement =
        DatePickerElement.builder()
            .actionId(formField.id() + "_date")
            .placeholder(plainText(placeholder))
            .initialDate("2010-03-21")
            .build();

    assertEquals(expectedElement, renderedElement);
  }

  @Test
  void extractValue() {
    String id = "1234";
    DateField formField = getBaseField(id).build();

    SelectedOption selectedOption = new SelectedOption();
    selectedOption.setValue("true");

    ViewState.Value value = new ViewState.Value();
    value.setSelectedDate("2010-03-15");

    Map<String, ViewState.Value> fields = Collections.singletonMap("1234_date", value);

    assertEquals(
        Either.right(Optional.of(LocalDate.of(2010, 3, 15))),
        new DateFieldRenderer(formField).extractValue(fields));
  }
}
