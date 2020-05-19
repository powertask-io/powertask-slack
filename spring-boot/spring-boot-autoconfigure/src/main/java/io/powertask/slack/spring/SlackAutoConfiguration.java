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
import com.slack.api.SlackConfig;
import com.slack.api.methods.AsyncMethodsClient;
import com.slack.api.methods.MethodsClient;
import com.slack.api.util.http.SlackHttpClient;
import io.powertask.slack.spring.config.SlackProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SlackProperties.class)
public class SlackAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(SlackConfig.class)
  public SlackConfig slackConfig(SlackProperties slackProperties) {
    SlackConfig config = new SlackConfig();
    config.setProxyUrl(slackProperties.getProxyUrl());
    config.setPrettyResponseLoggingEnabled(slackProperties.isPrettyResponseLoggingEnabled());
    config.setLibraryMaintainerMode(slackProperties.isLibraryMaintainerMode());
    config.setFailOnUnknownProperties(slackProperties.isFailOnUnknownProperties());
    config.setTokenExistenceVerificationEnabled(
        slackProperties.isTokenExistenceVerificationEnabled());
    config.setAuditEndpointUrlPrefix(slackProperties.getAuditEndpointUrlPrefix());
    config.setMethodsEndpointUrlPrefix(slackProperties.getMethodsEndpointUrlPrefix());
    config.setScimEndpointUrlPrefix(slackProperties.getScimEndpointUrlPrefix());
    config.setStatusEndpointUrlPrefix(slackProperties.getStatusEndpointUrlPrefix());
    return config;
  }

  @Bean
  @ConditionalOnMissingBean(SlackHttpClient.class)
  public SlackHttpClient slackHttpClient() {
    return new SlackHttpClient();
  }

  @Bean
  @ConditionalOnMissingBean(Slack.class)
  public Slack slack(SlackConfig slackConfig, SlackHttpClient slackHttpClient) {
    return Slack.getInstance(slackConfig, slackHttpClient);
  }

  @Bean
  @ConditionalOnMissingBean(MethodsClient.class)
  public MethodsClient methodsClient(Slack slack, SlackProperties slackProperties) {
    return slack.methods(slackProperties.getToken());
  }

  @Bean
  @ConditionalOnMissingBean(AsyncMethodsClient.class)
  public AsyncMethodsClient asyncMethodsClient(Slack slack, SlackProperties slackProperties) {
    return slack.methodsAsync(slackProperties.getToken());
  }
}
