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
package io.powertask.slack.camunda.identitysync;

import static io.powertask.slack.SlackApiOps.requireOk;

import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.response.users.UsersListResponse;
import com.slack.api.model.ConversationType;
import com.slack.api.model.User;
import io.powertask.slack.camunda.identitysync.IdentitySyncConfiguration.AdminMode;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Data;
import org.camunda.bpm.engine.IdentityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IdentitySync {

  private static final String CAMUNDA_ADMIN_GROUP = "camunda-admin";

  private final IdentitySyncConfiguration configuration;
  private final ScheduledExecutorService executorService;

  private ScheduledFuture<?> scheduledFuture;

  @Data
  static class Group {
    private final String id;
    private final String name;
  }

  @Data
  static class Name {
    private final String firstName;
    private final String lastName;

    // Okay, we know we can never do this right. But Camunda shows 'null' in the
    // UI if we don't set either first or last name, so we have to attempt anyway.
    public Name(String fullName) {
      String[] parts = fullName.split(" ", 2);
      if (parts.length == 2) {
        firstName = parts[0];
        lastName = parts[1];
      } else {
        firstName = " ";
        lastName = fullName;
      }
    }
  }

  private static final Logger logger = LoggerFactory.getLogger(IdentitySync.class);

  private final MethodsClient methodsClient;
  private final IdentityService identityService;

  public IdentitySync(
      IdentitySyncConfiguration configuration,
      MethodsClient methodsClient,
      IdentityService identityService) {
    this(Executors.newScheduledThreadPool(1), configuration, methodsClient, identityService);
  }

  public IdentitySync(
      ScheduledExecutorService executorService,
      IdentitySyncConfiguration configuration,
      MethodsClient methodsClient,
      IdentityService identityService) {

    this.executorService = executorService;
    this.configuration = configuration;
    this.methodsClient = methodsClient;
    this.identityService = identityService;
  }

  public synchronized void start() {
    if (scheduledFuture != null) {
      throw new IllegalStateException("Sync already started!");
    }

    // We want the first sync to be synchronous
    sync();
    scheduledFuture =
        executorService.scheduleWithFixedDelay(
            this::sync,
            configuration.getSyncInterval().getSeconds(),
            configuration.getSyncInterval().getSeconds(),
            TimeUnit.SECONDS);
  }

  public synchronized boolean stop() {
    boolean out = scheduledFuture.cancel(false);
    scheduledFuture = null;
    return out;
  }

  private void sync() {
    logger.info(
        "Starting identity sync, with group mode = "
            + configuration.getGroupMode()
            + " and admin mode = "
            + configuration.getAdminMode()
            + ", synching every "
            + configuration.getSyncInterval());

    if (configuration.getAdminMode() == AdminMode.GROUP) {
      logger.info(
          "Slack group " + configuration.getAdminGroupId() + " is the configured admin group.");
    }

    Set<User> allUsers = allUsers();
    syncUsers(allUsers);

    Set<String> adminIds = getCamundaAdminUserIds(allUsers);

    Set<Group> groups = allGroups();
    groups.add(new Group(CAMUNDA_ADMIN_GROUP, "Camunda Admins"));
    syncGroups(groups);

    syncGroupMemberships(groups, adminIds);
    logger.info("Identity sync completed.");
  }

  private Set<String> getCamundaAdminUserIds(Set<User> allUsers) {
    switch (configuration.getAdminMode()) {
      case GROUP:
        return getGroupMembers(
            Objects.requireNonNull(
                configuration.getAdminGroupId(),
                "adminGroupId must not be null if identity sync group mode is Group."));
      case SLACK_ADMIN:
        return allUsers.stream().filter(User::isAdmin).map(User::getId).collect(Collectors.toSet());
    }
    throw new RuntimeException("Unexpected Admin Mode!");
  }

  private void syncUsers(Set<User> allUsers) {

    Map<String, org.camunda.bpm.engine.identity.User> existingUsers =
        identityService.createUserQuery().list().stream()
            .collect(
                Collectors.toMap(org.camunda.bpm.engine.identity.User::getId, Function.identity()));

    allUsers.forEach(
        user -> {
          // Add the user if it doesn't exist
          if (!existingUsers.containsKey(user.getId())) {
            org.camunda.bpm.engine.identity.User newUser = identityService.newUser(user.getId());

            Name name = new Name(user.getRealName());
            newUser.setLastName(name.lastName);
            newUser.setFirstName(name.firstName);
            newUser.setEmail(user.getProfile().getEmail());
            logger.info(
                "New user " + user.getId() + " (" + user.getProfile().getEmail() + ") inserted.");
            identityService.saveUser(newUser);
            // Keep some administration of which users in the DB we've also seen in Slack.
          } else {
            logger.debug(
                "User " + user.getId() + "(" + user.getProfile().getEmail() + ") unchanged.");
            existingUsers.remove(user.getId());
          }
        });

    // By now, all users in 'existingUsers', are users that are in the database,
    // but no longer in Slack. So we remove them.
    existingUsers.forEach(
        (id, user) -> {
          logger.info("Old user " + user.getId() + " (" + user.getEmail() + ") deleted.");
          identityService.deleteUser(id);
        });
  }

  private void syncGroups(Set<Group> allGroups) {

    Map<String, Group> existingGroups =
        identityService.createGroupQuery().list().stream()
            .map(slackGroup -> new Group(slackGroup.getId(), slackGroup.getName()))
            .collect(Collectors.toMap(g -> g.id, Function.identity()));

    allGroups.forEach(
        group -> {
          if (!existingGroups.containsKey(group.id)) {
            org.camunda.bpm.engine.identity.Group newGroup = identityService.newGroup(group.id);
            newGroup.setName(group.name);
            newGroup.setType("Synced from Slack");
            logger.info("New group " + group.id + " (" + group.name + ") inserted!");
            identityService.saveGroup(newGroup);
          } else {
            logger.debug("Group " + group.id + " (" + group.name + ") unchanged.");
            existingGroups.remove(group.id);
          }
        });

    // Remove all remaining groupss
    existingGroups.forEach(
        (id, group) -> {
          logger.info("Old group " + group.getId() + "(" + group.getName() + ") deleted.");
          identityService.deleteGroup(id);
        });
  }

  private void syncGroupMemberships(Set<Group> groups, Set<String> admins) {
    groups.forEach(
        group -> {
          Set<String> existingMembers =
              identityService.createUserQuery().memberOfGroup(group.id).list().stream()
                  .map(org.camunda.bpm.engine.identity.User::getId)
                  .collect(Collectors.toSet());

          Set<String> groupMembers;
          if (group.id.equals(CAMUNDA_ADMIN_GROUP)) {
            groupMembers = admins;
          } else {
            groupMembers = getGroupMembers(group.id);
          }

          groupMembers.forEach(
              userId -> {
                if (!existingMembers.contains(userId)) {
                  logger.info("Adding user " + userId + " to group " + group.id);
                  identityService.createMembership(userId, group.id);
                } else {
                  existingMembers.remove(userId);
                }
              });

          existingMembers.forEach(
              userId -> {
                logger.info("Removing user " + userId + " from group " + group.id);
                identityService.deleteMembership(userId, group.id);
              });
        });
  }

  private Set<String> getGroupMembers(String groupId) {
    switch (configuration.getGroupMode()) {
      case GROUPS:
        return new HashSet<>(
            requireOk(() -> methodsClient.usergroupsUsersList(req -> req.usergroup(groupId)))
                .getUsers());
      case CHANNELS:
        return new HashSet<>(
            requireOk(() -> methodsClient.conversationsMembers(req -> req.channel(groupId)))
                .getMembers());
    }
    throw new RuntimeException("Unexpected Group Mode!");
  }

  private Set<User> allUsers() {
    Set<User> allUsers = new HashSet<>();
    String cursor = null;
    do {
      String c = cursor;
      UsersListResponse response =
          requireOk(() -> methodsClient.usersList(req -> req.cursor(c).limit(200)));
      allUsers.addAll(
          response.getMembers().stream()
              .filter(user -> !user.isBot())
              .filter(
                  user ->
                      !user.getName()
                          .equals(
                              "slackbot")) // For some reason slackbot doesn't have the isBot bit
              // set.
              .collect(Collectors.toSet()));
      cursor = response.getResponseMetadata().getNextCursor();
    } while (!cursor.equals(""));

    return allUsers;
  }

  private Set<Group> allGroups() {

    Set<Group> groups;

    switch (configuration.getGroupMode()) {
      case CHANNELS:
        groups = allSlackChannelsAsGroups();
        break;
      case GROUPS:
        groups = allSlackGroupsAsGroups();
        break;
      default:
        throw new RuntimeException("Unexpected Group Mode");
    }

    groups.add(new Group(CAMUNDA_ADMIN_GROUP, "Camunda Admins"));
    return groups;
  }

  private Set<Group> allSlackChannelsAsGroups() {
    return requireOk(
            () ->
                methodsClient.conversationsList(
                    req ->
                        req.types(
                            Arrays.asList(
                                ConversationType.PRIVATE_CHANNEL,
                                ConversationType.PUBLIC_CHANNEL))))
        .getChannels().stream()
        .filter(conversation -> !conversation.isGeneral())
        .map(conversation -> new Group(conversation.getId(), conversation.getNameNormalized()))
        .collect(Collectors.toSet());
  }

  private Set<Group> allSlackGroupsAsGroups() {
    return requireOk(() -> methodsClient.usergroupsList(req -> req)).getUsergroups().stream()
        .map(group -> new Group(group.getId(), group.getName()))
        .collect(Collectors.toSet());
  }
}
