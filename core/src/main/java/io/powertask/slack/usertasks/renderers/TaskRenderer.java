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

import com.slack.api.RequestConfigurator;
import com.slack.api.bolt.handler.builtin.BlockActionHandler;
import com.slack.api.bolt.handler.builtin.ViewSubmissionHandler;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import io.powertask.slack.usertasks.TaskDetails;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.camunda.bpm.engine.form.FormData;
import org.camunda.bpm.engine.form.TaskFormData;
import org.immutables.value.Value;

// The idea is to have something that doesn't depend on the Camunda engine, nor the Slack
// App (other than some Slack message types)
public interface TaskRenderer {

  @Value.Immutable
  interface TaskResult {
    String taskId();

    Map<String, Object> taskVariables();
  }

  @Value.Immutable
  interface BlockActionRegistration {
    Pattern pattern();

    BlockActionHandler blockActionHandler();
  }

  @Value.Immutable
  interface ViewSubmissionRegistration {
    Pattern pattern();

    ViewSubmissionHandler viewSubmissionHandler();
  }

  boolean canRender(TaskFormData formData);

  RequestConfigurator<ChatPostMessageRequest.ChatPostMessageRequestBuilder> initialMessage(
      TaskDetails taskDetails, FormData formData);

  default List<BlockActionRegistration> blockActionRegistrations() {
    return Collections.emptyList();
  }

  default List<ViewSubmissionRegistration> viewSubmissionRegistrations() {
    return Collections.emptyList();
  }
}
