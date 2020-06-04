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

import io.powertask.slack.camunda.spring.security.config.AuthenticationProperties;
import io.powertask.slack.camunda.spring.security.oauth2.SlackOAuth2AccessTokenResponseHttpMessageConverter;
import io.powertask.slack.camunda.spring.security.oauth2.SlackOAuth2AuthorizationRequestResolver;
import io.powertask.slack.camunda.spring.security.oauth2.SlackOAuth2UserService;
import java.util.Arrays;
import java.util.Collections;
import org.camunda.bpm.engine.rest.security.auth.ProcessEngineAuthenticationFilter;
import org.camunda.bpm.webapp.impl.security.auth.ContainerBasedAuthenticationFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties(AuthenticationProperties.class)
public class SlackOAuthSecurityConfig extends WebSecurityConfigurerAdapter {

  private final AuthenticationProperties authenticationProperties;

  public SlackOAuthSecurityConfig(AuthenticationProperties authenticationProperties) {
    this.authenticationProperties = authenticationProperties;
  }

  @Override
  // @formatter:off
  protected void configure(HttpSecurity http) throws Exception {
    ClientRegistrationRepository clientRegistrationRepository = clientRegistrationRepository();
    http.requestMatcher(req ->
        req.getRequestURI().startsWith("/login/") ||
            req.getRequestURI().startsWith("/oauth2/") ||
            req.getRequestURI().startsWith("/app/") ||
            req.getRequestURI().startsWith("/api/"))
        .authorizeRequests().anyRequest().authenticated()
        .and()
        .oauth2Login()
        .clientRegistrationRepository(clientRegistrationRepository)
        .authorizationEndpoint()
        .authorizationRequestResolver(
            new SlackOAuth2AuthorizationRequestResolver(clientRegistrationRepository))
        .and()
        .tokenEndpoint()
        .accessTokenResponseClient(accessTokenResponseClient())
        .and()
        .userInfoEndpoint()
        .userService(new SlackOAuth2UserService());
  }
  // @formatter:on

  private ClientRegistrationRepository clientRegistrationRepository() {
    ClientRegistration slack =
        ClientRegistration.withRegistrationId("slack")
            .clientId(authenticationProperties.getClientId())
            .clientSecret(authenticationProperties.getClientSecret())
            .clientAuthenticationMethod(
                ClientAuthenticationMethod.POST)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUriTemplate("{baseUrl}/login/oauth2/code/{registrationId}")
            .clientName("Slack")
            .authorizationUri("https://slack.com/oauth/v2/authorize")
            .tokenUri("https://slack.com/api/oauth.v2.access")
            .userInfoUri("https://slack.com/api/users.identity")
            .build();
    return new InMemoryClientRegistrationRepository(slack);
  }

  // Response client configured to use the SlackOAuth2AccessTokenResponseHttpMessageConverter
  private OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest>
  accessTokenResponseClient() {

    SlackOAuth2AccessTokenResponseHttpMessageConverter tokenResponseHttpMessageConverter =
        new SlackOAuth2AccessTokenResponseHttpMessageConverter();

    RestTemplate restTemplate =
        new RestTemplate(
            Arrays.asList(new FormHttpMessageConverter(), tokenResponseHttpMessageConverter));

    restTemplate.setErrorHandler(new OAuth2ErrorResponseErrorHandler());

    DefaultAuthorizationCodeTokenResponseClient accessTokenResponseClient =
        new DefaultAuthorizationCodeTokenResponseClient();
    accessTokenResponseClient.setRestOperations(restTemplate);
    return accessTokenResponseClient;
  }


  @Bean
  public FilterRegistrationBean<ContainerBasedAuthenticationFilter>
  containerBasedAuthenticationFilter() {
    FilterRegistrationBean<ContainerBasedAuthenticationFilter> filterRegistration =
        new FilterRegistrationBean<>();
    filterRegistration.setFilter(new ContainerBasedAuthenticationFilter());
    filterRegistration.setInitParameters(
        Collections.singletonMap(
            ProcessEngineAuthenticationFilter.AUTHENTICATION_PROVIDER_PARAM,
            SpringSecurityAuthenticationProvider.class.getName()));
    // make sure the filter is registered after the Spring Security Filter Chain
    filterRegistration.setOrder(101);
    filterRegistration.addUrlPatterns("/app/*", "/api/*");
    return filterRegistration;
  }
}
