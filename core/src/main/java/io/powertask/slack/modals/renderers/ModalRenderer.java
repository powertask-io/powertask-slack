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
import io.powertask.slack.Form;
import io.powertask.slack.FormField;
import io.powertask.slack.TaskLike;
import io.powertask.slack.TaskService;
import io.powertask.slack.formfields.BooleanField;
import io.powertask.slack.formfields.DateField;
import io.powertask.slack.formfields.EnumField;
import io.powertask.slack.formfields.LongField;
import io.powertask.slack.formfields.StringField;
import io.powertask.slack.modals.renderers.fieldrenderers.*;
import io.powertask.slack.usertasks.Task;
import io.powertask.slack.usertasks.renderers.MessageComponents;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ModalRenderer {

  private final TaskService taskService;

  public ModalRenderer(TaskService taskService) {
    this.taskService = taskService;
  }

  public boolean canRender(Form form) {
    return true; // TODO, YOLO
  }

  public View buildModal(TaskLike taskLike, Form form, String callbackId) {

    List<LayoutBlock> blocks = new ArrayList<>();

    if (taskLike instanceof Task) {
      blocks.addAll(MessageComponents.getErrorBlocks((Task) taskLike));
    }
    blocks.addAll(MessageComponents.getDescriptionBlocks(taskLike));
    if (taskLike instanceof Task) {
      blocks.addAll(MessageComponents.getVariablesBlocks(taskService, (Task) taskLike));
    }

    blocks.addAll(
        form.fields().stream()
            .map(field -> getRenderer(field).render())
            .collect(Collectors.toList()));

    return view(
        view ->
            view.callbackId(callbackId)
                .type("modal")
                .notifyOnClose(false)
                .title(
                    viewTitle(title -> title.type("plain_text").text(taskLike.title()).emoji(true)))
                .submit(viewSubmit(submit -> submit.type("plain_text").text("Submit").emoji(true)))
                .close(viewClose(close -> close.type("plain_text").text("Cancel").emoji(true)))
                .blocks(blocks));
  }

  public FieldRenderer getRenderer(FormField<?> field) {
    if (field instanceof StringField) {
      return new StringFieldRenderer((StringField) field);
    } else if (field instanceof EnumField) {
      return new RadioFieldRenderer((EnumField) field);
    } else if (field instanceof BooleanField) {
      return new BooleanFieldRenderer((BooleanField) field);
    } else if (field instanceof LongField) {
      return new LongFieldRenderer((LongField) field);
    } else if (field instanceof DateField) {
      return new DateFieldRenderer((DateField) field);
    } else {
      throw new RuntimeException("Missing implementation for field type " + field.getClass());
    }
  }
}
