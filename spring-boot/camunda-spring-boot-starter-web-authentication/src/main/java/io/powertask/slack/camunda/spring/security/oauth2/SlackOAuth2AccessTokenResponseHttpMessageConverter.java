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

/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;

public class SlackOAuth2AccessTokenResponseHttpMessageConverter
    extends AbstractHttpMessageConverter<OAuth2AccessTokenResponse> {

  private final ObjectMapper objectMapper;

  public SlackOAuth2AccessTokenResponseHttpMessageConverter() {
    super(MediaType.APPLICATION_JSON, new MediaType("application", "*+json"));
    objectMapper = new ObjectMapper();
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
  }

  @Override
  protected boolean supports(@NonNull Class<?> clazz) {
    return OAuth2AccessTokenResponse.class.isAssignableFrom(clazz);
  }

  @Override
  @NonNull
  protected OAuth2AccessTokenResponse readInternal(
      @NonNull Class<? extends OAuth2AccessTokenResponse> clazz, HttpInputMessage inputMessage)
      throws HttpMessageNotReadableException {
    try {

      SlackJsonResponse response =
          objectMapper.readValue(inputMessage.getBody(), SlackJsonResponse.class);

      if (response.authedUser == null) {
        throw new IOException("Json doesn't contain 'authed_user' field");
      }

      return OAuth2AccessTokenResponse.withToken(response.authedUser.accessToken)
          .tokenType(TokenType.BEARER)
          .scopes(new HashSet<>(Arrays.asList(response.authedUser.scope.split(","))))
          .expiresIn(3153600000L) // Approximately 100 years
          .build();
    } catch (IOException e) {
      throw new HttpMessageNotReadableException(
          "Failed to deserialize Slack Access Token response into known structure.", inputMessage);
    }
  }

  @Override
  protected void writeInternal(
      @NonNull OAuth2AccessTokenResponse tokenResponse, @NonNull HttpOutputMessage outputMessage)
      throws HttpMessageNotWritableException {
    throw new HttpMessageNotWritableException("Not implemented!");
  }

  // Subset of the JSON structure we're interested in
  private static class SlackJsonResponse {
    public AuthedUser authedUser;
  }

  private static class AuthedUser {
    public String accessToken;
    public String scope;
  }
}
