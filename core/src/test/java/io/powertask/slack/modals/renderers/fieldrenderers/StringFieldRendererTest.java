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
import io.powertask.slack.formfields.ImmutableStringField;
import io.powertask.slack.formfields.StringField;
import io.vavr.control.Either;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class StringFieldRendererTest {

  private ImmutableStringField.Builder getBaseField(String id) {
    return ImmutableStringField.builder().id(id).label(id).multiline(false).required(true);
  }

  @Test
  void renderBasicElement() {
    String id = "1234";

    StringField formField = getBaseField(id).build();

    BlockElement renderedElement =
        new io.powertask.slack.modals.renderers.fieldrenderers.StringFieldRenderer(formField)
            .renderElement();

    BlockElement expectedElement =
        PlainTextInputElement.builder().multiline(false).actionId(id + "_text").build();

    assertEquals(expectedElement, renderedElement);
  }

  @Test
  void renderFullOptionsElement() {
    String id = "1234";
    String value = "the-value";
    String placeholder = "Fill it out...";

    StringField formField =
        getBaseField(id)
            .value(value)
            .multiline(true)
            .placeholder(placeholder)
            .minLength(10)
            .maxLength(30)
            .build();

    BlockElement renderedElement = new StringFieldRenderer(formField).renderElement();

    BlockElement expectedElement =
        PlainTextInputElement.builder()
            .initialValue(value)
            .multiline(true)
            .minLength(10)
            .maxLength(30)
            .placeholder(plainText(placeholder))
            .actionId(id + "_text")
            .build();

    assertEquals(expectedElement, renderedElement);
  }

  @Test
  void extractValue() {
    String id = "1234";
    StringField formField = getBaseField(id).build();

    ViewState.Value value = new ViewState.Value();
    value.setValue("Hello, there!");

    Map<String, ViewState.Value> fields = Collections.singletonMap("1234_text", value);

    assertEquals(
        Either.right(Optional.of("Hello, there!")),
        new StringFieldRenderer(formField).extractValue(fields));
  }
}
