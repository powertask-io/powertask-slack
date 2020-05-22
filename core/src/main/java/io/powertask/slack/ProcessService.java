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

import java.util.List;
import java.util.Map;

public interface ProcessService {
  List<Process> startableProcesses(String engineUserId);

  StartEvent startEvent(String processDefinitionId);

  // This still takes variables, for example for the 'initiator' of the process.
  void startProcess(String processDefinitionId, Map<String, Object> processVariables);

  void startProcessWithForm(String processDefinitionId, Map<String, Object> formVariables);
}
