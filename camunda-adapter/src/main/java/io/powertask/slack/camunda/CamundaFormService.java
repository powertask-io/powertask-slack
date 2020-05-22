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

import io.powertask.slack.Form;
import io.powertask.slack.FormService;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CamundaFormService implements FormService {

  private final org.camunda.bpm.engine.FormService formService;

  public CamundaFormService(org.camunda.bpm.engine.FormService formService) {
    this.formService = formService;
  }

  @Override
  public Optional<Form> taskForm(String taskId) {
    return Optional.ofNullable(formService.getTaskFormData(taskId))
        .map(CamundaFormMapper::fromFormData);
  }

  @Override
  public Optional<Form> startForm(String processDefinitionId) {
    return Optional.ofNullable(formService.getStartFormData(processDefinitionId))
        .map(CamundaFormMapper::fromFormData);
  }

  @Override
  public void submitTaskForm(String taskId, Map<String, Object> variables) {
    formService.submitTaskForm(taskId, mapVariableValues(variables));
  }

  String submitStartForm(String processDefinitionId, Map<String, Object> variables) {
    return formService.submitStartForm(processDefinitionId, mapVariableValues(variables)).getId();
  }

  // We get back LocalDate object from Powertask, but Camunda form submit wants
  // a string date.
  private Map<String, Object> mapVariableValues(Map<String, Object> variables) {
    Map<String, Object> updated = new HashMap<>(variables);
    updated.replaceAll(
        (key, value) -> {
          if (value instanceof LocalDate) {
            return ((LocalDate) value).format(FormFieldMapper.CAMUNDA_DATE_FORMATTER);
          } else {
            return value;
          }
        });
    return updated;
  }
}
