package com.knime.textflow.core;
import java.util.*;
import java.util.function.BiFunction;
import com.google.re2j.Pattern;
import com.google.re2j.PatternSyntaxException;
import static com.knime.textflow.core.Transformation.*;

public final class Builtins {
    private Builtins() {}
    private static Parameter text(String id, String label, String value) {
        return new Parameter(id, label, "string", true, value, null, null, List.of(), "");
    }
    private static Parameter number(String id, String label, int value) {
        return new Parameter(id, label, "number", true, value, 0, 100000, List.of(), "Indices count Unicode code points; end is exclusive.");
    }
    private static Transformation define(String id, String name, String description, List<Parameter> params,
                                          BiFunction<String, Map<String,Object>,String> operation) {
        return new Transformation() {
            public Descriptor descriptor() { return new Descriptor(id, name, description, params); }
            public String apply(String input, Map<String,Object> parameters) { return operation.apply(input, parameters); }
        };
    }
    public static List<Transformation> all() {
        return List.of(
            define("replace", "Replace", "Find literal text and replace every occurrence.", List.of(text("old", "Find", "Fake"), text("new", "Replace with", "Real")), (s,p) -> {
                if (((String)p.get("old")).isEmpty()) throw new TransformException("Find text cannot be empty.", null, "old");
                String old=(String)p.get("old"), replacement=(String)p.get("new");
                long length=s.length(); int position=0;
                while((position=s.indexOf(old,position))>=0) {
                    length += replacement.length()-old.length(); position += old.length();
                    if(length>Processor.MAX_TEXT) throw new TransformException("Step output exceeds 100000 UTF-16 code units.",null,"new");
                }
                return s.replace(old,replacement);
            }),
            define("append", "Append", "Add text to the end.", List.of(text("text", "Text to append", "!")), (s,p) -> s + p.get("text")),
            define("prepend", "Prepend", "Add text to the beginning.", List.of(text("text", "Text to prepend", "Hello, ")), (s,p) -> p.get("text") + s),
            define("substring", "Substring", "Keep a range of Unicode characters.", List.of(number("start", "Start index", 0), number("end", "End index", 4)), (s,p) -> {
                int start=((Number)p.get("start")).intValue(), end=((Number)p.get("end")).intValue();
                if (end < start || end > s.codePointCount(0,s.length())) throw new TransformException("End must be at least start and within the text length.", null, "end");
                return s.substring(s.offsetByCodePoints(0,start),s.offsetByCodePoints(0,end));
            }),
            define("regex", "Regex match", "Extract all matches, separated by a line break.", List.of(text("pattern", "Pattern", "[A-Za-z]+")), (s,p) -> {
                String regex=(String)p.get("pattern");
                if (regex.length()>500) throw new TransformException("Pattern must be at most 500 characters.",null,"pattern");
                try {
                    var matcher=Pattern.compile(regex).matcher(s); var result=new StringBuilder(); boolean first=true;
                    while(matcher.find()) { if(!first) result.append('\n'); result.append(matcher.group()); first=false; }
                    return result.toString();
                } catch(PatternSyntaxException e) { throw new TransformException("Invalid or unsupported RE2 pattern: " + e.getDescription(),null,"pattern"); }
            }),
            define("case", "Change case", "Convert text using locale-independent casing.", List.of(new Parameter("mode","Letter case","enum",true,"UPPERCASE",null,null,List.of("UPPERCASE","lowercase"),"")), (s,p) -> "UPPERCASE".equals(p.get("mode")) ? s.toUpperCase(Locale.ROOT) : s.toLowerCase(Locale.ROOT)),
            define("trim", "Trim whitespace", "Remove leading and trailing Unicode whitespace.", List.of(), (s,p) -> s.strip())
        );
    }
}
