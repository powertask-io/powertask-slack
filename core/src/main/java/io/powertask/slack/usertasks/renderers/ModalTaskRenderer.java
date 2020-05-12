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
import static com.slack.api.model.view.Views.*;
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
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.element.ButtonElement;
import com.slack.api.model.view.View;
import com.slack.api.model.view.ViewState;
import io.powertask.slack.usertasks.TaskDetails;
import io.powertask.slack.usertasks.renderers.fieldrenderers.*;
import java.util.*;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.form.FormData;
import org.camunda.bpm.engine.form.FormField;
import org.camunda.bpm.engine.form.TaskFormData;
import org.camunda.bpm.engine.impl.form.type.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModalTaskRenderer extends AbstractTaskRenderer {

  private static final Logger logger = LoggerFactory.getLogger(ModalTaskRenderer.class);

  private static final Pattern taskOpenPattern =
      Pattern.compile("^modal-task-open/([a-z0-9\\-]+)$");
  private static final Pattern taskSubmitPattern =
      Pattern.compile("^modal-task-submit/([a-z0-9\\-]+)$");
  private final ProcessEngine processEngine;

  private static final String PROPERTY_SLACK_TITLE = "slack-title";

  public ModalTaskRenderer(
      BiFunction<TaskResult, Context, Response> submissionListener, ProcessEngine processEngine) {
    super(processEngine, submissionListener);

    this.processEngine = processEngine;
  }

  @Override
  public boolean canRender(TaskFormData formData) {
    return true; // TODO, YOLO
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
                getRenderer(field)
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
                .viewsOpen(r -> r.triggerId(ctx.getTriggerId()).view(buildModal(task, formData))));

    return ctx.ack();
  }

  public Response updateModal(
      ViewSubmissionContext ctx, TaskDetails followUpTask, TaskFormData formData) {
    return ctx.ack(r -> r.responseAction("update").view(buildModal(followUpTask, formData)));
  }

  private View buildModal(TaskDetails task, FormData formData) {
    String titleValue = getProperty(task, PROPERTY_SLACK_TITLE).orElse(task.getName());

    List<LayoutBlock> blocks = new ArrayList<>();

    blocks.addAll(getDescriptionBlocks(task));
    blocks.addAll(getVariablesBlocks(task));

    blocks.addAll(
        formData.getFormFields().stream()
            .map(field -> getRenderer(field).render(field))
            .collect(Collectors.toList()));

    return view(
        view ->
            view.callbackId(generateSubmitModalActionId(task.getId()))
                .type("modal")
                .notifyOnClose(false)
                .title(viewTitle(title -> title.type("plain_text").text(titleValue).emoji(true)))
                .submit(viewSubmit(submit -> submit.type("plain_text").text("Submit").emoji(true)))
                .close(viewClose(close -> close.type("plain_text").text("Cancel").emoji(true)))
                .blocks(blocks));
  }

  private FieldRenderer getRenderer(FormField field) {
    if (field.getType() instanceof StringFormType) {
      return new StringFieldRenderer(field);
    } else if (field.getType() instanceof EnumFormType) {
      return new EnumFieldRenderer(field);
    } else if (field.getType() instanceof BooleanFormType) {
      return new BooleanFieldRenderer(field);
    } else if (field.getType() instanceof LongFormType) {
      return new LongFieldRenderer(field);
    } else if (field.getType() instanceof DateFormType) {
      return new DateFieldRenderer(field);
    } else {
      throw new RuntimeException("Missing implementation for field type " + field.getType());
    }
  }

  public String extractTaskId(Pattern pattern, String identifier) {
    Matcher matcher = pattern.matcher(identifier);
    if (matcher.find()) {
      return matcher.group(1);
    } else {
      throw new IllegalArgumentException("Invalid action id.");
    }
  }
}
