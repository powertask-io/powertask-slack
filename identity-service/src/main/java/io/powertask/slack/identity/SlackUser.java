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
package io.powertask.slack.identity;

import org.camunda.bpm.engine.identity.User;

import java.util.Arrays;
import java.util.List;

public class SlackUser implements User {

    private final String id;
    private final String firstName;
    private final String lastName;
    private final String email;

    SlackUser(String id, String firstName,  String lastName, String email) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getFirstName() {
        return null;
    }

    @Override
    public void setFirstName(String firstName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setLastName(String lastName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getLastName() {
        return lastName;
    }

    @Override
    public void setEmail(String email) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public String getPassword() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setPassword(String password) {
        throw new UnsupportedOperationException();
    }

    static SlackUser from(com.slack.api.model.User slackApiUser) {
        List<String> nameParts = Arrays.asList(slackApiUser.getRealName().split(" ", 2));
        return new SlackUser(
                slackApiUser.getId(),
                nameParts.get(0),
                nameParts.size() == 2 ? nameParts.get(1) : "",
                slackApiUser.getProfile().getEmail());
    }
}
