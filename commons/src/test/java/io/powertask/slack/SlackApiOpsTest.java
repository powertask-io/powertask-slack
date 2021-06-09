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
package io.powertask.slack;

import com.slack.api.methods.SlackApiTextResponse;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SlackApiOpsTest {

  @Test
  public void successResponse() {

    SlackApiTextResponse response = new ChatPostMessageResponse();
    response.setOk(true);

    Assertions.assertEquals(response, SlackApiOps.requireOk(() -> response));
  }

  @Test
  public void errorResponse() {
    SlackApiTextResponse response = new ChatPostMessageResponse();
    response.setOk(false);
    response.setError("BOOM!");

    RuntimeException e =
        Assertions.assertThrows(
            RuntimeException.class, () -> SlackApiOps.requireOk(() -> response));
    Assertions.assertEquals("Slack API response indicated an error: BOOM!", e.getMessage());
  }

  @Test
  public void callFailure() {
    RuntimeException e =
        Assertions.assertThrows(
            RuntimeException.class,
            () ->
                SlackApiOps.requireOk(
                    () -> {
                      throw new IOException("BOOM!");
                    }));
    Assertions.assertEquals("Slack API call failure", e.getMessage());
    Assertions.assertEquals("BOOM!", e.getCause().getMessage());
  }
}
