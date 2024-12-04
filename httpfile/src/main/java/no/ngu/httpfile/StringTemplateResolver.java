package no.ngu.httpfile;

import java.io.IOException;
import java.util.function.BiConsumer;
import no.ngu.httpfile.HttpFile.Model;
import no.ngu.httpfile.HttpFile.StringTemplate.Part;

public class StringTemplateResolver {

  private StringValueProvider stringValueProvider;
  private InputStreamProvider inputStreamProvider;
  private MacroValueProvider macroValueProvider;

  public StringTemplateResolver() {}

  public StringTemplateResolver(StringValueProvider stringValueProvider,
      InputStreamProvider inputStreamProvider) {
    this.stringValueProvider = stringValueProvider;
    this.inputStreamProvider = inputStreamProvider;
    this.macroValueProvider = new MacroValueProvider(inputStreamProvider);
  }

  public void setStringValueProvider(StringValueProvider stringValueProvider) {
    this.stringValueProvider = stringValueProvider;
  }

  public void setInputStreamProvider(InputStreamProvider inputStreamProvider) {
    this.inputStreamProvider = inputStreamProvider;
    this.macroValueProvider = new MacroValueProvider(inputStreamProvider);
  }

  public void forEach(HttpFile.StringTemplate stringTemplate,
      BiConsumer<Integer, String> consumer) {
    for (int index = 0; index < stringTemplate.parts().size(); index++) {
      var s = switch (stringTemplate.parts().get(index)) {
        case Part.Constant constant -> constant.value();
        case Part.VariableRef(var name) -> stringValueProvider.getStringValue(name);
        case Part.MacroCall(var macro, var args) -> {
          try {
            yield macroValueProvider.applyMacro(macro, args);
          } catch (Exception e) {
            yield e.getMessage();
          }
        }
        case Part.ResourceRef resourceRef -> {
          try (var inputStream = inputStreamProvider.getInputStream(resourceRef.resource())) {
            if (inputStream == null) {
              yield "Resource '" + resourceRef.resource() + "' not found";
            } else {
              yield new String(inputStream.readAllBytes());
            }
          } catch (IOException e) {
            yield e.getMessage();
          }
        }
        // shouldn't need this one, it's already exhaustive
        default -> null;
      };
      consumer.accept(index, s);
    }
  }

  public void toStringBuffer(HttpFile.StringTemplate stringTemplate, StringBuffer buffer) {
    forEach(stringTemplate, (i, s) -> {
      if (s != null) {
        buffer.append(s);
      }
    });
  }

  public String toString(HttpFile.StringTemplate stringTemplate) {
    StringBuffer buffer = new StringBuffer();
    toStringBuffer(stringTemplate, buffer);
    return buffer.toString();
  }

  public void resolve(HttpFile.StringTemplate stringTemplate) {
    forEach(stringTemplate, (i, s) -> stringTemplate.parts().set(i, new Part.Constant(s)));
  }

  public void resolve(Model model) {
    for (var request : model.requests()) {
      resolve(request.target());
      for (var header : request.headers()) {
        resolve(header.value());
      }
      if (request.body() != null) {
        resolve(request.body().content());
      }
    }
  }
}
