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
package io.powertask.slack.servicetasks;

import static io.powertask.slack.FunctionOps.wrapExceptions;

import com.google.gson.Gson;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import io.powertask.slack.ImmutableMessageRef;
import io.powertask.slack.MessageRef;
import io.powertask.slack.identity.UserResolver;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SlackService {

  private static final Logger logger = LoggerFactory.getLogger(SlackService.class);

  private final MethodsClient methodsClient;
  private final UserResolver userResolver;
  private final Gson gson;

  public SlackService(MethodsClient methodsClient, UserResolver userResolver) {
    this.methodsClient = methodsClient;
    this.userResolver = userResolver;
    this.gson = new Gson();
  }

  // TODO, check if this works well with Spin
  public String postMessage(String engineUserId, String message)
      throws IOException, SlackApiException {
    ChatPostMessageResponse response =
        methodsClient.chatPostMessage(
            req -> req.channel(userResolver.toSlackUserId(engineUserId)).text(message));

    MessageRef messageRef =
        ImmutableMessageRef.builder()
            .channel(response.getMessage().getChannel())
            .ts(response.getTs())
            .build();

    return wrapExceptions(() -> gson.toJson(messageRef));
  }
}
