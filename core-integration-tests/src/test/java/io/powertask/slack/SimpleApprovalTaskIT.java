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

import static org.assertj.core.api.Assertions.entry;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.assertj.core.api.IntegerAssert;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.test.assertions.ProcessEngineTests;
import org.junit.jupiter.api.Test;

public class SimpleApprovalTaskIT extends AbstractIntegrationTest {

  public SimpleApprovalTaskIT() {
    super("simple-approval-task");
  }

  @Test
  public void processWithSimpleApprovalTask() throws InterruptedException {
    deploy("simple-approval-task.bpmn");

    Map<String, Object> variables = Collections.singletonMap("email", "some-user@example.com");
    ProcessInstance processInstance =
        processEngine
            .getRuntimeService()
            .startProcessInstanceByKey("simple-approval-task", variables);

    ProcessEngineTests.assertThat(processInstance).isWaitingAt("usertask_approve");

    Task task =
        processEngine
            .getTaskService()
            .createTaskQuery()
            .processInstanceId(processInstance.getId())
            .singleResult();

    Map<String, String> substitutions = new HashMap<>();
    substitutions.put("$.actions[0].action_id", "single-message-task/" + task.getId() + "/1");

    // There's some async work happening after rendering that we want to be completed.
    Thread.sleep(500);

    slackInteraction("block-action.http", substitutions);

    // Wait for async processing of the incoming request.
    Thread.sleep(500);

    ProcessEngineTests.assertThat(processInstance).isEnded();
    ProcessEngineTests.assertThat(processInstance).variables().contains(entry("approve", true));

    // TODO, we should figure out how to cut down on the 'authtest' and 'userslookupbyemail' calls.
    new IntegerAssert(wireMockServer.getAllServeEvents().size()).isBetween(5, 6);
  }
}
