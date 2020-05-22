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
package io.powertask.slack.formfields;

import io.powertask.slack.FormField;
import java.util.Map;
import java.util.Optional;
import org.immutables.value.Value.Check;
import org.immutables.value.Value.Immutable;

@Immutable
public interface EnumField extends FormField<String> {

  // TODO, move to subtype
  Map<String, EnumValue> values();

  @Immutable
  interface EnumValue {
    String text();

    Optional<String> description();
  }

  @Check
  default void check() {
    value()
        .ifPresent(
            v -> {
              if (!values().containsKey(v)) {
                throw new IllegalArgumentException("Value is not found in the set of values.");
              }
            });
  }
}
