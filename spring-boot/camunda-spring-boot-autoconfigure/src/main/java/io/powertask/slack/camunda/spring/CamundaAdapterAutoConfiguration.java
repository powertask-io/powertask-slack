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
package io.powertask.slack.camunda.spring;

import com.slack.api.methods.MethodsClient;
import io.powertask.slack.camunda.CamundaFormService;
import io.powertask.slack.camunda.CamundaProcessService;
import io.powertask.slack.camunda.CamundaTaskService;
import io.powertask.slack.camunda.PropertiesResolver;
import io.powertask.slack.camunda.TaskMapper;
import io.powertask.slack.camunda.identitysync.IdentitySync;
import io.powertask.slack.camunda.plugin.TaskListenerPlugin;
import io.powertask.slack.camunda.plugin.UserTaskDispatcherListener;
import io.powertask.slack.camunda.spring.config.IdentitySyncProperties;
import io.powertask.slack.usertasks.UserTaskDispatcher;
import org.camunda.bpm.engine.FormService;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@Configuration
@EnableConfigurationProperties(IdentitySyncProperties.class)
public class CamundaAdapterAutoConfiguration {

  static final String POWERTASK_CAMUNDA_IDENTITY_SYNC_ENABLED =
      "powertask.camunda.identity-sync.enabled";

  @Bean
  // TODO, should this be conditional / configurable?
  TaskListenerPlugin taskListenerPlugin() {
    return new TaskListenerPlugin();
  }

  @Bean
  PropertiesResolver propertiesResolver(RepositoryService repositoryService) {
    return new PropertiesResolver(repositoryService);
  }

  @Bean(name = "powertaskListener")
  UserTaskDispatcherListener userTaskDispatcherListener(
      UserTaskDispatcher userTaskDispatcher, TaskMapper taskMapper) {
    return new UserTaskDispatcherListener(taskMapper, userTaskDispatcher);
  }

  @Bean
  TaskMapper taskMapper(PropertiesResolver propertiesResolver, RuntimeService runtimeService) {
    return new TaskMapper(propertiesResolver, runtimeService);
  }

  @Bean
  CamundaTaskService camundaTaskService(TaskService taskService, TaskMapper taskMapper) {
    return new CamundaTaskService(taskService, taskMapper);
  }

  @Bean
  CamundaProcessService camundaProcessService(
      ProcessEngine processEngine,
      CamundaFormService formService,
      PropertiesResolver propertiesResolver) {
    return new CamundaProcessService(processEngine, formService, propertiesResolver);
  }

  @Bean
  CamundaFormService camundaFormService(FormService formService) {
    return new CamundaFormService(formService);
  }

  @Bean
  @ConditionalOnProperty(
      value = POWERTASK_CAMUNDA_IDENTITY_SYNC_ENABLED,
      havingValue = "true",
      matchIfMissing = true)
  public IdentitySync identitySync(
      IdentitySyncProperties properties,
      MethodsClient methodsClient,
      IdentityService identityService) {
    return new IdentitySync(properties, methodsClient, identityService);
  }
}
