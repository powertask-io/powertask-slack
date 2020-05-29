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

import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.SlackApiResponse;
import java.io.IOException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SlackApiOps {

  private static final Logger logger = LoggerFactory.getLogger(SlackApiOps.class);

  @FunctionalInterface
  public interface SlackApiCall<T> {
    T get() throws IOException, SlackApiException;
  }

  public static <T extends SlackApiResponse> T requireOk(SlackApiCall<T> call) {
    try {
      T response = call.get();
      if (response.isOk()) {
        Optional.ofNullable(response.getWarning()).ifPresent(logger::warn);
        return response;
      } else {
        throw new RuntimeException(
            "Slack API response indicated an error: "
                + response.getError()
                + " - "
                + response.toString());
      }
    } catch (IOException | SlackApiException e) {
      throw new RuntimeException("Slack API call failure", e);
    }
  }
}
