package com.knime.textflow.core;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class NormalizeWhitespaceTest {
    private final Processor processor = new Processor(TransformationDiscovery.load());

    private String normalize(String input, Map<String, Object> parameters) {
        return processor.execute(input, List.of(new Processor.Step("normalize-whitespace", parameters))).output();
    }

    @Test
    void discoversMetadataAndDefaults() {
        var descriptor = processor.descriptors().stream()
                .filter(d -> d.id().equals("normalize-whitespace")).findFirst().orElseThrow();
        assertEquals(2, descriptor.parameters().size());
        assertTrue(descriptor.parameters().stream()
                .allMatch(p -> p.type().equals("boolean") && p.defaultValue().equals(true)));
        assertEquals("Hello world", normalize("  Hello   world  ", Map.of()));
        assertEquals("Hello world", normalize("Hello\t \tworld", Map.of()));
    }

    @Test
    void preservesBlankLinesAndNormalizesAllLineEndings() {
        assertEquals("one\n\ntwo\nthree\nfour\nfive\nsix\nseven\neight\n",
                normalize(" one \r\n \r\n two \r three \n four \u0085 five \u2028 six \u2029 seven \u000B eight \u000C",
                        Map.of()));
    }

    @Test
    void canJoinLinesAndDisableTrimmingIndependently() {
        assertEquals("one two three", normalize(" \tone\r\n\n two\u2028three \t", Map.of("preserveLineBreaks", false)));
        assertEquals(" one \n two ", normalize("  one  \r\n\t two\t", Map.of("trimEdges", false)));
        assertEquals(" one two ",
                normalize("  one \r\n two  ", Map.of("trimEdges", false, "preserveLineBreaks", false)));
    }

    @Test
    void handlesEmptyAndWhitespaceOnlyInput() {
        assertEquals("", normalize("", Map.of()));
        assertEquals("", normalize(" \t\u00A0 ", Map.of()));
        assertEquals("\n\n", normalize(" \t\n \r\n ", Map.of()));
        assertEquals("", normalize(" \n\r\n ", Map.of("preserveLineBreaks", false)));
        assertEquals(" ", normalize("\t   ", Map.of("trimEdges", false)));
    }

    @Test
    void handlesUnicodeSpacesWithoutChangingOtherText() {
        assertEquals("😀 café e\u0301", normalize("\u00A0😀\u2003\u202Fcafé\t e\u0301\u00A0", Map.of()));
        assertEquals("a\u200Bb", normalize("a\u200Bb", Map.of())); // zero-width space is not Unicode whitespace
        String text = normalize("  a\t b\r\n c  ", Map.of());
        assertEquals(text, normalize(text, Map.of()));
    }

    @Test
    void composesAndValidatesBooleanParameters() {
        assertEquals("HELLO WORLD!", processor.execute("  hello   world ", List.of(
                new Processor.Step("normalize-whitespace", Map.of()),
                new Processor.Step("case", Map.of()),
                new Processor.Step("append", Map.of()))).output());
        var error = assertThrows(TransformException.class,
                () -> normalize("x", Map.of("trimEdges", "true")));
        assertEquals(0, error.getStep());
        assertEquals("trimEdges", error.getParameter());
    }
}
