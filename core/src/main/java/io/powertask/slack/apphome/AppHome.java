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
package io.powertask.slack.apphome;

import static com.slack.api.model.block.Blocks.actions;
import static com.slack.api.model.block.Blocks.divider;
import static com.slack.api.model.block.Blocks.section;
import static com.slack.api.model.block.composition.BlockCompositions.markdownText;
import static com.slack.api.model.block.composition.BlockCompositions.plainText;
import static com.slack.api.model.block.element.BlockElements.asElements;
import static com.slack.api.model.block.element.BlockElements.button;
import static com.slack.api.model.view.Views.view;
import static io.powertask.slack.SlackApiOps.requireOk;

import com.slack.api.bolt.App;
import com.slack.api.methods.MethodsClient;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.event.AppHomeOpenedEvent;
import com.slack.api.model.view.View;
import io.powertask.slack.Process;
import io.powertask.slack.ProcessService;
import io.powertask.slack.TaskService;
import io.powertask.slack.identity.UserResolver;
import io.powertask.slack.usertasks.Task;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AppHome {

  private final App app;
  private final ProcessService processService;
  private final TaskService taskService;
  private final MethodsClient methodsClient;
  private final UserResolver userResolver;

  public AppHome(
      App app,
      ProcessService processService,
      TaskService taskService,
      UserResolver userResolver,
      MethodsClient methodsClient) {
    this.app = app;
    this.processService = processService;
    this.taskService = taskService;
    this.userResolver = userResolver;
    this.methodsClient = methodsClient;
    subscribeAppHomeOpenedEvent();
  }

  private void subscribeAppHomeOpenedEvent() {
    app.event(
        AppHomeOpenedEvent.class,
        (payload, ctx) -> {
          updateUserHome(payload.getEvent().getUser());
          return ctx.ack();
        });
  }

  private void updateUserHome(String slackUserId) {
    requireOk(
        () ->
            methodsClient.viewsPublish(r -> r.userId(slackUserId).view(appHomeView(slackUserId))));
  }

  private View appHomeView(String slackUserId) {
    String engineUserId = userResolver.toEngineUserId(slackUserId);
    return view(
        view ->
            view.type("home")
                .blocks(introBlocks())
                .blocks(taskList(engineUserId))
                .blocks(processList(engineUserId)));
  }

  public List<LayoutBlock> introBlocks() {
    return Collections.singletonList(
        section(section -> section.text(markdownText("Jow, this is the intro block!"))));
  }

  public List<LayoutBlock> taskList(String engineUserId) {
    List<Task> tasks = taskService.tasksByAssignee(engineUserId);

    List<LayoutBlock> blocks = new ArrayList<>();
    blocks.add(section(s -> s.text(markdownText("*Your tasks*"))));
    blocks.add(divider());
    tasks.forEach(
        p -> {
          blocks.addAll(taskBlocks(p));
          blocks.add(divider());
        });
    return blocks;
  }

  public List<LayoutBlock> processList(String engineUserId) {
    List<Process> processes = processService.startableProcesses(engineUserId);

    List<LayoutBlock> blocks = new ArrayList<>();
    blocks.add(section(s -> s.text(markdownText("*Processes you can start*"))));
    blocks.add(divider());
    processes.forEach(
        p -> {
          blocks.addAll(processStartBlocks(p));
          blocks.add(divider());
        });
    return blocks;
  }

  private List<LayoutBlock> processStartBlocks(Process p) {
    return Arrays.asList(
        section(section -> section.text(markdownText("*" + p.name() + "*"))),
        actions(
            actions ->
                actions
                    .blockId("process/" + p.id())
                    .elements(
                        asElements(
                            button(
                                button ->
                                    button
                                        .actionId("process-start/" + p.id())
                                        .text(plainText("Start")))))));
  }

  // TODO, what can we reuse here from the modal task renderer?
  private List<LayoutBlock> taskBlocks(Task t) {
    return Arrays.asList(
        section(section -> section.text(markdownText("*" + t.name() + "*"))),
        actions(
            actions ->
                actions
                    .blockId("task/" + t.id())
                    .elements(
                        asElements(
                            button(
                                button ->
                                    button
                                        .actionId("modal-task-open/" + t.id())
                                        .text(plainText("Go")))))));
  }
}
