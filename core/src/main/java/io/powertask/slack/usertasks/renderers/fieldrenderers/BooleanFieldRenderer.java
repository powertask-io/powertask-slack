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

import static com.slack.api.model.block.composition.BlockCompositions.option;
import static com.slack.api.model.block.composition.BlockCompositions.plainText;

import com.slack.api.model.block.composition.OptionObject;
import com.slack.api.model.block.element.BlockElement;
import com.slack.api.model.block.element.RadioButtonsElement;
import com.slack.api.model.view.ViewState;
import io.powertask.slack.usertasks.renderers.FieldInformation;
import io.vavr.control.Either;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.camunda.bpm.engine.form.FormField;
import org.camunda.bpm.engine.variable.value.BooleanValue;

// TODO, we should also be able to render such a field as a select, or as a checkbox.
public class BooleanFieldRenderer extends AbstractFieldRenderer {

  private static final String FIELD_SUFFIX = "_boolean";
  public static final String PROPERTY_SLACK_TRUE_LABEL = "slack-true-label";
  public static final String PROPERTY_SLACK_FALSE_LABEL = "slack-false-label";

  public BooleanFieldRenderer(FormField formField) {
    super(formField);
  }

  @Override
  protected BlockElement renderElement() {

    List<OptionObject> options =
        Arrays.asList(
            option(
                FieldInformation.getPlainTextProperty(formField, PROPERTY_SLACK_TRUE_LABEL)
                    .orElse(plainText("Yes")),
                "true"),
            option(
                FieldInformation.getPlainTextProperty(formField, PROPERTY_SLACK_FALSE_LABEL)
                    .orElse(plainText("No")),
                "false"));

    Optional<String> initialValue =
        Optional.ofNullable((BooleanValue) formField.getValue())
            .map(BooleanValue::getValue)
            .map(Object::toString);
    Optional<OptionObject> initialOption =
        initialValue.flatMap(
            value -> options.stream().filter(o -> o.getValue().equals(value)).findFirst());

    return RadioButtonsElement.builder()
        .actionId(formField.getId() + FIELD_SUFFIX)
        .initialOption(initialOption.orElse(null))
        .options(options)
        .build();
  }

  @Override
  public Either<String, Optional<Object>> extractValue(
      FormField formField, Map<String, ViewState.Value> viewState) {
    return Either.right(
        Optional.ofNullable(viewState.get(formField.getId() + FIELD_SUFFIX).getSelectedOption())
            .map(ViewState.SelectedOption::getValue));
  }
}
