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
package io.powertask.slack.apphome;

import io.powertask.slack.FormLike;
import java.util.Optional;
import org.camunda.bpm.model.bpmn.instance.StartEvent;

interface StartEventDetails extends FormLike {

  public Optional<String> initiatorVariableName();

  static StartEventDetails of(StartEvent startEvent, String processDefinitionId) {
    return new StartEventDetails() {
      @Override
      public Optional<String> initiatorVariableName() {
        return Optional.ofNullable(startEvent.getCamundaInitiator());
      }

      @Override
      public String getName() {
        return Optional.ofNullable(startEvent.getName()).orElse("Nameless start event");
      }

      @Override
      public String getProcessDefinitionId() {
        return processDefinitionId;
      }

      @Override
      public String getProcessDefinitionElementId() {
        return startEvent.getId();
      }
    };
  }
}
