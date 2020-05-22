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
package io.powertask.slack.usertasks;

import static io.powertask.slack.FunctionOps.wrapExceptions;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.slack.api.RequestConfigurator;
import com.slack.api.bolt.App;
import com.slack.api.bolt.context.Context;
import com.slack.api.bolt.context.builtin.ActionContext;
import com.slack.api.bolt.context.builtin.ViewSubmissionContext;
import com.slack.api.bolt.response.Response;
import com.slack.api.methods.AsyncMethodsClient;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.model.block.Blocks;
import com.slack.api.model.block.composition.BlockCompositions;
import io.powertask.slack.Form;
import io.powertask.slack.FormService;
import io.powertask.slack.FutureOps;
import io.powertask.slack.ImmutableMessageRef;
import io.powertask.slack.MessageRef;
import io.powertask.slack.TaskService;
import io.powertask.slack.identity.UserResolver;
import io.powertask.slack.usertasks.renderers.ModalTaskRenderer;
import io.powertask.slack.usertasks.renderers.SingleMessageTaskRenderer;
import io.powertask.slack.usertasks.renderers.TaskRenderer;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserTaskDispatcher {

  private static final Logger logger = LoggerFactory.getLogger(UserTaskDispatcher.class);

  // TODO, make some type-safe data access object for this, that also does the serialization. See
  // the Camunda best practices.
  private static final String VARIABLE_POWERTASK_MESSAGEREFS = "powertaskMessageRefs";

  private final List<TaskRenderer> taskRenderers;
  private final ModalTaskRenderer modalTaskRenderer;
  private final AsyncMethodsClient asyncMethodsClient;
  private final UserResolver userResolver;
  private final Gson gson; // TODO, or should we piggy-back on the shaded Camunda Gson class?
  private final TaskService taskService;
  private final FormService formService;

  public UserTaskDispatcher(
      AsyncMethodsClient asyncMethodsClient,
      UserResolver userResolver,
      App app,
      TaskService taskService,
      FormService formService) {
    logger.info("Initializing SlackTaskProcessor");
    this.asyncMethodsClient = asyncMethodsClient;
    this.userResolver = userResolver;
    this.taskService = taskService;
    this.formService = formService;
    this.gson = new Gson();

    SingleMessageTaskRenderer singleMessageTaskRenderer =
        new SingleMessageTaskRenderer(taskService, this::submitAndShowNextTask);
    this.modalTaskRenderer =
        new ModalTaskRenderer(taskService, formService, this::submitAndShowNextTask);

    // These are ordered, the first renderer that returns true on the `canRender` method
    // will be used.
    this.taskRenderers = Arrays.asList(singleMessageTaskRenderer, modalTaskRenderer);

    taskRenderers.forEach(
        renderer -> {
          logger.info("Setting up task renderer " + renderer.getClass().getSimpleName());
          renderer
              .blockActionRegistrations()
              .forEach(
                  registration -> {
                    logger.info("Registering block action for " + registration.pattern());
                    app.blockAction(registration.pattern(), registration.blockActionHandler());
                  });

          renderer
              .viewSubmissionRegistrations()
              .forEach(
                  registration -> {
                    logger.info("Registering view submission for " + registration.pattern());
                    app.viewSubmission(
                        registration.pattern(), registration.viewSubmissionHandler());
                  });
        });
  }

  protected Optional<Task> getFollowUpTask(Task task) {
    return taskService.followUpTask(task);
  }

  private Response submitAndShowNextTask(TaskRenderer.TaskResult taskResult, Context ctx) {
    Task task = taskService.taskById(taskResult.taskId());

    formService.submitTaskForm(taskResult.taskId(), taskResult.taskVariables());

    return getFollowUpTask(task)
        .map(
            followUpTask -> {
              Form form = formService.taskForm(followUpTask.id()).get(); // TODO
              if (modalTaskRenderer.canRender(form)) {
                if (ctx instanceof ActionContext) {
                  return modalTaskRenderer.openModal((ActionContext) ctx, followUpTask, form);
                } else if (ctx instanceof ViewSubmissionContext) {
                  return modalTaskRenderer.updateModal(
                      (ViewSubmissionContext) ctx, followUpTask, form);
                } else {
                  throw new RuntimeException("Unknown Context!");
                }
              } else {
                return ctx.ack();
              }
            })
        .orElseGet(ctx::ack);
  }

  // TODO, should this be delegated to the task renderers, for customized formatting?
  public void notifyTaskCompletion(Task task) {
    String taskCompletedBy = userResolver.toSlackUserId(task.assignee());
    String taskCompletedByString = " by <@" + taskCompletedBy + ">";

    // TODO, can we get this from somewhere better, like the actual event?
    long unixTime = System.currentTimeMillis() / 1000L;

    // TODO, nicer fallback?
    String timestampString =
        "<!date^"
            + unixTime
            + "^completed {date_short_pretty} at {time}|"
            + OffsetDateTime.now().toString()
            + ">";

    String contextString = "Task *" + task.name() + "* " + timestampString + taskCompletedByString;

    String refsJson =
        (String)
            taskService
                .getVariable(task.id(), VARIABLE_POWERTASK_MESSAGEREFS)
                .get(); // TODO, check optional
    List<MessageRef> messageRefs =
        wrapExceptions(
            () -> gson.fromJson(refsJson, new TypeToken<List<ImmutableMessageRef>>() {}.getType()));

    messageRefs.forEach(
        messageRef ->
            asyncMethodsClient.chatUpdate(
                req ->
                    req.ts(messageRef.ts())
                        .channel(messageRef.channel())
                        .blocks(
                            Collections.singletonList(
                                Blocks.context(
                                    Collections.singletonList(
                                        BlockCompositions.markdownText(contextString)))))));
  }

  public void notifyTaskAssignment(Task task) {

    // TODO, probably we can optimize this at some point, predetermining this for all tasks in all
    // deployments.
    Form form = formService.taskForm(task.id()).get(); // TODO
    Optional<TaskRenderer> selectedRenderer = selectRenderer(form);

    selectedRenderer.ifPresent(renderer -> executeRenderer(renderer, task, form));

    if (!selectedRenderer.isPresent()) {
      logger.info("No task renderer found for this type of task!");
    }
  }

  private Optional<TaskRenderer> selectRenderer(Form form) {
    return taskRenderers.stream().filter(tr -> tr.canRender(form)).findFirst();
  }

  // TODO, this method is overly complicated, because it was built with the idea of having multiple
  // assignments for a task. Now, that's not the case, but we probably want to move towards
  // announcing to
  // all candidateUsers / candidateGroups and not just the assignee, so when we make that move, this
  // becomes
  // reasoble again (of course could still use cleanup :))
  private void executeRenderer(TaskRenderer renderer, Task task, Form form) {
    getAssigneeChannels(task)
        .thenAccept(
            slackChannels -> {
              if (slackChannels.isEmpty()) {
                logger.debug("No slack channels found for assignee " + task.assignee());
              } else {
                RequestConfigurator<ChatPostMessageRequest.ChatPostMessageRequestBuilder>
                    configurator = renderer.initialMessage(task, form);

                // TODO, handle failures of all the CompletionStage's here.
                CompletionStage<List<ChatPostMessageResponse>> responsesCompletionStage =
                    slackChannels.stream()
                        .map(
                            channel ->
                                asyncMethodsClient.chatPostMessage(
                                    req -> configurator.configure(req).channel(channel)))
                        .collect(FutureOps.completionStageListCollector());

                CompletionStage<List<MessageRef>> messageRefs =
                    responsesCompletionStage.thenApply(
                        responses ->
                            responses.stream()
                                .map(
                                    response ->
                                        ImmutableMessageRef.builder()
                                            .channel(response.getChannel())
                                            .ts(response.getMessage().getTs())
                                            .build())
                                .collect(Collectors.toList()));

                // There's a race condition here; we store the variables only after submitting the
                // message to slack,
                // so in theory a user could very quickly open the modal and respond to it, before
                // this is saved.
                // But it seems unlikely that it will happen.
                messageRefs.thenAccept(
                    refs -> {
                      String serialized = wrapExceptions(() -> gson.toJson(refs));
                      logger.debug(
                          "Storing serialized message ref: "
                              + serialized
                              + " for task "
                              + task.id());

                      taskService.setVariable(
                          task.id(), VARIABLE_POWERTASK_MESSAGEREFS, serialized);
                    });
              }
            });
  }

  private CompletionStage<Set<String>> getAssigneeChannels(Task task) {
    return CompletableFuture.completedFuture(
        Collections.singleton(userResolver.toSlackUserId(task.assignee())));
  }
}
