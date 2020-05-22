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
package io.powertask.slack.camunda.plugin;

import io.powertask.slack.camunda.TaskMapper;
import io.powertask.slack.usertasks.UserTaskDispatcher;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Wrapper for UserTaskDispatcher to make it a TaskListener.
public class UserTaskDispatcherListener implements TaskListener {

  private static final Logger logger = LoggerFactory.getLogger(UserTaskDispatcherListener.class);

  private final UserTaskDispatcher userTaskDispatcher;
  private final TaskMapper taskMapper;

  public UserTaskDispatcherListener(TaskMapper taskMapper, UserTaskDispatcher userTaskDispatcher) {
    this.taskMapper = taskMapper;
    this.userTaskDispatcher = userTaskDispatcher;
  }

  @Override
  public void notify(DelegateTask delegateTask) {
    if (delegateTask.getEventName().equals(TaskListener.EVENTNAME_ASSIGNMENT)) {
      logger.debug("Task {} assigned to {}", delegateTask.getId(), delegateTask.getAssignee());
      userTaskDispatcher.notifyTaskAssignment(taskMapper.fromDelegateTask(delegateTask));
    } else if (delegateTask.getEventName().equals(TaskListener.EVENTNAME_COMPLETE)) {
      logger.debug("Task {} assigned to {}", delegateTask.getId(), delegateTask.getAssignee());
      userTaskDispatcher.notifyTaskCompletion(taskMapper.fromDelegateTask(delegateTask));
    }
  }
}
