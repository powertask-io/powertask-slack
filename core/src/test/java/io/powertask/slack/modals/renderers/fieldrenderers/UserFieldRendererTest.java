package io.powertask.slack.modals.renderers.fieldrenderers;

import static com.slack.api.model.block.composition.BlockCompositions.plainText;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.slack.api.model.block.element.BlockElement;
import com.slack.api.model.block.element.UsersSelectElement;
import com.slack.api.model.view.ViewState;
import com.slack.api.model.view.ViewState.Value;
import io.powertask.slack.formfields.ImmutableUserField;
import io.powertask.slack.formfields.UserField;
import io.vavr.control.Either;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class UserFieldRendererTest {

  private ImmutableUserField.Builder getBaseField(String id) {
    return ImmutableUserField.builder().id(id).label(id).required(true);
  }

  @Test
  void renderBasicElement() {
    String id = "1234";

    UserField formField = getBaseField(id).build();

    BlockElement renderedElement =
        new io.powertask.slack.modals.renderers.fieldrenderers.UserFieldRenderer(formField)
            .renderElement();

    BlockElement expectedElement =
        UsersSelectElement.builder().actionId(id + "_user").build();

    assertEquals(expectedElement, renderedElement);
  }

  @Test
  void renderFullOptionsElement() {
    String id = "1234";
    String value = "123ABC123";
    String placeholder = "Fill it out...";

    UserField formField =
        getBaseField(id)
            .value(value)
            .placeholder(placeholder)
            .build();

    BlockElement renderedElement = new UserFieldRenderer(formField).renderElement();

    BlockElement expectedElement =
        UsersSelectElement.builder()
            .initialUser(value)
            .placeholder(plainText(placeholder))
            .actionId(id + "_user")
            .build();

    assertEquals(expectedElement, renderedElement);
  }

  @Test
  void extractValue() {
    String id = "1234";
    UserField formField = getBaseField(id).build();

    ViewState.Value value = new ViewState.Value();
    value.setSelectedUser("123ABC123");

    Map<String, Value> fields = Collections.singletonMap("1234_user", value);

    assertEquals(
        Either.right(Optional.of("123ABC123")),
        new UserFieldRenderer(formField).extractValue(fields));
  }
}
