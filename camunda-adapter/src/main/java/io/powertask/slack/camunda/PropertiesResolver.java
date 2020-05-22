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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.bpmn.instance.ExtensionElements;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperties;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperty;

public class PropertiesResolver {

  protected final RepositoryService repositoryService;

  public PropertiesResolver(RepositoryService repositoryService) {
    this.repositoryService = repositoryService;
  }

  public Map<String, Optional<String>> getProperties(
      String processDefinitionId, String processDefinitionElementId) {
    BpmnModelInstance bpmnModelInstance =
        repositoryService.getBpmnModelInstance(processDefinitionId);

    BaseElement bpmnElement = bpmnModelInstance.getModelElementById(processDefinitionElementId);

    ExtensionElements extensionElements = bpmnElement.getExtensionElements();
    if (extensionElements == null) {
      return Collections.emptyMap();
    }

    List<CamundaProperties> propertiesSets =
        extensionElements.getElementsQuery().filterByType(CamundaProperties.class).list();

    return propertiesSets.stream()
        .flatMap(set -> set.getCamundaProperties().stream())
        .collect(
            Collectors.toMap(
                CamundaProperty::getCamundaName,
                camundaProperty -> Optional.ofNullable(camundaProperty.getCamundaValue()),
                (v1, v2) -> v1));
  }

  Optional<String> getProperty(
      String processDefinitionId, String processDefinitionElementId, String name) {
    return getPropertyWithOptionalValue(processDefinitionId, processDefinitionElementId, name)
        .flatMap(Function.identity());
  }

  Optional<Optional<String>> getPropertyWithOptionalValue(
      String processDefinitionId, String processDefinitionElementId, String name) {
    return Optional.ofNullable(
        getProperties(processDefinitionId, processDefinitionElementId).get(name));
  }
}
