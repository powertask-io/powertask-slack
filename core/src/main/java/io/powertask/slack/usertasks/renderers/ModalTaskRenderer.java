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
package io.powertask.slack.usertasks.renderers;

import static com.slack.api.model.block.Blocks.section;
import static com.slack.api.model.block.composition.BlockCompositions.markdownText;
import static com.slack.api.model.block.composition.BlockCompositions.plainText;
import static io.powertask.slack.SlackApiOps.requireOk;

import com.slack.api.RequestConfigurator;
import com.slack.api.bolt.context.Context;
import com.slack.api.bolt.context.builtin.ActionContext;
import com.slack.api.bolt.context.builtin.ViewSubmissionContext;
import com.slack.api.bolt.request.builtin.BlockActionRequest;
import com.slack.api.bolt.request.builtin.ViewSubmissionRequest;
import com.slack.api.bolt.response.Response;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.model.block.ActionsBlock;
import com.slack.api.model.block.element.ButtonElement;
import com.slack.api.model.view.ViewState;
import io.powertask.slack.CamundaPropertiesResolver;
import io.powertask.slack.FormLikePropertiesBase;
import io.powertask.slack.modals.renderers.ModalRenderer;
import io.powertask.slack.usertasks.TaskDetails;
import io.powertask.slack.usertasks.TaskVariablesResolver;
import java.util.*;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.form.FormData;
import org.camunda.bpm.engine.form.TaskFormData;
import org.camunda.bpm.engine.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModalTaskRenderer extends FormLikePropertiesBase<TaskDetails> implements TaskRenderer {

  private static final Logger logger = LoggerFactory.getLogger(ModalTaskRenderer.class);

  private static final Pattern taskOpenPattern =
      Pattern.compile("^modal-task-open/([a-z0-9\\-]+)$");
  private static final Pattern taskSubmitPattern =
      Pattern.compile("^modal-task-submit/([a-z0-9\\-]+)$");
  protected final BiFunction<TaskResult, Context, Response> submissionListener;
  private final ProcessEngine processEngine;

  private final ModalRenderer modalRenderer;

  public ModalTaskRenderer(
      BiFunction<TaskResult, Context, Response> submissionListener, ProcessEngine processEngine) {
    super(
        new CamundaPropertiesResolver<>(processEngine.getRepositoryService()),
        new TaskVariablesResolver(processEngine.getTaskService()));

    this.submissionListener = submissionListener;
    this.processEngine = processEngine;
    this.modalRenderer = new ModalRenderer(propertiesResolver, variablesResolver);
  }

  @Override
  public boolean canRender(TaskFormData formData) {
    return modalRenderer.canRender(formData);
  }

  @Override
  public RequestConfigurator<ChatPostMessageRequest.ChatPostMessageRequestBuilder> initialMessage(
      TaskDetails taskDetails, FormData formData) {
    return req ->
        req.text("Task: " + taskDetails.getName())
            .blocks(
                Arrays.asList(
                    section(
                        section ->
                            section.text(
                                markdownText(
                                    "You have a new task:\n*" + taskDetails.getName() + "*"))),
                    ActionsBlock.builder()
                        .blockId("accept")
                        .elements(
                            Collections.singletonList(
                                ButtonElement.builder()
                                    .actionId(generateOpenModalActionId(taskDetails.getId()))
                                    .text(plainText("Open"))
                                    .build()))
                        .build()));
  }

  private String generateOpenModalActionId(String taskId) {
    return "modal-task-open/" + taskId;
  }

  private String generateSubmitModalActionId(String callbackId) {
    return "modal-task-submit/" + callbackId;
  }

  @Override
  public List<BlockActionRegistration> blockActionRegistrations() {
    return Collections.singletonList(
        ImmutableBlockActionRegistration.builder()
            .pattern(taskOpenPattern)
            .blockActionHandler(this::openHandler)
            .build());
  }

  @Override
  public List<ViewSubmissionRegistration> viewSubmissionRegistrations() {
    return Collections.singletonList(
        ImmutableViewSubmissionRegistration.builder()
            .pattern(taskSubmitPattern)
            .viewSubmissionHandler(this::submitHandler)
            .build());
  }

  private Response submitHandler(ViewSubmissionRequest req, ViewSubmissionContext ctx) {

    Map<String, Map<String, ViewState.Value>> viewStateValues =
        req.getPayload().getView().getState().getValues();

    String taskId = extractTaskId(taskSubmitPattern, req.getPayload().getView().getCallbackId());
    FormData formData = processEngine.getFormService().getTaskFormData(taskId);

    Map<String, Object> taskVariables = new HashMap<>();
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
                                value -> taskVariables.put(field.getId(), value)))
                    .peekLeft(error -> errors.put(field.getId(), error)));

    if (!errors.isEmpty()) {
      return ctx.ackWithErrors(errors);
    } else {
      TaskResult taskResult =
          ImmutableTaskResult.builder().taskId(taskId).taskVariables(taskVariables).build();

      logger.debug("Submitting task {} with values {}", taskId, taskVariables);

      return submissionListener.apply(taskResult, ctx);
    }
  }

  private Response openHandler(BlockActionRequest req, ActionContext ctx) {
    String actionId = req.getPayload().getActions().get(0).getActionId();
    String taskId = extractTaskId(taskOpenPattern, actionId);
    TaskDetails task = getTaskById(taskId);
    FormData formData = processEngine.getFormService().getTaskFormData(task.getId());
    return openModal(ctx, task, formData);
  }

  public Response openModal(ActionContext ctx, TaskDetails task, FormData formData) {
    requireOk(
        () ->
            ctx.client()
                .viewsOpen(
                    r ->
                        r.triggerId(ctx.getTriggerId())
                            .view(
                                modalRenderer.buildModal(
                                    task, formData, generateSubmitModalActionId(task.getId())))));

    return ctx.ack();
  }

  public Response updateModal(
      ViewSubmissionContext ctx, TaskDetails followUpTask, TaskFormData formData) {
    return ctx.ack(
        r ->
            r.responseAction("update")
                .view(
                    modalRenderer.buildModal(
                        followUpTask,
                        formData,
                        generateSubmitModalActionId(followUpTask.getId()))));
  }

  public String extractTaskId(Pattern pattern, String identifier) {
    Matcher matcher = pattern.matcher(identifier);
    if (matcher.find()) {
      return matcher.group(1);
    } else {
      throw new IllegalArgumentException("Invalid action id.");
    }
  }

  protected TaskDetails getTaskById(String taskId) {
    Task task = processEngine.getTaskService().createTaskQuery().taskId(taskId).singleResult();
    return TaskDetails.of(task);
  }
}
