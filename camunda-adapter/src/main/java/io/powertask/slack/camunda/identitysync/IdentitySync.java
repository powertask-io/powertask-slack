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
import com.slack.api.model.Conversation;
import com.slack.api.model.ConversationType;
import com.slack.api.model.User;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.camunda.bpm.engine.IdentityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IdentitySync {

  private static final String CAMUNDA_ADMIN_GROUP = "camunda-admin";

  static class Group {
    public final String id;
    public final String name;

    Group(String id, String name) {
      this.id = id;
      this.name = name;
    }
  }

  static class Name {
    public final String firstName;
    public final String lastName;

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

  public IdentitySync(MethodsClient methodsClient, IdentityService identityService) {

    this.methodsClient = methodsClient;
    this.identityService = identityService;
  }

  public void sync() {
    logger.info("Starting identity sync...");
    List<String> admins = syncUsers();
    Set<Group> groups = syncGroups();
    syncGroupMemberships(groups, admins);
    logger.info("Identity sync completed.");
  }

  // TODO, Returns admins, strange, isn't it?
  private List<String> syncUsers() {
    List<User> allUsers = allUsers();
    List<User> admins = allUsers.stream().filter(User::isAdmin).collect(Collectors.toList());

    Map<String, org.camunda.bpm.engine.identity.User> existingUsers =
        identityService.createUserQuery().list().stream()
            .collect(
                Collectors.toMap(org.camunda.bpm.engine.identity.User::getId, Function.identity()));

    allUsers.forEach(
        user -> {
          if (!existingUsers.containsKey(user.getId())) {
            org.camunda.bpm.engine.identity.User newUser = identityService.newUser(user.getId());

            Name name = new Name(user.getRealName());
            newUser.setLastName(name.lastName);
            newUser.setFirstName(name.firstName);
            newUser.setEmail(user.getProfile().getEmail());
            logger.info(
                "User " + user.getId() + " (" + user.getProfile().getEmail() + ") inserted!");
            identityService.saveUser(newUser);
          } else {
            existingUsers.remove(user.getId());
          }
        });

    // Remove all remaining users
    existingUsers.forEach(
        (id, user) -> {
          logger.info("Deleting old user " + user.getFirstName());
          identityService.deleteUser(id);
        });

    return admins.stream().map(User::getId).collect(Collectors.toList());
  }

  private Set<Group> syncGroups() {
    Set<Group> allGroups = allGroups();

    Map<String, Group> existingGroups =
        identityService.createGroupQuery().list().stream()
            .map(slackGroup -> new Group(slackGroup.getId(), slackGroup.getName()))
            .collect(Collectors.toMap(g -> g.id, Function.identity()));

    allGroups.forEach(
        group -> {
          if (!existingGroups.containsKey(group.id)) {
            org.camunda.bpm.engine.identity.Group newGroup = identityService.newGroup(group.id);
            newGroup.setName(group.name);
            logger.info("Group " + group.name + " inserted!");
            identityService.saveGroup(newGroup);
          } else {
            existingGroups.remove(group.id);
          }
        });

    // Remove all remaining groupss
    existingGroups.forEach(
        (id, group) -> {
          logger.info("Deleting old group " + group.name);
          identityService.deleteGroup(id);
        });

    return allGroups;
  }

  /**
   * FIXME, this one is for 'groups as groups', the other one for 'channels as groups' private void
   * syncGroupMemberships(Set<Group> groups) { groups.forEach(group -> { Set<String> existingMembers
   * = identityService.createUserQuery().memberOfGroup(group.getId()).list() .stream()
   * .map(org.camunda.bpm.engine.identity.User::getId) .collect(Collectors.toSet());
   *
   * <p>UsergroupsUsersListResponse groupMembers = requireOk(() ->
   * methodsClient.usergroupsUsersList(req -> req.usergroup(group.getId())));
   * groupMembers.getUsers().forEach(userId -> { if(!existingMembers.contains(userId)) {
   * identityService.createMembership(userId, group.getId()); } else {
   * existingMembers.remove(userId); } });
   *
   * <p>existingMembers.forEach(userId -> identityService.deleteMembership(userId, group.getId()));
   * }); }
   */
  private void syncGroupMemberships(Set<Group> groups, List<String> admins) {
    groups.forEach(
        group -> {
          Set<String> existingMembers =
              identityService.createUserQuery().memberOfGroup(group.id).list().stream()
                  .map(org.camunda.bpm.engine.identity.User::getId)
                  .collect(Collectors.toSet());

          List<String> groupMembers;
          if (group.id.equals(CAMUNDA_ADMIN_GROUP)) {
            groupMembers = admins;
          } else {
            groupMembers =
                requireOk(() -> methodsClient.conversationsMembers(req -> req.channel(group.id)))
                    .getMembers();
          }

          groupMembers.forEach(
              userId -> {
                if (!existingMembers.contains(userId)) {
                  identityService.createMembership(userId, group.id);
                } else {
                  existingMembers.remove(userId);
                }
              });

          existingMembers.forEach(userId -> identityService.deleteMembership(userId, group.id));
        });
  }

  private List<User> allUsers() {
    List<User> allUsers = new ArrayList<>();
    String cursor = null;
    do {
      String c = cursor;
      UsersListResponse response =
          requireOk(() -> methodsClient.usersList(req -> req.cursor(c).limit(200)));
      allUsers.addAll(
          response.getMembers().stream()
              .filter(user -> !user.isBot())
              .collect(Collectors.toList()));
      cursor = response.getResponseMetadata().getNextCursor();
    } while (!cursor.equals(""));

    return allUsers;
  }

  private Set<Group> allGroups() {
    List<Conversation> conversations =
        requireOk(
                () ->
                    methodsClient.conversationsList(
                        req ->
                            req.types(
                                Arrays.asList(
                                    ConversationType.PRIVATE_CHANNEL,
                                    ConversationType.PUBLIC_CHANNEL))))
            .getChannels();

    Set<Group> groups =
        conversations.stream()
            .filter(conversation -> !conversation.isGeneral())
            .map(conversation -> new Group(conversation.getId(), conversation.getNameNormalized()))
            .collect(Collectors.toSet());

    groups.add(new Group(CAMUNDA_ADMIN_GROUP, "Camunda Admins"));
    return groups;
  }

  /*
  FIXME, make it configurable whether to use user groups or channel membership as groups!
   user groups are only available in paid plans.
  private List<Usergroup> allGroups() {
    return requireOk(() -> methodsClient.usergroupsList(req -> req)).getUsergroups();
  }
   */

}
