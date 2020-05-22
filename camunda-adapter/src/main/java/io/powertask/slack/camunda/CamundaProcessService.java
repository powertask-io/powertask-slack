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

import io.powertask.slack.ImmutableProcess;
import io.powertask.slack.Process;
import io.powertask.slack.ProcessService;
import io.powertask.slack.StartEvent;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;

public class CamundaProcessService implements ProcessService {

  private final ProcessEngine processEngine;
  private final CamundaFormService formService;
  private final StartEventMapper startEventMapper;

  public CamundaProcessService(
      ProcessEngine processEngine,
      CamundaFormService formService,
      PropertiesResolver propertiesResolver) {
    this.processEngine = processEngine;
    this.formService = formService;
    this.startEventMapper = new StartEventMapper(propertiesResolver);
  }

  @Override
  public List<Process> startableProcesses(String engineUserId) {

    List<ProcessDefinition> processDefinitions =
        processEngine
            .getRepositoryService()
            .createProcessDefinitionQuery()
            .startableByUser(engineUserId)
            .startableInTasklist()
            .active()
            .list();

    return processDefinitions.stream()
        .map(
            processDefinition ->
                ImmutableProcess.builder()
                    .id(processDefinition.getId())
                    .name(
                        Optional.ofNullable(processDefinition.getName())
                            .orElseGet(
                                () -> "Nameless process with id " + processDefinition.getId()))
                    // TODO, this corresponds to the 'documentation' field in the Modeler. I think
                    // we
                    // should use the extension similar to tasks.
                    .description(Optional.ofNullable(processDefinition.getDescription()))
                    .build())
        .collect(Collectors.toList());
  }

  @Override
  public StartEvent startEvent(String processDefinitionId) {
    BpmnModelInstance def =
        processEngine.getRepositoryService().getBpmnModelInstance(processDefinitionId);
    Collection<org.camunda.bpm.model.bpmn.instance.StartEvent> startEvents =
        def.getModelElementsByType(org.camunda.bpm.model.bpmn.instance.StartEvent.class);
    org.camunda.bpm.model.bpmn.instance.StartEvent startEvent = startEvents.iterator().next();
    return startEventMapper.of(startEvent, processDefinitionId);
  }

  @Override
  public String startProcess(String processDefinitionId, Map<String, Object> processVariables) {
    return processEngine
        .getRuntimeService()
        .startProcessInstanceById(processDefinitionId, processVariables)
        .getId();
  }

  @Override
  public String startProcessWithForm(String processDefinitionId, Map<String, Object> variables) {
    return formService.submitStartForm(processDefinitionId, variables);
  }
}
