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

import static com.slack.api.model.block.Blocks.section;
import static com.slack.api.model.block.composition.BlockCompositions.markdownText;

import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.composition.MarkdownTextObject;
import com.slack.api.model.block.composition.TextObject;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO, get rid of this class, just use the resolver directly.
public abstract class FormLikePropertiesBase<T extends FormLike> {
  protected static final Logger logger = LoggerFactory.getLogger(FormLikePropertiesBase.class);

  protected static final String PROPERTY_SLACK_DESCRIPTION = "slack-description";
  protected static final String PROPERTY_SLACK_SHOW_VARIABLES = "slack-show-variables";

  protected final PropertiesResolver<T> propertiesResolver;
  protected final VariablesResolver<T> variablesResolver;

  protected FormLikePropertiesBase(
      PropertiesResolver<T> formLikePropertiesResolver, VariablesResolver<T> variablesResolver) {
    this.propertiesResolver = formLikePropertiesResolver;
    this.variablesResolver = variablesResolver;
  }

  protected boolean hasProperty(T t, String name) {
    return getPropertyWithOptionalValue(t, name).isPresent();
  }

  protected Optional<String> getProperty(T t, String name) {
    return getPropertyWithOptionalValue(t, name).flatMap(Function.identity());
  }

  // TODO, this is likely slow. How to best cache this?
  protected Optional<Optional<String>> getPropertyWithOptionalValue(T t, String name) {
    return Optional.ofNullable(propertiesResolver.getProperties(t).get(name));
  }

  protected List<LayoutBlock> getErrorBlocks(T t) {
    return variablesResolver
        .getVariable(t, "task-error-message")
        .map(
            errorMessage ->
                Collections.<LayoutBlock>singletonList(
                    section(section -> section.text(markdownText("*" + errorMessage + "*")))))
        .orElseGet(Collections::emptyList);
  }

  protected List<LayoutBlock> getDescriptionBlocks(T t) {
    return getProperty(t, PROPERTY_SLACK_DESCRIPTION)
        .map(
            description ->
                Collections.<LayoutBlock>singletonList(
                    section(section -> section.text(markdownText(description)))))
        .orElseGet(Collections::emptyList);
  }

  protected List<LayoutBlock> getVariablesBlocks(T t) {
    if (hasProperty(t, PROPERTY_SLACK_SHOW_VARIABLES)) {
      Optional<String> requestedVariables = getProperty(t, PROPERTY_SLACK_SHOW_VARIABLES);

      Map<String, Object> variables =
          requestedVariables
              .map(
                  variablesToShow -> {
                    Set<String> variableNames =
                        Arrays.stream(variablesToShow.split(","))
                            .map(String::trim)
                            .collect(Collectors.toSet());
                    return variablesResolver.getVariables(t, variableNames);
                  })
              .orElseGet(() -> variablesResolver.getVariables(t));

      List<TextObject> fields =
          variables.entrySet().stream()
              .map(
                  entrySet ->
                      MarkdownTextObject.builder()
                          .text("*" + entrySet.getKey() + ":*\n" + entrySet.getValue())
                          .build())
              .collect(Collectors.toList());

      if (fields.isEmpty()) {
        // When I'm nice, I'm really nice.
        if (requestedVariables.equals(Optional.of("true"))) {
          logger.warn(
              "Property "
                  + PROPERTY_SLACK_SHOW_VARIABLES
                  + " is set to 'true', this is unlikely to be what you want, since there's no variable with that name in the process. Leave it empty to show all variables, or use a comma-separated list of variable names to display.");
        } else {
          logger.warn(
              "Property "
                  + PROPERTY_SLACK_SHOW_VARIABLES
                  + " is set for object "
                  + t.getName()
                  + ", but no variables found!");
        }
        return Collections.emptyList();
      } else {
        return Collections.singletonList(section(section -> section.fields(fields)));
      }
    } else {
      return Collections.emptyList();
    }
  }
}
