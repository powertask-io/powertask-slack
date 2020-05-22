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

import static com.slack.api.model.block.composition.BlockCompositions.option;
import static com.slack.api.model.block.composition.BlockCompositions.plainText;

import com.slack.api.model.block.composition.BlockCompositions;
import com.slack.api.model.block.composition.OptionObject;
import com.slack.api.model.block.composition.TextObject;
import com.slack.api.model.block.element.BlockElement;
import com.slack.api.model.block.element.RadioButtonsElement;
import com.slack.api.model.view.ViewState;
import io.powertask.slack.formfields.BooleanField;
import io.vavr.control.Either;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// TODO, we should also be able to render such a field as a select, or as a checkbox.
public class BooleanFieldRenderer extends AbstractFieldRenderer<Boolean> {

  private final BooleanField booleanField;

  private static final String FIELD_SUFFIX = "_boolean";
  public static final String PROPERTY_SLACK_TRUE_LABEL = "slack-true-label";
  public static final String PROPERTY_SLACK_FALSE_LABEL = "slack-false-label";

  public BooleanFieldRenderer(BooleanField formField) {
    super(formField);
    booleanField = formField;
  }

  @Override
  protected BlockElement renderElement() {

    List<OptionObject> options =
        Arrays.asList(
            option(
                booleanField
                    .trueLabel()
                    .<TextObject>map(BlockCompositions::markdownText)
                    .orElse(plainText("Yes")),
                "true"),
            option(
                booleanField
                    .falseLabel()
                    .<TextObject>map(BlockCompositions::markdownText)
                    .orElse(plainText("No")),
                "false"));

    Optional<String> initialValue = booleanField.value().map(Object::toString);
    Optional<OptionObject> initialOption =
        initialValue.flatMap(
            value -> options.stream().filter(o -> o.getValue().equals(value)).findFirst());

    return RadioButtonsElement.builder()
        .actionId(booleanField.id() + FIELD_SUFFIX)
        .initialOption(initialOption.orElse(null))
        .options(options)
        .build();
  }

  @Override
  public Either<String, Optional<Object>> extractValue(Map<String, ViewState.Value> viewState) {
    return Either.right(
        Optional.ofNullable(viewState.get(formField.id() + FIELD_SUFFIX).getSelectedOption())
            .map(ViewState.SelectedOption::getValue));
  }
}
