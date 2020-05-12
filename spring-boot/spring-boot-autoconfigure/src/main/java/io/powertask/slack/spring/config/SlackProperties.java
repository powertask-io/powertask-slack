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

import com.slack.api.audit.AuditClient;
import com.slack.api.methods.MethodsClient;
import com.slack.api.scim.SCIMClient;
import com.slack.api.status.v2.StatusClient;
import io.powertask.slack.spring.ConfigurationKeys;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Lazy;

@Lazy
@ConfigurationProperties(prefix = ConfigurationKeys.SLACK)
@Data
public class SlackProperties {

  //
  // Note: properties and instructions copied from com.slack.api.SlackConfig,
  // plus the 'token' field, which doesn't reside on the main Slack object.
  //

  /** Access token */
  String token = null;

  /** The proxy server URL supposed to be used for all api calls. */
  String proxyUrl = null;

  /** Whether to enable pretty response logging */
  boolean prettyResponseLoggingEnabled = false;

  /**
   * Don't enable this flag in production. This flag enables some validation features for
   * development.
   */
  boolean libraryMaintainerMode = false;

  /**
   * If you would like to detect unknown properties by throwing exceptions, set this flag as true.
   */
  boolean failOnUnknownProperties = false;

  /**
   * Slack Web API client verifies the existence of tokens before sending HTTP requests to Slack
   * servers.
   */
  boolean tokenExistenceVerificationEnabled = false;

  String auditEndpointUrlPrefix = AuditClient.ENDPOINT_URL_PREFIX;

  String methodsEndpointUrlPrefix = MethodsClient.ENDPOINT_URL_PREFIX;

  String ScimEndpointUrlPrefix = SCIMClient.ENDPOINT_URL_PREFIX;

  String statusEndpointUrlPrefix = StatusClient.ENDPOINT_URL_PREFIX;
}
