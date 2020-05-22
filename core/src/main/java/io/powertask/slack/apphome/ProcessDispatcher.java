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
import static io.powertask.slack.SlackApiOps.requireOk;

import com.slack.api.bolt.App;
import com.slack.api.bolt.context.Context;
import com.slack.api.bolt.context.builtin.ActionContext;
import com.slack.api.bolt.context.builtin.ViewSubmissionContext;
import com.slack.api.bolt.request.builtin.BlockActionRequest;
import com.slack.api.bolt.request.builtin.ViewSubmissionRequest;
import com.slack.api.bolt.response.Response;
import com.slack.api.model.block.LayoutBlock;
import io.powertask.slack.Form;
import io.powertask.slack.FormService;
import io.powertask.slack.Process;
import io.powertask.slack.ProcessService;
import io.powertask.slack.StartEvent;
import io.powertask.slack.TaskService;
import io.powertask.slack.identity.UserResolver;
import io.powertask.slack.modals.renderers.ModalRenderer;
import io.powertask.slack.usertasks.UserTaskDispatcher;
import io.vavr.control.Either;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessDispatcher {

  private static final Logger logger = LoggerFactory.getLogger(ProcessDispatcher.class);

  private static final Pattern processOpenPattern = Pattern.compile("^process-start/(.*)$");
  private static final Pattern processSubmitPattern =
      Pattern.compile("^modal-process-submit/(.*)$");

  private final App app;
  private final UserResolver userResolver;
  private final ModalRenderer modalRenderer;

  private final ProcessService processService;
  private final FormService formService;
  private final UserTaskDispatcher userTaskDispatcher;

  public ProcessDispatcher(
      App app,
      ProcessService processService,
      TaskService taskService,
      FormService formService,
      UserTaskDispatcher userTaskDispatcher,
      UserResolver userResolver) {
    this.app = app;
    this.processService = processService;
    this.formService = formService;
    this.userTaskDispatcher = userTaskDispatcher;
    this.userResolver = userResolver;
    this.modalRenderer = new ModalRenderer(taskService);
    registerActionHandlers();
  }

  private void registerActionHandlers() {
    app.blockAction(processOpenPattern, this::startProcessAction);
    app.viewSubmission(processSubmitPattern, this::submitProcessModal);
  }

  private Response submitProcessModal(ViewSubmissionRequest req, ViewSubmissionContext ctx) {
    String processDefinitionId =
        extractProcessDefinitionIdFromCallbackId(req.getPayload().getView().getCallbackId());

    Form form =
        formService
            .startForm(processDefinitionId)
            .orElseThrow(
                () ->
                    new RuntimeException(
                        "No form found for process definition " + processDefinitionId));

    Either<Map<String, String>, Map<String, Object>> errorsOrVariables =
        modalRenderer.extractVariables(form, req.getPayload().getView().getState());

    return errorsOrVariables.fold(
        ctx::ackWithErrors,
        formVariables -> {
          StartEvent startEventDetails = processService.startEvent(processDefinitionId);
          String engineUserId = userResolver.toEngineUserId(req.getPayload().getUser().getId());
          return startProcess(ctx, startEventDetails, formVariables, engineUserId);
        });
  }

  private Response startProcess(
      Context ctx,
      StartEvent startEventDetails,
      Map<String, Object> formVariables,
      String engineUserId) {

    Map<String, Object> initiatorVariables =
        startEventDetails
            .initiatorVariableName()
            .map(name -> Collections.<String, Object>singletonMap(name, engineUserId))
            .orElse(Collections.emptyMap());

    String processInstanceId;

    if (formVariables.isEmpty()) {
      logger.info(
          "Starting process "
              + startEventDetails.processDefinitionId()
              + " with variables "
              + initiatorVariables);
      processInstanceId =
          processService.startProcess(startEventDetails.processDefinitionId(), initiatorVariables);

    } else {
      Map<String, Object> allVariables = new HashMap<>(formVariables);
      allVariables.putAll(initiatorVariables);
      logger.info(
          "Starting process "
              + startEventDetails.processDefinitionId()
              + " with variables "
              + allVariables);
      processInstanceId =
          processService.startProcessWithForm(
              startEventDetails.processDefinitionId(), allVariables);
    }
    return userTaskDispatcher.showFollowupTask(ctx, processInstanceId, engineUserId);
  }

  private Response startProcessAction(BlockActionRequest req, ActionContext ctx) {
    String processDefinitionId =
        extractProcessDefinitionId(req.getPayload().getActions().get(0).getActionId());

    StartEvent startEvent = processService.startEvent(processDefinitionId);

    // TODO, we should probably do the following:
    // - If there's a form on the process; show it in a modal
    // - If there's no form, start the process here.
    //     - If there's a follow-up task, show it.
    //     - If there's no follow-up task, show some feedback in some way. Maybe a an icon to the
    // button?
    return formService
        .startForm(processDefinitionId)
        .map(
            form -> {
              openModal(ctx, startEvent, form);
              return ctx.ack();
            })
        .orElseGet(
            () -> {
              String engineUserId = userResolver.toEngineUserId(req.getPayload().getUser().getId());
              return startProcess(ctx, startEvent, Collections.emptyMap(), engineUserId);
            });
  }

  private String generateSubmitModalActionId(String processDefinitionId) {
    return "modal-process-submit/" + processDefinitionId;
  }

  public List<LayoutBlock> processList(String slackUserId) {
    String engineUserId = userResolver.toEngineUserId(slackUserId);
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

  public void openModal(ActionContext ctx, StartEvent startEvent, Form form) {
    requireOk(
        () ->
            ctx.client()
                .viewsOpen(
                    request ->
                        request
                            .triggerId(ctx.getTriggerId())
                            .view(
                                modalRenderer.buildModal(
                                    startEvent,
                                    form,
                                    generateSubmitModalActionId(
                                        startEvent.processDefinitionId())))));
  }

  private String extractProcessDefinitionId(String actionId) {
    Matcher matcher = processOpenPattern.matcher(actionId);
    if (matcher.find()) {
      return matcher.group(1);
    } else {
      throw new IllegalArgumentException("Invalid process id.");
    }
  }

  private String extractProcessDefinitionIdFromCallbackId(String callbackId) {
    Matcher matcher = processSubmitPattern.matcher(callbackId);
    if (matcher.find()) {
      return matcher.group(1);
    } else {
      throw new IllegalArgumentException("Invalid process id.");
    }
  }
}
