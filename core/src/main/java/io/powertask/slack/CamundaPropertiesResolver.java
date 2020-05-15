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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.bpmn.instance.ExtensionElements;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperties;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperty;

public class CamundaPropertiesResolver<T extends FormLike> implements PropertiesResolver<T> {
  protected final RepositoryService repositoryService;

  public CamundaPropertiesResolver(RepositoryService repositoryService) {
    this.repositoryService = repositoryService;
  }

  @Override
  public Map<String, Optional<String>> getProperties(T t) {
    BpmnModelInstance bpmnModelInstance =
        repositoryService.getBpmnModelInstance(t.getProcessDefinitionId());

    // TODO, this is not really right. A valid 'FormLike' instance may make this crash.
    BaseElement bpmnElement =
        bpmnModelInstance.getModelElementById(t.getProcessDefinitionElementId());

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
}
