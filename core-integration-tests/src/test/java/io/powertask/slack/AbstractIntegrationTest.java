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
package io.powertask.slack;

import static io.powertask.slack.FunctionOps.wrapExceptions;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.slack.api.Slack;
import com.slack.api.SlackConfig;
import com.slack.api.app_backend.SlackSignature;
import com.slack.api.app_backend.SlackSignature.Generator;
import com.slack.api.app_backend.SlackSignature.HeaderNames;
import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.bolt.request.Request;
import com.slack.api.bolt.request.RequestHeaders;
import com.slack.api.bolt.response.Response;
import com.slack.api.bolt.util.SlackRequestParser;
import com.slack.api.bolt.util.SlackRequestParser.HttpRequest;
import com.slack.api.methods.AsyncMethodsClient;
import com.slack.api.methods.MethodsClient;
import io.powertask.slack.camunda.CamundaFormService;
import io.powertask.slack.camunda.CamundaTaskService;
import io.powertask.slack.camunda.PropertiesResolver;
import io.powertask.slack.camunda.TaskMapper;
import io.powertask.slack.camunda.plugin.TaskListenerPlugin;
import io.powertask.slack.camunda.plugin.UserTaskDispatcherListener;
import io.powertask.slack.identity.EmailUserResolver;
import io.powertask.slack.identity.UserResolver;
import io.powertask.slack.usertasks.UserTaskDispatcher;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import rawhttp.core.RawHttp;
import rawhttp.core.RawHttpHeaders;
import rawhttp.core.RawHttpRequest;
import rawhttp.core.body.StringBody;

public abstract class AbstractIntegrationTest {

  private final String wiremockFiles;

  protected static final String SIGNING_SECRET = "8daf55e89e3cc43824510b198127ee38";
  protected static final String TOKEN = "xoxb-fake-token";

  protected ProcessEngine processEngine;
  protected UserTaskDispatcher userTaskDispatcher;
  protected UserTaskDispatcherListener powertaskListener;
  protected WireMockServer wireMockServer;
  protected App app;
  protected SlackRequestParser requestParser;
  protected Map<Object, Object> beans = new HashMap<>();

  protected AbstractIntegrationTest(String wiremockFiles) {
    this.wiremockFiles = wiremockFiles;
  }

  @BeforeEach
  void setup() {
    setupWireMock();
    setupProcessEngine();
    setupUserTaskDispatcher();
    addTaskDispatcherToProcessEngine();
  }

  @AfterEach
  void teardown() {
    app.stop();
    wireMockServer.shutdown();
    processEngine.close();
  }

  private void setupWireMock() {
    WireMockConfiguration conf =
        WireMockConfiguration.options().usingFilesUnderClasspath(wiremockFiles).dynamicPort();
    wireMockServer = new WireMockServer(conf);
    wireMockServer.start();
  }

  private void setupProcessEngine() {
    ProcessEngineConfigurationImpl configuration = new StandaloneInMemProcessEngineConfiguration();
    configuration.setProcessEnginePlugins(Collections.singletonList(new TaskListenerPlugin()));
    configuration.setBeans(beans);
    processEngine = configuration.buildProcessEngine();
  }

  private void setupUserTaskDispatcher() {
    SlackConfig config = new SlackConfig();
    config.setMethodsEndpointUrlPrefix("http://localhost:" + wireMockServer.port() + "/");
    Slack slack = Slack.getInstance(config);
    AsyncMethodsClient asyncMethodsClient = slack.methodsAsync(TOKEN);
    MethodsClient methodsClient = slack.methods(TOKEN);
    AppConfig appConfig =
        AppConfig.builder()
            .singleTeamBotToken(TOKEN)
            .slack(slack)
            .signingSecret(SIGNING_SECRET)
            .build();
    app = new App(appConfig);
    requestParser = new SlackRequestParser(appConfig);
    UserResolver userResolver = new EmailUserResolver(methodsClient);

    PropertiesResolver propertiesResolver =
        new PropertiesResolver(processEngine.getRepositoryService());
    TaskMapper taskMapper = new TaskMapper(propertiesResolver, processEngine.getRuntimeService());
    TaskService taskService = new CamundaTaskService(processEngine.getTaskService(), taskMapper);
    FormService formService = new CamundaFormService(processEngine.getFormService());

    userTaskDispatcher =
        new UserTaskDispatcher(asyncMethodsClient, userResolver, app, taskService, formService);
    powertaskListener = new UserTaskDispatcherListener(taskMapper, userTaskDispatcher);
  }

  // TODO, ugly cyclic dep :'(
  private void addTaskDispatcherToProcessEngine() {
    beans.put("powertaskListener", powertaskListener);
  }

  protected Response slackInteraction(String name, Map<String, String> variables) {
    try (InputStream is =
        getClass().getClassLoader().getResourceAsStream(wiremockFiles + "/incoming/" + name)) {
      RawHttpRequest request = new RawHttp().parseRequest(is).eagerly();
      RawHttpRequest updated = replaceBodyVariables(request, variables);
      RawHttpRequest resigned = resign(updated);
      Request<?> req = requestParser.parse(toSlackHttpRequest(resigned));
      return app.run(req);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private byte[] getRequestBody(RawHttpRequest request) {
    return wrapExceptions(() -> request.getBody().get().asRawBytes());
  }

  private RawHttpRequest replaceBodyVariables(
      RawHttpRequest request, Map<String, String> variables) {
    return wrapExceptions(
        () -> {
          String jsonBody =
              URLDecoder.decode(
                  new String(getRequestBody(request), StandardCharsets.UTF_8)
                      .substring("payload=".length()),
                  StandardCharsets.UTF_8.toString());

          DocumentContext documentContext = JsonPath.parse(jsonBody);
          variables.forEach(documentContext::set);

          String requestBody =
              "payload="
                  + URLEncoder.encode(
                      documentContext.jsonString(), StandardCharsets.UTF_8.toString());
          return request.withBody(new StringBody(requestBody)).eagerly();
        });
  }

  private RawHttpRequest resign(RawHttpRequest req) {
    String newTimestamp = Long.valueOf(System.currentTimeMillis()).toString();
    Generator generator = new SlackSignature.Generator(app.config().getSigningSecret());
    String signature =
        generator.generate(newTimestamp, new String(getRequestBody(req), StandardCharsets.UTF_8));

    return req.withHeaders(
        RawHttpHeaders.newBuilder()
            .with(HeaderNames.X_SLACK_REQUEST_TIMESTAMP, newTimestamp)
            .with(HeaderNames.X_SLACK_SIGNATURE, signature)
            .build());
  }

  private HttpRequest toSlackHttpRequest(RawHttpRequest request) {
    return HttpRequest.builder()
        .requestUri(request.getUri().getPath())
        .headers(new RequestHeaders(request.getHeaders().asMap()))
        .requestBody(new String(getRequestBody(request), StandardCharsets.UTF_8))
        .queryString(
            URLEncodedUtils.parse(request.getUri(), StandardCharsets.UTF_8).stream()
                .collect(
                    Collectors.toMap(
                        NameValuePair::getName,
                        pair -> Collections.singletonList(pair.getValue()),
                        (list1, list2) -> {
                          ArrayList<String> list = new ArrayList<>(list1.size() + list2.size());
                          list.addAll(list1);
                          list.addAll(list2);
                          return list;
                        })))
        .remoteAddress(request.getSenderAddress().map(InetAddress::toString).orElse(null))
        .build();
  }

  protected void deploy(String classPathResource) {
    processEngine
        .getRepositoryService()
        .createDeployment()
        .addClasspathResource(classPathResource)
        .deploy();
  }
}
