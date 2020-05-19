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
package io.powertask.slack;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface VariablesResolver<T> {
  Map<String, Object> getVariables(T t);

  Map<String, Object> getVariables(T t, Set<String> variableNames);

  default Optional<Object> getVariable(T t, String variableName) {
    return Optional.ofNullable(
        getVariables(t, Collections.singleton(variableName)).get(variableName));
  }
}
