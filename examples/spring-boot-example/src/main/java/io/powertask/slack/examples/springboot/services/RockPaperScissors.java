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
package io.powertask.slack.examples.springboot.services;

import io.powertask.slack.servicetasks.SlackService;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.engine.variable.value.StringValue;
import org.springframework.stereotype.Component;

@Component
public class RockPaperScissors {

  private final SlackService slackService;

  private final String DRAW_ERROR = "DRAW_ERROR";

  public RockPaperScissors(SlackService slackService) {
    this.slackService = slackService;
  }

  public JavaDelegate decide() {
    return execution -> {
      String challengerWeapon =
          execution.<StringValue>getVariableTyped("challenger-weapon").getValue();
      String opponentWeapon = execution.<StringValue>getVariableTyped("opponent-weapon").getValue();

      if (challengerWeapon.equals(opponentWeapon)) {
        throw new BpmnError(
            DRAW_ERROR,
            "Both players chose " + challengerWeapon + " so you'll have to pick again!");
      } else if (beats(challengerWeapon, opponentWeapon)) {
        execution.setVariable("winner", execution.getVariable("challenger"));
        execution.setVariable("loser", execution.getVariable("opponent"));
      } else {
        execution.setVariable("loser", execution.getVariable("challenger"));
        execution.setVariable("winner", execution.getVariable("opponent"));
      }
    };
  }

  public JavaDelegate announce() {
    return execution -> {
      String winnerId = execution.<StringValue>getVariableTyped("winner").getValue();
      String loserId = execution.<StringValue>getVariableTyped("loser").getValue();

      slackService.postMessage(winnerId, "Woohoo! You HAVE WON!");
      slackService.postMessage(loserId, "Boohoo! You HAVE LOST!");
    };
  }

  private static boolean beats(String weapon, String other) {
    return (weapon.equals("rock") && other.equals("scissors"))
        || (weapon.equals("paper") && other.equals("rock"))
        || (weapon.equals("scissors") && other.equals("paper"));
  }
}
