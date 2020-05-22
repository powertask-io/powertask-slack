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

import static io.powertask.slack.spring.ConfigurationKeys.POWERTASK_APP_HOME_ENABLED;
import static io.powertask.slack.spring.ConfigurationKeys.POWERTASK_ENGINE_USERRESOLVER_USER_ID_TYPE;
import static io.powertask.slack.spring.ConfigurationKeys.POWERTASK_SERVICETASKS_SLACKSERVICE_ENABLED;

import com.slack.api.bolt.App;
import com.slack.api.bolt.servlet.SlackAppServlet;
import com.slack.api.methods.AsyncMethodsClient;
import com.slack.api.methods.MethodsClient;
import io.powertask.slack.FormService;
import io.powertask.slack.ProcessService;
import io.powertask.slack.TaskService;
import io.powertask.slack.apphome.AppHome;
import io.powertask.slack.apphome.ProcessDispatcher;
import io.powertask.slack.identity.CachingResolver;
import io.powertask.slack.identity.EmailUserResolver;
import io.powertask.slack.identity.SlackIdUserResolver;
import io.powertask.slack.identity.UserResolver;
import io.powertask.slack.servicetasks.SlackService;
import io.powertask.slack.usertasks.UserTaskDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public class PowertaskSlackAutoConfiguration {

  private static final Logger logger =
      LoggerFactory.getLogger(PowertaskSlackAutoConfiguration.class);

  public PowertaskSlackAutoConfiguration() {
    logger.info("Auto-configuring Powertask Slack");
  }

  @Bean
  @ConditionalOnMissingBean(UserTaskDispatcher.class)
  public UserTaskDispatcher userTaskDispatcher(
      AsyncMethodsClient asyncMethodsClient,
      UserResolver userResolver,
      App app,
      TaskService taskService,
      FormService formService) {
    return new UserTaskDispatcher(asyncMethodsClient, userResolver, app, taskService, formService);
  }

  @Bean
  @ConditionalOnMissingBean(SlackService.class)
  @ConditionalOnProperty(value = POWERTASK_SERVICETASKS_SLACKSERVICE_ENABLED)
  public SlackService slackService(MethodsClient methodsClient, UserResolver userResolver) {
    return new SlackService(methodsClient, userResolver);
  }

  @Bean
  // TODO, how to make this conditional?
  public ServletRegistrationBean<SlackAppServlet> slackAppServletBean(App app) {
    logger.info("Creating ServletRegistrationBean for Slack App Servlet");
    ServletRegistrationBean<SlackAppServlet> bean =
        new ServletRegistrationBean<>(new SlackAppServlet(app), "/slack/events/");
    bean.setLoadOnStartup(1);
    return bean;
  }

  @Bean
  @ConditionalOnMissingBean(UserResolver.class)
  @ConditionalOnProperty(
      value = POWERTASK_ENGINE_USERRESOLVER_USER_ID_TYPE,
      havingValue = "email",
      matchIfMissing = true)
  public UserResolver emailUserResolver(MethodsClient methodsClient) {
    return new CachingResolver(new EmailUserResolver(methodsClient));
  }

  @Bean
  @ConditionalOnMissingBean(UserResolver.class)
  @ConditionalOnProperty(
      value = POWERTASK_ENGINE_USERRESOLVER_USER_ID_TYPE,
      havingValue = "slack-id")
  public UserResolver slackIdUserResolver() {
    return new SlackIdUserResolver();
  }

  @Bean
  @ConditionalOnMissingBean(ProcessDispatcher.class)
  @ConditionalOnProperty(
      value = POWERTASK_APP_HOME_ENABLED,
      havingValue = "true",
      matchIfMissing = true)
  public ProcessDispatcher processDispatcher(
      App app,
      ProcessService processEngine,
      TaskService taskService,
      FormService formService,
      UserResolver userResolver) {
    return new ProcessDispatcher(app, processEngine, taskService, formService, userResolver);
  }

  @Bean
  @ConditionalOnProperty(
      value = POWERTASK_APP_HOME_ENABLED,
      havingValue = "true",
      matchIfMissing = true)
  public AppHome appHome(App app, ProcessDispatcher processDispatcher) {
    return new AppHome(app, processDispatcher);
  }
}
