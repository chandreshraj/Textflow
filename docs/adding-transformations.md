# Adding a transformation

Add one Java class in `backend/transformation-core/src/main/java/com/knime/textflow/core/transformations/`. Implement `Transformation` and annotate it with `@AutoService(Transformation.class)`:

```java
package com.knime.textflow.core.transformations;

import com.google.auto.service.AutoService;
import com.knime.textflow.core.Transformation;
import java.util.List;
import java.util.Map;
import static com.knime.textflow.core.Parameters.text;

@AutoService(Transformation.class)
public final class SurroundTransformation implements Transformation {
    @Override
    public Descriptor descriptor() {
        return new Descriptor("surround", "Surround", "Wrap text with a prefix and suffix.",
                List.of(text("prefix", "Prefix", "["), text("suffix", "Suffix", "]")));
    }

    @Override
    public String apply(String input, Map<String, Object> parameters) {
        return parameters.get("prefix") + input + parameters.get("suffix");
    }
}
```

Then rebuild with `./scripts/build.sh`, restart the executable JAR, and reload the browser. “Surround” appears in the library with two fields; `Hello` becomes `[Hello]` with the defaults. Add focused tests for the operation's semantics and invalid combinations.

There is no registry list to edit. The configured AutoService annotation processor generates a Java SPI service file during compilation. The API obtains providers through `TransformationDiscovery.load()`; frontend fields come from descriptors. Do not hand-edit generated files in `target/`.

## Contract

- Use a unique, stable transformation ID. Duplicate IDs fail processor construction at startup.
- Provide a public no-argument constructor (an implicit one is fine). Providers are instantiated at startup, not hot-loaded.
- Keep implementations stateless and thread-safe. Do not retain request text or parameter values in fields.
- Use the existing generic parameter types: `string`, `multiline`, `number` (integer), `boolean`, and `enum`. Helpers in `Parameters` cover common text, index, and enum metadata. Construct `Parameter` records directly for other configurations.
- The processor validates types, required values, enum options, and numeric bounds, and supplies defaults before `apply`. Enforce operation-specific rules inside the implementation and throw `TransformException(message, null, parameterId)` for located errors. The processor fills in the step index.
- Return deterministic text and respect `Processor.MAX_TEXT`. Check potentially large expansions before allocation. Use bounded algorithms for untrusted parameters.
- Keep metadata and semantics in the same class. Generic frontend components and API controllers do not need knowledge of a transformation's ID.
- Providers are sorted by ID for deterministic discovery order. Pipeline order is still the order chosen by the user.

## Standalone library use

```java
var processor = new Processor(TransformationDiscovery.load());
```

Or pass an explicit list of transformations to `Processor` for isolated tests and embedded consumers. Discovery uses the library's classloader by default; an overload accepts a custom classloader for plugin hosts. External modules need their own AutoService processor configuration, or a standard Java SPI provider resource, and must be on the application's runtime classpath.

The annotated `DiscoveredTestTransformation` fixture demonstrates adding a new class without touching any production registration code. It is discovered in core tests but is excluded from the production JAR; the HTTP test checks that it does not leak into the API catalog.
