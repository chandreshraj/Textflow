package com.knime.textflow.core;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

class SplitJoinTest {
    private final Processor processor = new Processor(TransformationDiscovery.load());

    private String run(String input, Map<String, Object> parameters) {
        return processor.execute(input, List.of(new Processor.Step("split-join", parameters))).output();
    }

    @Test
    void joinsWithDefaultsAndPreservesEmptyItems() {
        assertEquals("a · b · c", run("a,b,c", Map.of()));
        assertEquals(" · a ·  · b · ", run(",a,,b,", Map.of()));
        assertEquals("", run("", Map.of()));
        assertEquals("abc", run("abc", Map.of()));
    }

    @Test
    void usesLiteralMultiCharacterSeparatorsAndPreservesWhitespace() {
        assertEquals("a| b|c", run("a.* b.*c", Map.of("separator", ".*", "joinSeparator", "|")));
        assertEquals("a😀b", run("a::b", Map.of("separator", "::", "joinSeparator", "😀")));
        assertEquals("a,b", run("a\nb", Map.of("separator", "\n", "joinSeparator", ",")));
        assertEquals("-a", run("aaa", Map.of("separator", "aa", "joinSeparator", "-")));
        assertEquals("ab", run("a,b,", Map.of("joinSeparator", "")));
    }

    @Test
    void validatesSeparatorAndBoundsBeforeAllocation() {
        var error = assertThrows(TransformException.class, () -> run("x", Map.of("separator", "")));
        assertEquals("separator", error.getParameter());
        assertThrows(TransformException.class, () -> run("x", Map.of("separator", false)));
        assertThrows(TransformException.class, () -> run(",".repeat(100000), Map.of("joinSeparator", "long")));
        assertEquals(100000, run(",".repeat(50000), Map.of("joinSeparator", "xx")).length());
    }

    @Test
    void composesWithUrlEncoding() {
        var encoded = processor.execute("a,b,c", List.of(new Processor.Step("split-join", Map.of()),
                new Processor.Step("url-codec", Map.of()))).output();
        assertEquals("a%20%C2%B7%20b%20%C2%B7%20c", encoded);
    }
}
