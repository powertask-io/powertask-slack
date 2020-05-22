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

import io.powertask.slack.TaskService;
import io.powertask.slack.usertasks.Task;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class CamundaTaskService implements TaskService {

  private final org.camunda.bpm.engine.TaskService taskService;
  private final TaskMapper taskMapper;

  public CamundaTaskService(org.camunda.bpm.engine.TaskService taskService, TaskMapper taskMapper) {
    this.taskService = taskService;
    this.taskMapper = taskMapper;
  }

  @Override
  public Task taskById(String taskId) {
    return taskMapper.fromTask(taskService.createTaskQuery().taskId(taskId).singleResult());
  }

  @Override
  public Optional<Task> followUpTask(String processInstanceId, String assignee) {
    return taskService.createTaskQuery().taskAssignee(assignee).processInstanceId(processInstanceId)
        .list().stream()
        .map(taskMapper::fromTask)
        .findFirst();
  }

  @Override
  public Map<String, Object> getVariables(String taskId) {
    return taskService.getVariables(taskId);
  }

  @Override
  public Map<String, Object> getVariables(String taskId, Set<String> names) {
    return taskService.getVariables(taskId, names);
  }

  @Override
  public Optional<Object> getVariable(String taskId, String name) {
    return Optional.ofNullable(taskService.getVariable(taskId, name));
  }

  @Override
  public void setVariables(String taskId, Map<String, Object> variables) {
    taskService.setVariables(taskId, variables);
  }

  @Override
  public void setVariable(String taskId, String name, Object value) {
    taskService.setVariable(taskId, name, value);
  }
}
