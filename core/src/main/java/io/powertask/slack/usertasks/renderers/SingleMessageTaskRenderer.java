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

import static com.slack.api.model.block.Blocks.*;
import static com.slack.api.model.block.composition.BlockCompositions.markdownText;
import static com.slack.api.model.block.composition.BlockCompositions.plainText;
import static com.slack.api.model.block.element.BlockElements.asElements;
import static com.slack.api.model.block.element.BlockElements.button;
import static io.powertask.slack.modals.renderers.fieldrenderers.AbstractFieldRenderer.PROPERTY_SLACK_HINT;

import com.slack.api.RequestConfigurator;
import com.slack.api.app_backend.interactive_components.payload.BlockActionPayload;
import com.slack.api.bolt.context.Context;
import com.slack.api.bolt.context.builtin.ActionContext;
import com.slack.api.bolt.request.builtin.BlockActionRequest;
import com.slack.api.bolt.response.Response;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.model.block.LayoutBlock;
import io.powertask.slack.CamundaPropertiesResolver;
import io.powertask.slack.FieldInformation;
import io.powertask.slack.FormLikePropertiesBase;
import io.powertask.slack.modals.renderers.fieldrenderers.AbstractFieldRenderer;
import io.powertask.slack.modals.renderers.fieldrenderers.BooleanFieldRenderer;
import io.powertask.slack.usertasks.TaskDetails;
import io.powertask.slack.usertasks.TaskVariablesResolver;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.form.FormData;
import org.camunda.bpm.engine.form.FormField;
import org.camunda.bpm.engine.form.TaskFormData;
import org.camunda.bpm.engine.impl.form.type.BooleanFormType;

public class SingleMessageTaskRenderer extends FormLikePropertiesBase<TaskDetails>
    implements TaskRenderer {

  private static final Pattern taskIdPattern =
      Pattern.compile("^single-message-task/([a-z0-9\\-]+)/[0-9]+$");
  protected final BiFunction<TaskResult, Context, Response> submissionListener;

  public SingleMessageTaskRenderer(
      BiFunction<TaskResult, Context, Response> submissionListener, ProcessEngine processEngine) {
    super(
        new CamundaPropertiesResolver<>(processEngine.getRepositoryService()),
        new TaskVariablesResolver(processEngine.getTaskService()));
    this.submissionListener = submissionListener;
  }

  @Override
  // TODO, we could render more forms types inline, like an enum-only form as multiple buttons
  public boolean canRender(TaskFormData taskFormData) {
    List<FormField> fields = taskFormData.getFormFields();
    return fields.size() == 1
        && (fields.get(0).getType() instanceof BooleanFormType)
        && FieldInformation.hasConstraint(fields.get(0), AbstractFieldRenderer.CONSTRAINT_REQUIRED);
  }

  @Override
  public RequestConfigurator<ChatPostMessageRequest.ChatPostMessageRequestBuilder> initialMessage(
      TaskDetails task, FormData formData) {
    FormField formField = formData.getFormFields().get(0);

    List<LayoutBlock> blocks = new ArrayList<>();
    blocks.addAll(messageBlocks(task));
    blocks.addAll(getDescriptionBlocks(task));
    blocks.addAll(getVariablesBlocks(task));
    blocks.add(inlineFormHeader(formField));
    blocks.add(inlineForm(task, formField));

    return req -> req.text("Task: " + task.getName()).blocks(blocks);
  }

  private LayoutBlock inlineFormHeader(FormField formField) {
    String hintMarkdown =
        FieldInformation.getStringProperty(formField, PROPERTY_SLACK_HINT)
            .map(hint -> "\n_" + hint + "_")
            .orElse("");

    return section(
        section -> section.text(markdownText("*" + formField.getLabel() + "*\n" + hintMarkdown)));
  }

  private LayoutBlock inlineForm(TaskDetails task, FormField formField) {
    return actions(
        actions ->
            actions
                .blockId(formField.getId())
                .elements(
                    asElements(
                        button(
                            b ->
                                b.actionId(generateActionId(task.getId(), 1))
                                    .text(
                                        FieldInformation.getPlainTextProperty(
                                                formField,
                                                BooleanFieldRenderer.PROPERTY_SLACK_TRUE_LABEL)
                                            .orElse(plainText("Yes")))
                                    .value("true")),
                        button(
                            b ->
                                b.actionId(generateActionId(task.getId(), 2))
                                    .text(
                                        FieldInformation.getPlainTextProperty(
                                                formField,
                                                BooleanFieldRenderer.PROPERTY_SLACK_FALSE_LABEL)
                                            .orElse(plainText("No")))
                                    .value("false")))));
  }

  public Map<String, Object> extractTaskVariables(BlockActionRequest req) {
    return req.getPayload().getActions().stream()
        .collect(
            Collectors.toMap(
                BlockActionPayload.Action::getBlockId, BlockActionPayload.Action::getValue));
  }

  private List<LayoutBlock> messageBlocks(TaskDetails taskDetails) {
    return asBlocks(
        section(
            section ->
                section.text(markdownText("You have a new task:* " + taskDetails.getName() + "*"))),
        divider());
  }

  @Override
  public List<BlockActionRegistration> blockActionRegistrations() {
    return Collections.singletonList(
        ImmutableBlockActionRegistration.builder()
            .pattern(taskIdPattern)
            .blockActionHandler(this::handleBlockAction)
            .build());
  }

  public Response handleBlockAction(BlockActionRequest req, ActionContext ctx) {
    Map<String, Object> taskVariables = extractTaskVariables(req);
    String taskId = extractTaskId(req.getPayload().getActions().get(0).getActionId());

    TaskResult taskResult =
        ImmutableTaskResult.builder().taskId(taskId).taskVariables(taskVariables).build();

    return submissionListener.apply(taskResult, ctx);
  }

  public String extractTaskId(String actionId) {
    Matcher matcher = taskIdPattern.matcher(actionId);
    if (matcher.find()) {
      return matcher.group(1);
    } else {
      throw new IllegalArgumentException("Invalid action id.");
    }
  }

  private String generateActionId(String taskId, Integer index) {
    return "single-message-task/" + taskId + "/" + index.toString();
  }
}
