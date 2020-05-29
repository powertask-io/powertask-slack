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
package io.powertask.slack.camunda.spring.config;

import io.powertask.slack.camunda.identitysync.IdentitySyncConfiguration;
import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Lazy;

@Lazy
@ConfigurationProperties(prefix = "powertask.slack.camunda.identitysync")
@Data
public class IdentitySyncProperties implements IdentitySyncConfiguration {

  public GroupMode groupMode = GroupMode.GROUPS;
  public Duration syncInterval = Duration.ofHours(1);
  public AdminMode adminMode = AdminMode.SLACK_ADMIN;
  public String adminGroupId;
}
