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
package io.powertask.slack.spring.config;

import io.powertask.slack.spring.ConfigurationKeys;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Lazy;

@Lazy
@ConfigurationProperties(prefix = ConfigurationKeys.SLACK_BOLT)
@Data
public class BoltAppProperties {

  //
  // Note: properties and default copied from com.slack.api.bolt.AppConfig,
  // minus deprecated fields and 'Slack' object.
  //

  private String singleTeamBotToken;
  private String signingSecret;
  private boolean oAuthStartEnabled = false;
  private boolean oAuthCallbackEnabled = false;
  private boolean classicAppPermissionsEnabled = false;
  private String clientId;
  private String clientSecret;
  private String redirectUri;
  private String scope;
  private String userScope;
  private String appPath;
  private String oauthStartPath = "start";
  private String oauthCallbackPath = "callback";
  private String oauthCancellationUrl;
  private String oauthCompletionUrl;
  private boolean alwaysRequestUserTokenNeeded = false;
  private boolean appInitializersEnabled = true;
}
