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
import com.slack.api.bolt.context.builtin.ActionContext;
import com.slack.api.bolt.context.builtin.ViewSubmissionContext;
import com.slack.api.bolt.request.builtin.BlockActionRequest;
import com.slack.api.bolt.request.builtin.ViewSubmissionRequest;
import com.slack.api.bolt.response.Response;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.view.ViewState;
import io.powertask.slack.CamundaPropertiesResolver;
import io.powertask.slack.apphome.AppHome.Process;
import io.powertask.slack.identity.UserResolver;
import io.powertask.slack.modals.renderers.ModalRenderer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.form.FormData;
import org.camunda.bpm.engine.form.StartFormData;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessDispatcher {

  private static final Logger logger = LoggerFactory.getLogger(ProcessDispatcher.class);

  private static final Pattern processOpenPattern = Pattern.compile("^process-start/(.*)$");
  private static final Pattern processSubmitPattern =
      Pattern.compile("^modal-process-submit/(.*)$");

  private final App app;
  private final ProcessEngine processEngine;
  private final UserResolver userResolver;
  private final ModalRenderer<StartEventDetails> modalRenderer;

  public ProcessDispatcher(App app, ProcessEngine processEngine, UserResolver userResolver) {
    this.app = app;
    this.processEngine = processEngine;
    this.userResolver = userResolver;
    this.modalRenderer =
        new ModalRenderer<>(
            new CamundaPropertiesResolver<>(processEngine.getRepositoryService()),
            new StartEventVariablesResolver());
    registerActionHandlers();
  }

  public List<Process> listStartableProcesses(String slackUserId) {
    String engineUserId = userResolver.toEngineUserId(slackUserId);

    List<ProcessDefinition> processDefinitions =
        processEngine
            .getRepositoryService()
            .createProcessDefinitionQuery()
            .startableByUser(engineUserId)
            .startableInTasklist()
            .active()
            .list();

    return processDefinitions.stream()
        .map(
            processDefinition ->
                ImmutableProcess.builder()
                    .id(processDefinition.getId())
                    .name(
                        Optional.ofNullable(processDefinition.getName())
                            .orElseGet(
                                () -> "Nameless process with id " + processDefinition.getId()))
                    // TODO, this corresponds to the 'documentation' field in the Modeler. I think
                    // we
                    // should use the extension similar to tasks.
                    .description(Optional.ofNullable(processDefinition.getDescription()))
                    .build())
        .collect(Collectors.toList());
  }

  private void registerActionHandlers() {
    app.blockAction(processOpenPattern, this::startProcessAction);
    app.viewSubmission(processSubmitPattern, this::submitProcessModal);
  }

  // TODO, lot of duplication with ModalTaskRenderer.submitHandler
  private Response submitProcessModal(ViewSubmissionRequest req, ViewSubmissionContext ctx) {

    Map<String, Map<String, ViewState.Value>> viewStateValues =
        req.getPayload().getView().getState().getValues();

    String processDefinitionId =
        extractProcessDefinitionIdFromCallbackId(req.getPayload().getView().getCallbackId());
    FormData formData = processEngine.getFormService().getStartFormData(processDefinitionId);

    Map<String, Object> formVariables = new HashMap<>();
    Map<String, String> errors = new HashMap<>();
    formData
        .getFormFields()
        .forEach(
            field ->
                modalRenderer
                    .getRenderer(field)
                    .extractValue(field, viewStateValues.get(field.getId()))
                    .peek(
                        optionalValue ->
                            optionalValue.ifPresent(
                                value -> formVariables.put(field.getId(), value)))
                    .peekLeft(error -> errors.put(field.getId(), error)));

    if (!errors.isEmpty()) {
      return ctx.ackWithErrors(errors);
    } else {
      StartEventDetails startEventDetails = getStartEventDetails(processDefinitionId);
      startProcess(startEventDetails, formVariables, req.getPayload().getUser().getId());
      return ctx.ack();
    }
  }

  private void startProcess(
      StartEventDetails startEventDetails, Map<String, Object> formVariables, String slackUserId) {

    Map<String, Object> initiatorVariables =
        startEventDetails
            .initiatorVariableName()
            .map(
                name ->
                    Collections.<String, Object>singletonMap(
                        name, userResolver.toEngineUserId(slackUserId)))
            .orElse(Collections.emptyMap());

    if (formVariables.isEmpty()) {
      logger.info(
          "Starting process "
              + startEventDetails.getProcessDefinitionId()
              + " with variables "
              + initiatorVariables);
      // Probably there's no start form, so we use `startProcessInstanceById`.
      processEngine
          .getRuntimeService()
          .startProcessInstanceById(startEventDetails.getProcessDefinitionId(), initiatorVariables);
    } else {
      Map<String, Object> allVariables = new HashMap<>(formVariables);
      allVariables.putAll(initiatorVariables);
      logger.info(
          "Starting process "
              + startEventDetails.getProcessDefinitionId()
              + " with variables "
              + allVariables);

      processEngine
          .getFormService()
          .submitStartForm(startEventDetails.getProcessDefinitionId(), allVariables);
    }
  }

  private StartEventDetails getStartEventDetails(String processDefinitionId) {
    BpmnModelInstance def =
        processEngine.getRepositoryService().getBpmnModelInstance(processDefinitionId);
    // TODO, find the right one in case of multiple start events.
    // There should only be one 'none' start event
    // (https://camunda.com/best-practices/routing-events-to-processes/#__strong_start_strong_events)
    Collection<StartEvent> startEvents = def.getModelElementsByType(StartEvent.class);
    StartEvent startEvent = startEvents.iterator().next();

    return StartEventDetails.of(startEvent, processDefinitionId);
  }

  // TODO, rename because this doesn't always open a modal.
  private Response startProcessAction(BlockActionRequest req, ActionContext ctx) {
    String processDefinitionId =
        extractProcessDefinitionId(req.getPayload().getActions().get(0).getActionId());

    StartEventDetails startEventDetails = getStartEventDetails(processDefinitionId);

    // TODO, we should probably do the following:
    // - If there's a form on the process; show it in a modal
    // - If there's no form, start the process here.
    //     - If there's a follow-up task, show it.
    //     - If there's no follow-up task, show some feedback in some way. Maybe a an icon to the
    // button?
    StartFormData formData = processEngine.getFormService().getStartFormData(processDefinitionId);

    if (formData.getFormFields().isEmpty()) {
      startProcess(startEventDetails, Collections.emptyMap(), req.getPayload().getUser().getId());
      return ctx.ack(); // TODO, what can we do to show some interaction?
    } else {
      openModal(ctx, startEventDetails, formData);
      return ctx.ack();
    }
  }

  private String generateSubmitModalActionId(String processDefinitionId) {
    return "modal-process-submit/" + processDefinitionId;
  }

  public List<LayoutBlock> processList(String slackUserId) {
    List<Process> processes = listStartableProcesses(slackUserId);

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

  public void openModal(ActionContext ctx, StartEventDetails startEventDetails, FormData formData) {
    requireOk(
        () ->
            ctx.client()
                .viewsOpen(
                    request ->
                        request
                            .triggerId(ctx.getTriggerId())
                            .view(
                                modalRenderer.buildModal(
                                    startEventDetails,
                                    formData,
                                    generateSubmitModalActionId(
                                        startEventDetails.getProcessDefinitionId())))));
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
