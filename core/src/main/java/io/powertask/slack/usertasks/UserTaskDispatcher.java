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
import io.powertask.slack.FutureOps;
import io.powertask.slack.ImmutableMessageRef;
import io.powertask.slack.MessageRef;
import io.powertask.slack.identity.UserResolver;
import io.powertask.slack.usertasks.renderers.ModalTaskRenderer;
import io.powertask.slack.usertasks.renderers.SingleMessageTaskRenderer;
import io.powertask.slack.usertasks.renderers.TaskRenderer;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.engine.form.TaskFormData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserTaskDispatcher implements TaskListener {

  private static final Logger logger = LoggerFactory.getLogger(UserTaskDispatcher.class);

  // TODO, make some type-safe data access object for this, that also does the serialization. See
  // the Camunda best practices.
  private static final String VARIABLE_POWERTASK_MESSAGEREFS = "powertaskMessageRefs";

  private final ProcessEngine processEngine;
  private final List<TaskRenderer> taskRenderers;
  private final ModalTaskRenderer modalTaskRenderer;
  private final AsyncMethodsClient asyncMethodsClient;
  private final UserResolver userResolver;
  private final Gson gson; // TODO, or should we piggy-back on the shaded Camunda Gson class?

  public UserTaskDispatcher(
      AsyncMethodsClient asyncMethodsClient,
      UserResolver userResolver,
      App app,
      ProcessEngine processEngine) {
    logger.info("Initializing SlackTaskProcessor");
    this.asyncMethodsClient = asyncMethodsClient;
    this.userResolver = userResolver;
    this.processEngine = processEngine;
    this.gson = new Gson();

    SingleMessageTaskRenderer singleMessageTaskRenderer =
        new SingleMessageTaskRenderer(this::submitAndShowNextTask, processEngine);
    this.modalTaskRenderer = new ModalTaskRenderer(this::submitAndShowNextTask, processEngine);

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

  protected Optional<TaskDetails> getFollowUpTask(TaskDetails task) {
    return processEngine.getTaskService().createTaskQuery()
        .taskAssignee(task.getAssignee()) // TODO, is this always correct?
        .processInstanceId(task.getProcessInstanceId()).list().stream()
        .map(TaskDetails::of)
        .findFirst();
  }

  private Response submitAndShowNextTask(TaskRenderer.TaskResult taskResult, Context ctx) {
    TaskDetails taskDetails =
        TaskDetails.of(
            processEngine
                .getTaskService()
                .createTaskQuery()
                .taskId(taskResult.taskId())
                .singleResult());

    processEngine.getFormService().submitTaskForm(taskResult.taskId(), taskResult.taskVariables());

    return getFollowUpTask(taskDetails)
        .map(
            followUpTask -> {
              TaskFormData formData =
                  processEngine.getFormService().getTaskFormData(followUpTask.getId());
              if (modalTaskRenderer.canRender(formData)) {
                if (ctx instanceof ActionContext) {
                  return modalTaskRenderer.openModal((ActionContext) ctx, followUpTask, formData);
                } else if (ctx instanceof ViewSubmissionContext) {
                  return modalTaskRenderer.updateModal(
                      (ViewSubmissionContext) ctx, followUpTask, formData);
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
  public void notifyTaskCompletion(TaskDetails taskDetails) {
    String taskCompletedBy = userResolver.toSlackUserId(taskDetails.getAssignee());
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

    String contextString =
        "Task *" + taskDetails.getName() + "* " + timestampString + taskCompletedByString;

    String refsJson =
        (String)
            processEngine
                .getTaskService()
                .getVariableLocal(taskDetails.getId(), VARIABLE_POWERTASK_MESSAGEREFS);
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

  public void notifyTaskAssignment(TaskDetails taskDetails) {

    // TODO, probably we can optimize this at some point, predetermining this for all tasks in all
    // deployments.
    TaskFormData taskFormData = processEngine.getFormService().getTaskFormData(taskDetails.getId());
    Optional<TaskRenderer> selectedRenderer = selectRenderer(taskFormData);

    selectedRenderer.ifPresent(renderer -> executeRenderer(renderer, taskDetails, taskFormData));

    if (!selectedRenderer.isPresent()) {
      logger.info("No task renderer found for this type of task!");
    }
  }

  private Optional<TaskRenderer> selectRenderer(TaskFormData taskFormData) {
    return taskRenderers.stream().filter(tr -> tr.canRender(taskFormData)).findFirst();
  }

  // TODO, this method is overly complicated, because it was built with the idea of having multiple
  // assignments for a task. Now, that's not the case, but we probably want to move towards
  // announcing to
  // all candidateUsers / candidateGroups and not just the assignee, so when we make that move, this
  // becomes
  // reasoble again (of course could still use cleanup :))
  private void executeRenderer(
      TaskRenderer renderer, TaskDetails taskDetails, TaskFormData taskFormData) {
    getAssigneeChannels(taskDetails)
        .thenAccept(
            slackChannels -> {
              if (slackChannels.isEmpty()) {
                logger.debug("No slack channels found for assignee " + taskDetails.getAssignee());
              } else {
                RequestConfigurator<ChatPostMessageRequest.ChatPostMessageRequestBuilder>
                    configurator = renderer.initialMessage(taskDetails, taskFormData);

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
                              + taskDetails.getId());
                      processEngine
                          .getTaskService()
                          .setVariableLocal(
                              taskDetails.getId(), VARIABLE_POWERTASK_MESSAGEREFS, serialized);
                    });
              }
            });
  }

  private CompletionStage<Set<String>> getAssigneeChannels(TaskDetails taskDetails) {
    return CompletableFuture.completedFuture(
        Collections.singleton(userResolver.toSlackUserId(taskDetails.getAssignee())));
  }

  @Override
  public void notify(DelegateTask delegateTask) {
    if (delegateTask.getEventName().equals(TaskListener.EVENTNAME_ASSIGNMENT)) {
      logger.debug("Task {} assigned to {}", delegateTask.getId(), delegateTask.getAssignee());
      notifyTaskAssignment(TaskDetails.of(delegateTask));
    } else if (delegateTask.getEventName().equals(TaskListener.EVENTNAME_COMPLETE)) {
      logger.debug("Task {} assigned to {}", delegateTask.getId(), delegateTask.getAssignee());
      notifyTaskCompletion(TaskDetails.of(delegateTask));
    }
  }
}
