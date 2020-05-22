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
import com.slack.api.model.block.element.PlainTextInputElement;
import com.slack.api.model.view.ViewState;
import io.powertask.slack.formfields.ImmutableLongField;
import io.powertask.slack.formfields.LongField;
import io.vavr.control.Either;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class LongFieldRendererTest {

  private ImmutableLongField.Builder getBaseField(String id) {
    return ImmutableLongField.builder().id(id).label(id).required(true);
  }

  @Test
  void renderBasicElement() {
    String id = "1234";

    LongField formField = getBaseField(id).build();

    BlockElement renderedElement = new LongFieldRenderer(formField).renderElement();

    BlockElement expectedElement = PlainTextInputElement.builder().actionId(id + "_long").build();

    assertEquals(expectedElement, renderedElement);
  }

  @Test
  void renderFullOptionsElement() {
    String id = "1234";
    long value = 713L;
    String placeholder = "Enter your long...";

    LongField formField = getBaseField(id).value(value).placeholder(placeholder).build();

    BlockElement renderedElement = new LongFieldRenderer(formField).renderElement();

    BlockElement expectedElement =
        PlainTextInputElement.builder()
            .initialValue(String.valueOf(value))
            .placeholder(plainText(placeholder))
            .actionId(id + "_long")
            .build();

    assertEquals(expectedElement, renderedElement);
  }

  @Test
  void extractValue() {
    String id = "1234";
    LongField formField = getBaseField(id).build();

    ViewState.Value value = new ViewState.Value();
    value.setValue("1234");

    Map<String, ViewState.Value> fields = Collections.singletonMap("1234_long", value);

    assertEquals(
        Either.right(Optional.of(1234L)), new LongFieldRenderer(formField).extractValue(fields));
  }

  @Test
  void extractValueMinMaxConstraint() {
    String id = "1234";
    LongField formField = getBaseField(id).min(10).max(100).build();

    // Value is below 'min' constraint
    ViewState.Value valueMin = new ViewState.Value();
    valueMin.setValue("5");
    Map<String, ViewState.Value> fieldsMin = Collections.singletonMap("1234_long", valueMin);

    assertEquals(
        Either.left("Minimum value is 10"),
        new LongFieldRenderer(formField).extractValue(fieldsMin));

    // Value is above 'max' constraint
    ViewState.Value valueMax = new ViewState.Value();
    valueMax.setValue("500");
    Map<String, ViewState.Value> fieldsMax = Collections.singletonMap("1234_long", valueMax);

    assertEquals(
        // The configured max is exclusive, but we report an inclusive value
        Either.left("Maximum value is 100"),
        new LongFieldRenderer(formField).extractValue(fieldsMax));
  }
}
