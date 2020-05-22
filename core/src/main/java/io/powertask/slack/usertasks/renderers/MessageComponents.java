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
package io.powertask.slack.usertasks.renderers;

import static com.slack.api.model.block.Blocks.section;
import static com.slack.api.model.block.composition.BlockCompositions.markdownText;

import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.composition.MarkdownTextObject;
import com.slack.api.model.block.composition.TextObject;
import io.powertask.slack.TaskLike;
import io.powertask.slack.TaskService;
import io.powertask.slack.usertasks.Task;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MessageComponents {

  public static List<LayoutBlock> getErrorBlocks(Task task) {
    return task.errorMessage()
        .map(
            errorMessage ->
                Collections.<LayoutBlock>singletonList(
                    section(section -> section.text(markdownText("*" + errorMessage + "*")))))
        .orElseGet(Collections::emptyList);
  }

  public static List<LayoutBlock> getDescriptionBlocks(TaskLike taskLike) {
    return taskLike
        .description()
        .map(
            description ->
                Collections.<LayoutBlock>singletonList(
                    section(section -> section.text(markdownText(description)))))
        .orElseGet(Collections::emptyList);
  }

  public static List<LayoutBlock> getVariablesBlocks(TaskService taskService, Task task) {

    return task.showVariables()
        .map(
            variableNames -> {
              Map<String, Object> variables =
                  variableNames
                      .map(names -> taskService.getVariables(task.id(), new HashSet<>(names)))
                      .orElseGet(() -> taskService.getVariables(task.id()));

              // TODO, sort this to maintain the order of the variableNames!
              List<TextObject> fields =
                  variables.entrySet().stream()
                      .map(
                          entrySet ->
                              MarkdownTextObject.builder()
                                  .text("*" + entrySet.getKey() + ":*\n" + entrySet.getValue())
                                  .build())
                      .collect(Collectors.toList());

              if (fields.isEmpty()) {
                // TODO, or throw here?
                return Collections.<LayoutBlock>emptyList();
              } else {
                return Collections.<LayoutBlock>singletonList(
                    section(section -> section.fields(fields)));
              }
            })
        .orElseGet(Collections::emptyList);
  }
}
