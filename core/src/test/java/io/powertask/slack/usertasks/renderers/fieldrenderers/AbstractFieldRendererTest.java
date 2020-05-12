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

import static com.slack.api.model.block.composition.BlockCompositions.plainText;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.slack.api.model.block.InputBlock;
import com.slack.api.model.block.UnknownBlockElement;
import com.slack.api.model.block.element.BlockElement;
import com.slack.api.model.view.ViewState;
import io.vavr.control.Either;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.camunda.bpm.engine.form.FormField;
import org.camunda.bpm.engine.form.FormFieldValidationConstraint;
import org.camunda.bpm.engine.impl.form.FormFieldImpl;
import org.camunda.bpm.engine.impl.form.FormFieldValidationConstraintImpl;
import org.camunda.bpm.engine.impl.form.type.StringFormType;
import org.junit.jupiter.api.Test;

public class AbstractFieldRendererTest {

  private static final BlockElement inner = new UnknownBlockElement("foo");

  static class MyFieldRenderer extends AbstractFieldRenderer {
    protected MyFieldRenderer(FormField formField) {
      super(formField);
    }

    protected BlockElement renderElement() {
      return inner;
    }

    public Either<String, Optional<Object>> extractValue(
        FormField formField, Map<String, ViewState.Value> viewState) {
      return null;
    }
  }

  @Test
  public void renderBasicBlock() {
    String label = "The Label";

    FormFieldImpl formField = new FormFieldImpl();
    formField.setType(new StringFormType());
    formField.setLabel(label);

    InputBlock inputBlock = new MyFieldRenderer(formField).render(formField);

    InputBlock expectedInputBlock =
        InputBlock.builder()
            .blockId(formField.getId())
            .optional(true)
            .label(plainText(label))
            .element(inner)
            .build();

    assertEquals(expectedInputBlock, inputBlock);
  }

  @Test
  public void renderFullOptionsBlock() {
    String label = "The Label";
    String hint = "The Hint";

    FormFieldImpl formField = new FormFieldImpl();
    formField.setType(new StringFormType());
    formField.setLabel(label);

    Map<String, String> properties = new HashMap<>();
    properties.put("slack-hint", hint);
    formField.setProperties(properties);

    ArrayList<FormFieldValidationConstraint> constraints = new ArrayList<>();
    constraints.add(new FormFieldValidationConstraintImpl("required", "true"));
    formField.setValidationConstraints(constraints);

    InputBlock inputBlock = new MyFieldRenderer(formField).render(formField);

    InputBlock expectedInputBlock =
        InputBlock.builder()
            .blockId(formField.getId())
            .optional(false)
            .hint(plainText(hint))
            .label(plainText(label))
            .element(inner)
            .build();

    assertEquals(expectedInputBlock, inputBlock);
  }
}
