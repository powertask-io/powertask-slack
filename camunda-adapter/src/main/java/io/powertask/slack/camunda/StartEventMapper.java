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
package io.powertask.slack.camunda;

import static io.powertask.slack.camunda.TaskMapper.PROPERTY_SLACK_DESCRIPTION;
import static io.powertask.slack.camunda.TaskMapper.PROPERTY_SLACK_TITLE;

import io.powertask.slack.StartEvent;
import java.util.Optional;

public class StartEventMapper {

  private final PropertiesResolver propertiesResolver;

  public StartEventMapper(PropertiesResolver propertiesResolver) {
    this.propertiesResolver = propertiesResolver;
  }

  public StartEvent of(
      org.camunda.bpm.model.bpmn.instance.StartEvent startEvent, String processDefinitionId) {
    return new StartEvent() {

      @Override
      public String name() {
        return Optional.ofNullable(startEvent.getName()).orElse("Nameless start event");
      }

      @Override
      public String processDefinitionId() {
        return processDefinitionId;
      }

      @Override
      public String title() {
        return propertiesResolver
            .getProperty(processDefinitionId, startEvent.getId(), PROPERTY_SLACK_TITLE)
            .orElse(name());
      }

      @Override
      public Optional<String> description() {
        return propertiesResolver.getProperty(
            processDefinitionId, startEvent.getId(), PROPERTY_SLACK_DESCRIPTION);
      }

      @Override
      public Optional<String> initiatorVariableName() {
        return Optional.ofNullable(startEvent.getCamundaInitiator());
      }
    };
  }
}
