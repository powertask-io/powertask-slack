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
package io.powertask.slack.examples.springboot;

import java.util.Collections;
import org.camunda.bpm.engine.ProcessEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ExampleApp implements CommandLineRunner {
  private static final Logger logger = LoggerFactory.getLogger(ExampleApp.class);

  @Value("${sample-process.email}")
  String email;

  private final ProcessEngine processEngine;

  public ExampleApp(ProcessEngine processEngine) {
    this.processEngine = processEngine;
  }

  public static void main(String... args) {
    SpringApplication.run(ExampleApp.class, args);
  }

  @Override
  public void run(String... args) {
    // Trigger a process :)
    logger.info("Triggering the movie-review process for Slack user with email:" + email);
    processEngine
        .getRuntimeService()
        .startProcessInstanceByKey(
            "movie-review-process", Collections.singletonMap("email", email));
  }
}
