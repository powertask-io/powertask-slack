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

import io.powertask.slack.usertasks.Task;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface TaskService {
  Task taskById(String taskId);

  Optional<Task> followUpTask(String processInstanceId, String assignee);

  Map<String, Object> getVariables(String taskId);

  Map<String, Object> getVariables(String taskId, Set<String> names);

  Optional<Object> getVariable(String taskId, String name);

  void setVariables(String taskId, Map<String, Object> variables);

  void setVariable(String taskId, String name, Object value);
}
