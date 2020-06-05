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

import com.slack.api.model.block.composition.BlockCompositions;
import com.slack.api.model.block.element.BlockElement;
import com.slack.api.model.block.element.PlainTextInputElement;
import com.slack.api.model.block.element.UsersSelectElement;
import com.slack.api.model.view.ViewState;
import io.powertask.slack.formfields.StringField;
import io.powertask.slack.formfields.UserField;
import io.vavr.control.Either;
import java.util.Map;
import java.util.Optional;

public class UserFieldRenderer extends AbstractFieldRenderer<String> {

  private static final String FIELD_SUFFIX = "_user";
  private final UserField userField;

  public UserFieldRenderer(UserField formField) {
    super(formField);
    userField = formField;
  }

  @Override
  protected BlockElement renderElement() {
    return UsersSelectElement.builder()
        .placeholder(userField.placeholder().map(BlockCompositions::plainText).orElse(null))
        .initialUser(userField.value().orElse(null))
        .actionId(formField.id() + FIELD_SUFFIX)
        .build();
  }

  @Override
  public Either<String, Optional<Object>> extractValue(Map<String, ViewState.Value> viewState) {
    return Either.right(
        Optional.ofNullable(viewState.get(formField.id() + FIELD_SUFFIX).getSelectedUser()));
  }
}
