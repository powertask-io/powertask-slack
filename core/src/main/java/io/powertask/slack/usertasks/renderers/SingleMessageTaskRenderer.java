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
import static io.powertask.slack.usertasks.renderers.MessageComponents.getDescriptionBlocks;
import static io.powertask.slack.usertasks.renderers.MessageComponents.getVariablesBlocks;

import com.slack.api.RequestConfigurator;
import com.slack.api.app_backend.interactive_components.payload.BlockActionPayload;
import com.slack.api.bolt.context.Context;
import com.slack.api.bolt.context.builtin.ActionContext;
import com.slack.api.bolt.request.builtin.BlockActionRequest;
import com.slack.api.bolt.response.Response;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.composition.BlockCompositions;
import io.powertask.slack.Form;
import io.powertask.slack.FormField;
import io.powertask.slack.TaskService;
import io.powertask.slack.formfields.BooleanField;
import io.powertask.slack.usertasks.Task;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SingleMessageTaskRenderer implements TaskRenderer {

  private static final Pattern taskIdPattern =
      Pattern.compile("^single-message-task/([a-z0-9\\-]+)/[0-9]+$");
  protected final BiFunction<TaskResult, Context, Response> submissionListener;

  private final TaskService taskService;

  public SingleMessageTaskRenderer(
      TaskService taskService, BiFunction<TaskResult, Context, Response> submissionListener) {
    this.taskService = taskService;
    this.submissionListener = submissionListener;
  }

  @Override
  // TODO, we could render more forms types inline, like an enum-only form as multiple buttons
  public boolean canRender(Form form) {
    List<FormField<?>> fields = form.fields();
    return fields.size() == 1
        && (fields.get(0) instanceof BooleanField)
        && fields.get(0).required();
  }

  @Override
  public RequestConfigurator<ChatPostMessageRequest.ChatPostMessageRequestBuilder> initialMessage(
      Task task, Form form) {
    BooleanField formField = (BooleanField) form.fields().get(0);

    List<LayoutBlock> blocks = new ArrayList<>();
    blocks.addAll(messageBlocks(task));
    blocks.addAll(getDescriptionBlocks(task));
    blocks.addAll(getVariablesBlocks(taskService, task));
    blocks.add(inlineFormHeader(formField));
    blocks.add(inlineForm(task, formField));

    return req -> req.text("Task: " + task.name()).blocks(blocks);
  }

  private LayoutBlock inlineFormHeader(FormField<?> formField) {
    String hintMarkdown = formField.hint().map(hint -> "\n_" + hint + "_").orElse("");

    return section(
        section -> section.text(markdownText("*" + formField.label() + "*\n" + hintMarkdown)));
  }

  private LayoutBlock inlineForm(Task task, BooleanField formField) {
    return actions(
        actions ->
            actions
                .blockId(formField.id())
                .elements(
                    asElements(
                        button(
                            b ->
                                b.actionId(generateActionId(task.id(), 1))
                                    .text(
                                        formField
                                            .trueLabel()
                                            .map(BlockCompositions::plainText)
                                            .orElse(plainText("Yes")))
                                    .value("true")),
                        button(
                            b ->
                                b.actionId(generateActionId(task.id(), 2))
                                    .text(
                                        formField
                                            .falseLabel()
                                            .map(BlockCompositions::plainText)
                                            .orElse(plainText("No")))
                                    .value("false")))));
  }

  public Map<String, Object> extractTaskVariables(BlockActionRequest req) {
    return req.getPayload().getActions().stream()
        .collect(
            Collectors.toMap(
                BlockActionPayload.Action::getBlockId, BlockActionPayload.Action::getValue));
  }

  private List<LayoutBlock> messageBlocks(Task task) {
    return asBlocks(
        section(
            section -> section.text(markdownText("You have a new task:* " + task.name() + "*"))),
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

  @Override
  public List<ViewSubmissionRegistration> viewSubmissionRegistrations() {
    return Collections.emptyList();
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
