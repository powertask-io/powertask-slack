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

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.RequestEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequestEntityConverter;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.web.client.RestTemplate;

/**
 * Retrieves user details from Slack as part of OAuth2 flow.
 *
 * <p>The default implementation can't handle the nested structure of Slack's response.
 */
public class SlackOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {
  private static final String NAME_ATTRIBUTE_KEY = "user_id";

  private final RestTemplate restTemplate = new RestTemplate();
  private final Converter<OAuth2UserRequest, RequestEntity<?>> requestEntityConverter;

  public SlackOAuth2UserService() {
    restTemplate.setErrorHandler(new OAuth2ErrorResponseErrorHandler());
    requestEntityConverter = new OAuth2UserRequestEntityConverter();
  }

  @Override
  public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
    RequestEntity<?> request =
        Optional.ofNullable(requestEntityConverter.convert(userRequest))
            .orElseThrow(
                () ->
                    new OAuth2AuthenticationException(
                        new OAuth2Error("Failed to convert user request")));

    Response response =
        Optional.ofNullable(restTemplate.exchange(request, Response.class).getBody())
            .orElseThrow(
                () ->
                    new OAuth2AuthenticationException(
                        new OAuth2Error("Failed to interpret Slack response")));

    Map<String, Object> attributes = new HashMap<>();
    attributes.put(NAME_ATTRIBUTE_KEY, response.user.id);
    Set<GrantedAuthority> authorities = new LinkedHashSet<>();
    authorities.add(new OAuth2UserAuthority(attributes));
    return new DefaultOAuth2User(authorities, attributes, NAME_ATTRIBUTE_KEY);
  }

  // The JSON structure we're interested in.
  private static class Response {
    public User user;
  }

  private static class User {
    public String id;
  }
}
