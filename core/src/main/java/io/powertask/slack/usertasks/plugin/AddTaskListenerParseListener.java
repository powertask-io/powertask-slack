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
package io.powertask.slack.usertasks.plugin;

import static org.camunda.bpm.engine.delegate.TaskListener.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.camunda.bpm.engine.impl.bpmn.behavior.UserTaskActivityBehavior;
import org.camunda.bpm.engine.impl.bpmn.parser.AbstractBpmnParseListener;
import org.camunda.bpm.engine.impl.el.ExpressionManager;
import org.camunda.bpm.engine.impl.pvm.delegate.ActivityBehavior;
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl;
import org.camunda.bpm.engine.impl.pvm.process.ScopeImpl;
import org.camunda.bpm.engine.impl.task.TaskDefinition;
import org.camunda.bpm.engine.impl.task.listener.DelegateExpressionTaskListener;
import org.camunda.bpm.engine.impl.util.xml.Element;

public class AddTaskListenerParseListener extends AbstractBpmnParseListener {

  private final ExpressionManager expressionManager;

  private final List<String> ALL_EVENT_NAMES =
      Arrays.asList(
          EVENTNAME_CREATE,
          EVENTNAME_ASSIGNMENT,
          EVENTNAME_COMPLETE,
          EVENTNAME_UPDATE,
          EVENTNAME_DELETE,
          EVENTNAME_TIMEOUT);

  public AddTaskListenerParseListener(ExpressionManager expressionManager) {
    this.expressionManager = expressionManager;
  }

  @Override
  public void parseUserTask(Element userTaskElement, ScopeImpl scope, ActivityImpl activity) {
    ActivityBehavior activityBehavior = activity.getActivityBehavior();
    if (activityBehavior instanceof UserTaskActivityBehavior) {
      UserTaskActivityBehavior userTaskActivityBehavior =
          (UserTaskActivityBehavior) activityBehavior;
      TaskDefinition taskDefinition = userTaskActivityBehavior.getTaskDefinition();

      for (String eventName : ALL_EVENT_NAMES) {
        taskDefinition.addTaskListener(
            eventName,
            new DelegateExpressionTaskListener(
                expressionManager.createExpression("${userTaskDispatcher}"),
                Collections.emptyList()));
      }
    }
  }
}
