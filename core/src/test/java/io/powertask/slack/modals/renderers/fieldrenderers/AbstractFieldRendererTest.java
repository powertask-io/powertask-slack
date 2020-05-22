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

import com.slack.api.model.block.InputBlock;
import com.slack.api.model.block.UnknownBlockElement;
import com.slack.api.model.block.element.BlockElement;
import com.slack.api.model.view.ViewState;
import io.powertask.slack.FormField;
import io.powertask.slack.formfields.ImmutableStringField;
import io.vavr.control.Either;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

public class AbstractFieldRendererTest {

  private static final BlockElement inner = new UnknownBlockElement("foo");

  @Test
  public void renderBasicBlock() {
    String label = "The Label";
    String id = "foo";

    FormField<String> formField =
        ImmutableStringField.builder().id(id).label(label).required(false).multiline(false).build();

    InputBlock inputBlock = new MyFieldRenderer(formField).render();

    InputBlock expectedInputBlock =
        InputBlock.builder()
            .blockId(formField.id())
            .optional(true)
            .label(plainText(label))
            .element(inner)
            .build();

    assertEquals(expectedInputBlock, inputBlock);
  }

  @Test
  public void renderFullOptionsBlock() {
    String id = "foo";
    String label = "The Label";
    String hint = "The Hint";

    FormField<String> formField =
        ImmutableStringField.builder()
            .id(id)
            .label(label)
            .hint(Optional.of(hint))
            .required(true)
            .multiline(false)
            .build();

    InputBlock inputBlock = new MyFieldRenderer(formField).render();

    InputBlock expectedInputBlock =
        InputBlock.builder()
            .blockId(formField.id())
            .optional(false)
            .hint(plainText(hint))
            .label(plainText(label))
            .element(inner)
            .build();

    assertEquals(expectedInputBlock, inputBlock);
  }

  static class MyFieldRenderer extends AbstractFieldRenderer<String> {

    protected MyFieldRenderer(FormField<String> formField) {
      super(formField);
    }

    protected BlockElement renderElement() {
      return inner;
    }

    public Either<String, Optional<Object>> extractValue(Map<String, ViewState.Value> viewState) {
      return null;
    }
  }
}
