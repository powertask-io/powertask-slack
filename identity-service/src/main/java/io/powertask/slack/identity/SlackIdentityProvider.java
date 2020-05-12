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

import com.slack.api.Slack;
import com.slack.api.methods.SlackApiResponse;
import com.slack.api.methods.response.users.UsersInfoResponse;
import org.camunda.bpm.engine.impl.identity.IdentityProviderException;
import org.camunda.bpm.engine.impl.identity.ReadOnlyIdentityProvider;
import org.camunda.bpm.engine.impl.interceptor.CommandContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

class SlackIdentityProvider implements ReadOnlyIdentityProvider {
    private final static Logger logger = LoggerFactory.getLogger(SlackIdentityProvider.class);

    private final Slack slack;

    SlackIdentityProvider(Slack slack) {
        this.slack = slack;
    }

    @Override
    public User findUserById(String userId) {
        return wrapExceptions(() -> {
            UsersInfoResponse response = okOrThrow(slack.methods().usersInfo(req -> req
                    .user(userId)));
            return SlackUser.from(response.getUser());
        });
    }

    @Override
    public UserQuery createUserQuery() {
        return null;
    }

    @Override
    public UserQuery createUserQuery(CommandContext commandContext) {
        return null;
    }

    @Override
    public NativeUserQuery createNativeUserQuery() {
        return null;
    }

    @Override
    public boolean checkPassword(String userId, String password) {
        return false;
    }

    @Override
    public Group findGroupById(String groupId) {
        return null;
    }

    @Override
    public GroupQuery createGroupQuery() {
        return null;
    }

    @Override
    public GroupQuery createGroupQuery(CommandContext commandContext) {
        return null;
    }

    @Override
    public Tenant findTenantById(String tenantId) {
        return null;
    }

    @Override
    public TenantQuery createTenantQuery() {
        return null;
    }

    @Override
    public TenantQuery createTenantQuery(CommandContext commandContext) {
        return null;
    }

    @Override
    public void flush() {

    }

    @Override
    public void close() {

    }

}
