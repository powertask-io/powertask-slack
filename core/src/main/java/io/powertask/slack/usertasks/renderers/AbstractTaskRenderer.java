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
package io.powertask.slack.usertasks.renderers;

import static com.slack.api.model.block.Blocks.section;
import static com.slack.api.model.block.composition.BlockCompositions.markdownText;

import com.slack.api.bolt.context.Context;
import com.slack.api.bolt.response.Response;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.composition.MarkdownTextObject;
import com.slack.api.model.block.composition.TextObject;
import io.powertask.slack.usertasks.TaskDetails;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperties;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO, support Confirmation dialogs
public abstract class AbstractTaskRenderer implements TaskRenderer {

  protected static final String PROPERTY_SLACK_DESCRIPTION = "slack-description";
  protected static final String PROPERTY_SLACK_SHOW_VARIABLES = "slack-show-variables";
  private static final Logger logger = LoggerFactory.getLogger(AbstractTaskRenderer.class);
  protected final ProcessEngine processEngine;
  protected final BiFunction<TaskResult, Context, Response> submissionListener;

  AbstractTaskRenderer(
      ProcessEngine processEngine, BiFunction<TaskResult, Context, Response> submissionListener) {
    this.processEngine = processEngine;
    this.submissionListener = submissionListener;
  }

  protected List<LayoutBlock> getDescriptionBlocks(TaskDetails task) {
    return getProperty(task, PROPERTY_SLACK_DESCRIPTION)
        .map(
            description ->
                Collections.<LayoutBlock>singletonList(
                    section(section -> section.text(markdownText(description)))))
        .orElse(Collections.emptyList());
  }

  protected List<LayoutBlock> getVariablesBlocks(TaskDetails task) {
    if (hasProperty(task, PROPERTY_SLACK_SHOW_VARIABLES)) {
      Optional<String> requestedVariables = getProperty(task, PROPERTY_SLACK_SHOW_VARIABLES);

      Map<String, Object> variables =
          requestedVariables
              .map(
                  variablesToShow -> {
                    Set<String> variableNames =
                        Arrays.stream(variablesToShow.split(","))
                            .map(String::trim)
                            .collect(Collectors.toSet());
                    return processEngine.getTaskService().getVariables(task.getId(), variableNames);
                  })
              .orElseGet(() -> processEngine.getTaskService().getVariables(task.getId()));

      List<TextObject> fields =
          variables.entrySet().stream()
              .map(
                  entrySet ->
                      MarkdownTextObject.builder()
                          .text("*" + entrySet.getKey() + ":*\n" + entrySet.getValue())
                          .build())
              .collect(Collectors.toList());

      if (fields.isEmpty()) {
        // When I'm nice, I'm really nice.
        if (requestedVariables.equals(Optional.of("true"))) {
          logger.warn(
              "Property "
                  + PROPERTY_SLACK_SHOW_VARIABLES
                  + " is set to 'true', this is unlikely to be what you want, since there's no variable with that name in the process. Leave it empty to show all variables, or use a comma-separated list of variable names to display.");
        } else {
          logger.warn(
              "Property "
                  + PROPERTY_SLACK_SHOW_VARIABLES
                  + " is set for task "
                  + task.getName()
                  + ", but no variables found!");
        }
        return Collections.emptyList();
      } else {
        return Collections.singletonList(section(section -> section.fields(fields)));
      }
    } else {
      return Collections.emptyList();
    }
  }

  protected boolean hasProperty(TaskDetails task, String name) {
    return getPropertyWithOptionalValue(task, name).isPresent();
  }

  protected Optional<String> getProperty(TaskDetails task, String name) {
    return getPropertyWithOptionalValue(task, name).flatMap(Function.identity());
  }

  // TODO, this is likely slow. How to best cache this?
  protected Optional<Optional<String>> getPropertyWithOptionalValue(TaskDetails task, String name) {
    BpmnModelInstance bpmnModelInstance =
        processEngine.getRepositoryService().getBpmnModelInstance(task.getProcessDefinitionId());
    org.camunda.bpm.model.bpmn.instance.UserTask userTask =
        bpmnModelInstance.getModelElementById(task.getTaskDefinitionKey());
    List<CamundaProperties> propertiesSets =
        userTask
            .getExtensionElements()
            .getElementsQuery()
            .filterByType(CamundaProperties.class)
            .list();
    Collection<CamundaProperty> properties =
        propertiesSets.stream()
            .flatMap(set -> set.getCamundaProperties().stream())
            .collect(Collectors.toList());
    return properties.stream()
        .filter(property -> property.getCamundaName().equals(name))
        .findFirst()
        .map(property -> Optional.ofNullable(property.getCamundaValue()));
  }

  protected TaskDetails getTaskById(String taskId) {
    Task task = processEngine.getTaskService().createTaskQuery().taskId(taskId).singleResult();
    return TaskDetails.of(task);
  }
}
