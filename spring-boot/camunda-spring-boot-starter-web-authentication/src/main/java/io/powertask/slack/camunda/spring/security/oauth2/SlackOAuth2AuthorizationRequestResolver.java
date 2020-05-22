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
package io.powertask.slack.camunda.spring.security.oauth2;

import java.util.Collections;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

/**
 * Adds the correct user_scope parameter to the authorization request towards Slack
 *
 * <p>The default one doesn't have support for additional parameters.
 */
public class SlackOAuth2AuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

  private final OAuth2AuthorizationRequestResolver defaultResolver;

  public SlackOAuth2AuthorizationRequestResolver(
      ClientRegistrationRepository clientRegistrationRepository) {
    defaultResolver =
        new DefaultOAuth2AuthorizationRequestResolver(
            clientRegistrationRepository, "/oauth2/authorization");
  }

  @Override
  public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
    return Optional.ofNullable(defaultResolver.resolve(request))
        .map(this::customizeAuthorizationRequest)
        .orElse(null);
  }

  @Override
  public OAuth2AuthorizationRequest resolve(
      HttpServletRequest request, String clientRegistrationId) {
    return Optional.ofNullable(defaultResolver.resolve(request, clientRegistrationId))
        .map(this::customizeAuthorizationRequest)
        .orElse(null);
  }

  private OAuth2AuthorizationRequest customizeAuthorizationRequest(OAuth2AuthorizationRequest req) {
    return OAuth2AuthorizationRequest.from(req)
        // TODO, add support for the 'team' parameter, so Slack automagically selects
        //  the right team :)
        .additionalParameters(Collections.singletonMap("user_scope", "identity.basic"))
        .build();
  }
}
