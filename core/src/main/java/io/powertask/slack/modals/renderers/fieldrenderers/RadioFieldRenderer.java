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

import com.slack.api.model.block.composition.BlockCompositions;
import com.slack.api.model.block.composition.OptionObject;
import com.slack.api.model.block.element.BlockElement;
import com.slack.api.model.block.element.RadioButtonsElement;
import com.slack.api.model.view.ViewState;
import io.powertask.slack.formfields.EnumField;
import io.vavr.control.Either;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

// TODO, support drop-down
public class RadioFieldRenderer extends AbstractFieldRenderer<String> {

  private static final String FIELD_SUFFIX = "_enum";
  private static final String PROPERTY_SLACK_DESCRIPTION_PREFIX = "slack-description-";
  private final EnumField enumField;

  public RadioFieldRenderer(EnumField formField) {
    super(formField);
    enumField = formField;
  }

  @Override
  protected BlockElement renderElement() {
    @SuppressWarnings("unchecked")
    Map<String, EnumField.EnumValue> values = enumField.values();

    List<OptionObject> options =
        values.entrySet().stream()
            .map(
                entry ->
                    OptionObject.builder()
                        .value(entry.getKey())
                        .text(plainText(entry.getValue().text()))
                        .description(
                            entry
                                .getValue()
                                .description()
                                .map(BlockCompositions::plainText)
                                .orElse(null))
                        .build())
            .collect(Collectors.toList());

    Optional<OptionObject> initialOption =
        formField
            .value()
            .flatMap(
                key ->
                    // TODO, should we throw instead of silently fail if this can't be found?
                    options.stream()
                        .filter(optionObject -> optionObject.getValue().equals(key))
                        .findFirst());

    return RadioButtonsElement.builder()
        .actionId(formField.id() + FIELD_SUFFIX)
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
