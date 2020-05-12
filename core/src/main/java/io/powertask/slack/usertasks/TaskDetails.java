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

import java.util.Date;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.task.Task;

/**
 * Read-only wrapper for a Task.
 *
 * <p>Can be used to wrap a DelegateTask as well as an engine Task.
 */
public interface TaskDetails {
  String getAssignee();

  String getCaseDefinitionId();

  String getCaseExecutionId();

  String getCaseInstanceId();

  Date getCreateTime();

  String getDescription();

  Date getDueDate();

  String getExecutionId();

  Date getFollowUpDate();

  String getId();

  String getName();

  String getOwner();

  int getPriority();

  String getProcessDefinitionId();

  String getProcessInstanceId();

  String getTaskDefinitionKey();

  String getTenantId();

  static TaskDetails of(Task task) {
    return new TaskDetails() {

      @Override
      public String getAssignee() {
        return task.getAssignee();
      }

      @Override
      public String getCaseDefinitionId() {
        return task.getCaseDefinitionId();
      }

      @Override
      public String getCaseExecutionId() {
        return task.getCaseExecutionId();
      }

      @Override
      public String getCaseInstanceId() {
        return task.getCaseInstanceId();
      }

      @Override
      public Date getCreateTime() {
        return task.getCreateTime();
      }

      @Override
      public String getDescription() {
        return task.getDescription();
      }

      @Override
      public Date getDueDate() {
        return task.getDueDate();
      }

      @Override
      public String getExecutionId() {
        return task.getExecutionId();
      }

      @Override
      public Date getFollowUpDate() {
        return task.getFollowUpDate();
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
      public String getOwner() {
        return task.getOwner();
      }

      @Override
      public int getPriority() {
        return task.getPriority();
      }

      @Override
      public String getProcessDefinitionId() {
        return task.getProcessDefinitionId();
      }

      @Override
      public String getProcessInstanceId() {
        return task.getProcessInstanceId();
      }

      @Override
      public String getTaskDefinitionKey() {
        return task.getTaskDefinitionKey();
      }

      @Override
      public String getTenantId() {
        return task.getTenantId();
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
      public String getCaseDefinitionId() {
        return delegateTask.getCaseDefinitionId();
      }

      @Override
      public String getCaseExecutionId() {
        return delegateTask.getCaseExecutionId();
      }

      @Override
      public String getCaseInstanceId() {
        return delegateTask.getCaseInstanceId();
      }

      @Override
      public Date getCreateTime() {
        return delegateTask.getCreateTime();
      }

      @Override
      public String getDescription() {
        return delegateTask.getDescription();
      }

      @Override
      public Date getDueDate() {
        return delegateTask.getDueDate();
      }

      @Override
      public String getExecutionId() {
        return delegateTask.getExecutionId();
      }

      @Override
      public Date getFollowUpDate() {
        return delegateTask.getFollowUpDate();
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
      public String getOwner() {
        return delegateTask.getOwner();
      }

      @Override
      public int getPriority() {
        return delegateTask.getPriority();
      }

      @Override
      public String getProcessDefinitionId() {
        return delegateTask.getProcessDefinitionId();
      }

      @Override
      public String getProcessInstanceId() {
        return delegateTask.getProcessInstanceId();
      }

      @Override
      public String getTaskDefinitionKey() {
        return delegateTask.getTaskDefinitionKey();
      }

      @Override
      public String getTenantId() {
        return delegateTask.getTenantId();
      }
    };
  }
}
