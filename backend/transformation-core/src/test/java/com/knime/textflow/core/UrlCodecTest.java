package com.knime.textflow.core;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

class UrlCodecTest {
    private final Processor processor = new Processor(TransformationDiscovery.load());

    private String run(String input, String mode) {
        return processor.execute(input, List.of(new Processor.Step("url-codec", Map.of("mode", mode)))).output();
    }

    @Test
    void encodesAComponentRatherThanAWholeUrl() {
        assertEquals("hello%20world", run("hello world", "Encode"));
        assertEquals("%3A%2F%3F%26%3D%23%2B%25%2A%21%27%28%29", run(":/?&=#+%*!'()", "Encode"));
        assertEquals("AZaz09-._~", run("AZaz09-._~", "Encode"));
        assertEquals("", run("", "Encode"));
    }

    @Test
    void supportsUnicodeAndLiteralPlus() {
        String input = "café 😀 +\n";
        String encoded = run(input, "Encode");
        assertEquals("caf%C3%A9%20%F0%9F%98%80%20%2B%0A", encoded);
        assertEquals(input, run(encoded, "Decode"));
        assertEquals("a+b c", run("a+b%20c", "Decode"));
        assertEquals("café 😀", run("café%20%f0%9f%98%80", "Decode"));
        assertEquals("%20", run("%2520", "Decode")); // exactly one decoding pass
        assertEquals("", run("", "Decode"));
    }

    @Test
    void rejectsMalformedEscapesAndUtf8() {
        for (String invalid : List.of("%", "%2", "%GG", "%FF", "%C3%28", "%ED%A0%80", "%C0%AF")) {
            var error = assertThrows(TransformException.class, () -> run(invalid, "Decode"));
            assertEquals(0, error.getStep());
            assertEquals("input", error.getParameter());
        }
        assertThrows(TransformException.class, () -> run("\uD800", "Encode"));
        assertThrows(TransformException.class, () -> run("text", "Unknown"));
    }

    @Test
    void boundsExpansionAndUsesDefaultMode() {
        assertEquals("%20", processor.execute(" ", List.of(new Processor.Step("url-codec", Map.of()))).output());
        assertEquals(99999, run(" ".repeat(33333), "Encode").length());
        assertThrows(TransformException.class, () -> run(" ".repeat(33334), "Encode"));
    }
}
