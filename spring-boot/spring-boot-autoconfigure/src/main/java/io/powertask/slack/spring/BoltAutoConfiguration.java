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
package io.powertask.slack.spring;

import com.slack.api.Slack;
import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import io.powertask.slack.spring.config.BoltAppProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(BoltAppProperties.class)
public class BoltAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(AppConfig.class)
  public AppConfig appConfig(Slack slack, BoltAppProperties boltAppProperties) {
    return AppConfig.builder()
        .slack(slack)
        .singleTeamBotToken(boltAppProperties.getSingleTeamBotToken())
        .signingSecret(boltAppProperties.getSigningSecret())
        .oAuthStartEnabled(boltAppProperties.isOAuthStartEnabled())
        .oAuthCallbackEnabled(boltAppProperties.isOAuthCallbackEnabled())
        .classicAppPermissionsEnabled(boltAppProperties.isClassicAppPermissionsEnabled())
        .clientId(boltAppProperties.getClientId())
        .clientSecret(boltAppProperties.getClientSecret())
        .redirectUri(boltAppProperties.getRedirectUri())
        .scope(boltAppProperties.getScope())
        .userScope(boltAppProperties.getUserScope())
        .appPath(boltAppProperties.getAppPath())
        .oauthStartPath(boltAppProperties.getOauthStartPath())
        .oauthCallbackPath(boltAppProperties.getOauthCallbackPath())
        .oauthCancellationUrl(boltAppProperties.getOauthCancellationUrl())
        .oauthCompletionUrl(boltAppProperties.getOauthCompletionUrl())
        .alwaysRequestUserTokenNeeded(boltAppProperties.isAlwaysRequestUserTokenNeeded())
        .appInitializersEnabled(boltAppProperties.isAppInitializersEnabled())
        .build();
  }

  @Bean
  @ConditionalOnMissingBean(App.class)
  public App slackApp(AppConfig appConfig) {
    return new App(appConfig);
  }
}
