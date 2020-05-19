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
package io.powertask.slack.modals.renderers;

import static com.slack.api.model.view.Views.view;
import static com.slack.api.model.view.Views.viewClose;
import static com.slack.api.model.view.Views.viewSubmit;
import static com.slack.api.model.view.Views.viewTitle;

import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.view.View;
import io.powertask.slack.FormLike;
import io.powertask.slack.FormLikePropertiesBase;
import io.powertask.slack.PropertiesResolver;
import io.powertask.slack.VariablesResolver;
import io.powertask.slack.modals.renderers.fieldrenderers.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.camunda.bpm.engine.form.FormData;
import org.camunda.bpm.engine.form.FormField;
import org.camunda.bpm.engine.impl.form.type.BooleanFormType;
import org.camunda.bpm.engine.impl.form.type.DateFormType;
import org.camunda.bpm.engine.impl.form.type.EnumFormType;
import org.camunda.bpm.engine.impl.form.type.LongFormType;
import org.camunda.bpm.engine.impl.form.type.StringFormType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModalRenderer<T extends FormLike> extends FormLikePropertiesBase<T> {
  private static final Logger logger = LoggerFactory.getLogger(ModalRenderer.class);

  private static final String PROPERTY_SLACK_TITLE = "slack-title";

  public ModalRenderer(
      PropertiesResolver<T> formLikePropertiesResolver, VariablesResolver<T> variablesResolver) {
    super(formLikePropertiesResolver, variablesResolver);
  }

  public boolean canRender(FormData formData) {
    return true; // TODO, YOLO
  }

  public View buildModal(T t, FormData formData, String callbackId) {
    // TODO, this can be max 25 characters.
    String titleValue = getProperty(t, PROPERTY_SLACK_TITLE).orElse(t.getName());

    List<LayoutBlock> blocks = new ArrayList<>();

    blocks.addAll(getErrorBlocks(t));
    blocks.addAll(getDescriptionBlocks(t));
    blocks.addAll(getVariablesBlocks(t));

    blocks.addAll(
        formData.getFormFields().stream()
            .map(field -> getRenderer(field).render(field))
            .collect(Collectors.toList()));

    return view(
        view ->
            view.callbackId(callbackId)
                .type("modal")
                .notifyOnClose(false)
                .title(viewTitle(title -> title.type("plain_text").text(titleValue).emoji(true)))
                .submit(viewSubmit(submit -> submit.type("plain_text").text("Submit").emoji(true)))
                .close(viewClose(close -> close.type("plain_text").text("Cancel").emoji(true)))
                .blocks(blocks));
  }

  public FieldRenderer getRenderer(FormField field) {
    if (field.getType() instanceof StringFormType) {
      return new StringFieldRenderer(field);
    } else if (field.getType() instanceof EnumFormType) {
      return new EnumFieldRenderer(field);
    } else if (field.getType() instanceof BooleanFormType) {
      return new BooleanFieldRenderer(field);
    } else if (field.getType() instanceof LongFormType) {
      return new LongFieldRenderer(field);
    } else if (field.getType() instanceof DateFormType) {
      return new DateFieldRenderer(field);
    } else {
      throw new RuntimeException("Missing implementation for field type " + field.getType());
    }
  }
}
