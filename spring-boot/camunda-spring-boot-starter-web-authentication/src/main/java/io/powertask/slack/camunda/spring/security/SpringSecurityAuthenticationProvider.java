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
package io.powertask.slack.camunda.spring.security;

import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.rest.security.auth.AuthenticationResult;
import org.camunda.bpm.engine.rest.security.auth.impl.ContainerBasedAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Exposes a Spring Security session as a Camunda AuthenticationResult
 *
 * <p>Code from
 * https://github.com/camunda-consulting/code/blob/master/snippets/springboot-security-sso/src/main/java/com/camunda/demo/filter/webapp/SpringSecurityAuthenticationProvider.java
 */
public class SpringSecurityAuthenticationProvider extends ContainerBasedAuthenticationProvider {
  @Override
  public AuthenticationResult extractAuthenticatedUser(
      HttpServletRequest request, ProcessEngine engine) {
    return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
        .map(Authentication::getName)
        // We can leave groups and tenants null, in which case
        // org.camunda.bpm.webapp.impl.security.auth.AuthenticationService
        // will look them up for us, based on the userId.
        .map(userId -> new AuthenticationResult(userId, true))
        .orElse(AuthenticationResult.unsuccessful());
  }
}
