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
package io.powertask.slack.usertasks;

import io.powertask.slack.FormLike;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.task.Task;

/**
 * Read-only wrapper for a Task.
 *
 * <p>Can be used to wrap a DelegateTask as well as an engine Task.
 */
public interface TaskDetails extends FormLike {
  String getAssignee();

  String getId();

  String getName();

  String getProcessDefinitionId();

  String getProcessInstanceId();

  static TaskDetails of(Task task) {
    return new TaskDetails() {

      @Override
      public String getAssignee() {
        return task.getAssignee();
      }

      @Override
      public String getId() {
        return task.getId();
      }

      @Override
      public String getName() {
        return task.getName();
      }

      @Override
      public String getProcessDefinitionId() {
        return task.getProcessDefinitionId();
      }

      @Override
      public String getProcessDefinitionElementId() {
        return task.getTaskDefinitionKey();
      }

      @Override
      public String getProcessInstanceId() {
        return task.getProcessInstanceId();
      }
    };
  }

  static TaskDetails of(DelegateTask delegateTask) {
    return new TaskDetails() {
      @Override
      public String getAssignee() {
        return delegateTask.getAssignee();
      }

      @Override
      public String getId() {
        return delegateTask.getId();
      }

      @Override
      public String getName() {
        return delegateTask.getName();
      }

      @Override
      public String getProcessDefinitionId() {
        return delegateTask.getProcessDefinitionId();
      }

      @Override
      public String getProcessDefinitionElementId() {
        return delegateTask.getTaskDefinitionKey();
      }

      @Override
      public String getProcessInstanceId() {
        return delegateTask.getProcessInstanceId();
      }
    };
  }
}
