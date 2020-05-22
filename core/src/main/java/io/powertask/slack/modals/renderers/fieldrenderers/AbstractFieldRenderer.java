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

import com.slack.api.model.block.InputBlock;
import com.slack.api.model.block.composition.BlockCompositions;
import com.slack.api.model.block.element.BlockElement;
import io.powertask.slack.FormField;

public abstract class AbstractFieldRenderer<T> implements FieldRenderer {

  protected final FormField<T> formField;

  protected AbstractFieldRenderer(FormField<T> formField) {
    this.formField = formField;
  }

  protected abstract BlockElement renderElement();

  @Override
  public InputBlock render() {
    return InputBlock.builder()
        .blockId(formField.id())
        .optional(!formField.required())
        .label(plainText(formField.label()))
        .hint(formField.hint().map(BlockCompositions::plainText).orElse(null))
        .element(renderElement())
        .build();
  }
}
