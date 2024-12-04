package no.ngu.httpfile.data;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

public class JsonbDataTraverser implements DataTraverser {

  private Jsonb jsonb = null;

  private Jsonb getJsonb() {
    if (jsonb == null) {
      jsonb = JsonbBuilder.create();
    }
    return jsonb;
  }

  @Override
  public boolean traverses(Object data, String step) {
    return switch (data) {
      case JsonStructure json -> true;
      case String s -> "json".equals(step) || "$".equals(step);
      default -> false;
    };
  }

  @Override
  public Object traverse(Object data, String step) throws IllegalArgumentException {
    return switch (data) {
      case JsonObject jsonObject -> jsonObject.get(step);
      case JsonArray jsonArray -> jsonArray.get(DataTraverser.checkIndexStep(step, jsonArray.size()));
      case String string -> switch (step) {
        case "json", "$" -> getJsonb().fromJson(string, JsonValue.class);
        default -> DataTraverser.throwIllegalStep(data, step);
      };
      default -> DataTraverser.throwIllegalStep(data, step);
    };
  }

  @Override
  public boolean converts(Object data) {
    // strip the quotes
    return data instanceof JsonString;
  }
  
  @Override
  public String asString(Object data) {
    return switch (data) {
      // strip the quotes
      case JsonString js -> js.getString();
      default -> DataTraverser.super.asString(data);
    };
  }
}
