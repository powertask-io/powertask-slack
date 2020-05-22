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

import java.util.ArrayList;
import java.util.List;
import org.camunda.bpm.engine.impl.bpmn.parser.BpmnParseListener;
import org.camunda.bpm.engine.impl.cfg.AbstractProcessEnginePlugin;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.el.ExpressionManager;

public class TaskListenerPlugin extends AbstractProcessEnginePlugin {

  @Override
  //
  // Problem:
  // - At preInit the ExpressionManager isn't initialized yet.
  // - At postInit, the BpmnParser is already created, so we can't add parseListeners anymore.
  // 'Solution':
  // - Create a new Expression Manager if it's not set, and set it on the config.
  //
  public void preInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
    setupExpressionManager(processEngineConfiguration);
    addPreParseListeners(processEngineConfiguration);
  }

  private void setupExpressionManager(ProcessEngineConfigurationImpl processEngineConfiguration) {
    if (processEngineConfiguration.getExpressionManager() == null) {
      processEngineConfiguration.setExpressionManager(
          new ExpressionManager(processEngineConfiguration.getBeans()));
    }
  }

  private void addPreParseListeners(ProcessEngineConfigurationImpl processEngineConfiguration) {
    List<BpmnParseListener> preParseListeners =
        processEngineConfiguration.getCustomPreBPMNParseListeners();
    if (preParseListeners == null) {
      preParseListeners = new ArrayList<>();
      processEngineConfiguration.setCustomPreBPMNParseListeners(preParseListeners);
    }
    preParseListeners.add(
        new AddTaskListenerParseListener(processEngineConfiguration.getExpressionManager()));
  }
}
