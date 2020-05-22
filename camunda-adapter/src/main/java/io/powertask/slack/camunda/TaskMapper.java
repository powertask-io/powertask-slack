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

import io.powertask.slack.usertasks.Task;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.DelegateTask;

public class TaskMapper {

  private final PropertiesResolver propertiesResolver;

  static final String PROPERTY_SLACK_TITLE = "slack-title";
  static final String PROPERTY_SLACK_DESCRIPTION = "slack-description";
  protected static final String PROPERTY_SLACK_SHOW_VARIABLES = "slack-show-variables";

  protected static final String VARIABLE_TASK_ERROR_MESSAGE = "task-error-message";
  private final RuntimeService runtimeService;

  public TaskMapper(PropertiesResolver propertiesResolver, RuntimeService runtimeService) {
    this.propertiesResolver = propertiesResolver;
    this.runtimeService = runtimeService;
  }

  Task fromTask(org.camunda.bpm.engine.task.Task task) {
    return new Task() {

      @Override
      public String assignee() {
        return task.getAssignee();
      }

      @Override
      public String id() {
        return task.getId();
      }

      @Override
      public String name() {
        return task.getName();
      }

      @Override
      public String title() {
        return propertiesResolver
            .getProperty(
                task.getProcessDefinitionId(), task.getTaskDefinitionKey(), PROPERTY_SLACK_TITLE)
            .orElse(task.getName());
      }

      @Override
      public Optional<String> description() {
        return propertiesResolver.getProperty(
            task.getProcessDefinitionId(), task.getTaskDefinitionKey(), PROPERTY_SLACK_DESCRIPTION);
      }

      @Override
      public String processDefinitionId() {
        return task.getProcessDefinitionId();
      }

      @Override
      public String processInstanceId() {
        return task.getProcessInstanceId();
      }

      @Override
      public Optional<String> errorMessage() {
        return Optional.ofNullable(
            (String)
                runtimeService.getVariable(task.getExecutionId(), VARIABLE_TASK_ERROR_MESSAGE));
      }

      @Override
      public Optional<Optional<List<String>>> showVariables() {
        return propertiesResolver
            .getPropertyWithOptionalValue(
                task.getProcessDefinitionId(),
                task.getTaskDefinitionKey(),
                PROPERTY_SLACK_SHOW_VARIABLES)
            .map(
                v ->
                    v.map(
                        s ->
                            Arrays.stream(s.split(","))
                                .map(String::trim)
                                .collect(Collectors.toList())));
      }
    };
  }

  public Task fromDelegateTask(DelegateTask delegateTask) {
    return new Task() {
      @Override
      public String assignee() {
        return delegateTask.getAssignee();
      }

      @Override
      public String id() {
        return delegateTask.getId();
      }

      @Override
      public String name() {
        return delegateTask.getName();
      }

      @Override
      public String title() {
        return propertiesResolver
            .getProperty(
                delegateTask.getProcessDefinitionId(),
                delegateTask.getTaskDefinitionKey(),
                PROPERTY_SLACK_TITLE)
            .orElse(delegateTask.getName());
      }

      @Override
      public Optional<String> description() {
        return propertiesResolver.getProperty(
            delegateTask.getProcessDefinitionId(),
            delegateTask.getTaskDefinitionKey(),
            PROPERTY_SLACK_DESCRIPTION);
      }

      @Override
      public String processDefinitionId() {
        return delegateTask.getProcessDefinitionId();
      }

      @Override
      public String processInstanceId() {
        return delegateTask.getProcessInstanceId();
      }

      @Override
      public Optional<String> errorMessage() {
        return Optional.ofNullable((String) delegateTask.getVariable(VARIABLE_TASK_ERROR_MESSAGE));
      }

      @Override
      public Optional<Optional<List<String>>> showVariables() {
        return propertiesResolver
            .getPropertyWithOptionalValue(
                delegateTask.getProcessDefinitionId(),
                delegateTask.getTaskDefinitionKey(),
                PROPERTY_SLACK_SHOW_VARIABLES)
            .map(
                v ->
                    v.map(
                        s ->
                            Arrays.stream(s.split(","))
                                .map(String::trim)
                                .collect(Collectors.toList())));
      }
    };
  }
}
