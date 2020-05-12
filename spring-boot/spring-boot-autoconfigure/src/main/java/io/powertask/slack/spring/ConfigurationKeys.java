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

/*-
 * #%L
 * Powertask Slack - Spring Boot Autoconfigure
 * %%
 * Copyright (C) 2020 Lunatech
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

public class ConfigurationKeys {
  public static final String POWERTASK_SERVICETASKS_SLACKSERVICE_ENABLED =
      "powertask.servicetasks.slackservice.enabled";
  public static final String POWERTASK_ENGINE_USERRESOLVER_USER_ID_TYPE =
      "powertask.engine.userresolver.user-id-type";

  public static final String SLACK_BOLT = "slack.bolt";
  public static final String SLACK = "slack";
}
