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
package io.powertask.slack.apphome;

import static com.slack.api.model.view.Views.view;

import com.slack.api.bolt.App;
import com.slack.api.methods.response.views.ViewsPublishResponse;
import com.slack.api.model.event.AppHomeOpenedEvent;
import com.slack.api.model.view.View;
import java.util.Optional;
import org.immutables.value.Value;

// First step: Process types
// - Process without a form, just show feedback that the form was started. And maybe for who the
// next action is?
// - Process with a form, show the form.

// Next step:
// - Check for any tasks in the process.

public class AppHome {

  private final App app;
  private final ProcessDispatcher processDispatcher;

  @Value.Immutable
  interface Process {
    String name();

    String id();

    Optional<String> description();
  }

  public AppHome(App app, ProcessDispatcher processDispatcher) {
    this.app = app;
    this.processDispatcher = processDispatcher;
    subscribeAppHomeOpenedEvent();
  }

  private void subscribeAppHomeOpenedEvent() {
    app.event(
        AppHomeOpenedEvent.class,
        (payload, ctx) -> {
          String slackUserId = payload.getEvent().getUser();
          // Build a Home tab view
          View appHomeView =
              view(view -> view.type("home").blocks(processDispatcher.processList(slackUserId)));
          // Update the App Home for the given user
          ViewsPublishResponse res =
              ctx.client()
                  .viewsPublish(
                      r ->
                          r.userId(slackUserId)
                              // TODO, getView() is (sometimes) null, figure out why.
                              // .hash(payload.getEvent().getView().getHash()) // To protect against
                              // possible race conditions
                              .view(appHomeView));
          return ctx.ack();
        });
  }
}
