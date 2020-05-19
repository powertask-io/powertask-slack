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
import static org.junit.Assert.assertEquals;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.test.assertions.ProcessEngineTests;
import org.junit.jupiter.api.Test;

public class MovieReviewIT extends AbstractIntegrationTest {

  protected MovieReviewIT() {
    super("movie-review");
  }

  @Test
  public void testMovieReviewProcess() throws InterruptedException {

    deploy("movie-review.bpmn");

    Map<String, Object> variables = Collections.singletonMap("email", "some-user@example.com");
    ProcessInstance processInstance =
        processEngine
            .getRuntimeService()
            .startProcessInstanceByKey("movie-review-process", variables);

    ProcessEngineTests.assertThat(processInstance).isWaitingAt("basic-review");

    Task task =
        processEngine
            .getTaskService()
            .createTaskQuery()
            .processInstanceId(processInstance.getId())
            .singleResult();

    Map<String, String> substitutions = new HashMap<>();
    substitutions.put("$.actions[0].action_id", "modal-task-open/" + task.getId());

    // Wait for the async work after notification of a new task is completed.
    Thread.sleep(100);

    slackInteraction("block-action.http", substitutions);
    Thread.sleep(100);

    // Should still be waiting.
    ProcessEngineTests.assertThat(processInstance).isWaitingAt("basic-review");

    // Let the user fill out the modal.
    substitutions.clear();
    substitutions.put("$.view.callback_id", "modal-task-submit/" + task.getId());
    slackInteraction("view-submission-1.http", substitutions);
    Thread.sleep(100);

    // Should now be waiting in the next task.
    ProcessEngineTests.assertThat(processInstance).isWaitingAt("elaborate-review");

    Task task2 =
        processEngine
            .getTaskService()
            .createTaskQuery()
            .processInstanceId(processInstance.getId())
            .singleResult();

    // Let the user fill out the second modal.
    substitutions.clear();
    substitutions.put("$.view.callback_id", "modal-task-submit/" + task2.getId());
    slackInteraction("view-submission-2.http", substitutions);
    Thread.sleep(100);

    ProcessEngineTests.assertThat(processInstance).isEnded();
    ProcessEngineTests.assertThat(processInstance)
        .variables()
        .contains(entry("movie", "The Men Who Stare at Goats"));
    ProcessEngineTests.assertThat(processInstance).variables().contains(entry("rating", 8L));
    ProcessEngineTests.assertThat(processInstance)
        .variables()
        .contains(entry("when", Date.valueOf(LocalDate.of(2020, 4, 8))));
    ProcessEngineTests.assertThat(processInstance)
        .variables()
        .contains(entry("review", "Awesome movie. Wonderful scenery. Great goats."));

    // TODO, we should figure out how to cut down on the 'authtest' and 'userslookupbyemail' calls.
    assertEquals(12, wireMockServer.getAllServeEvents().size());
  }
}
