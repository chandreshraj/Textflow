package com.knime.textflow.core;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ProcessorTest {
    Processor processor=new Processor(Builtins.all());
    Processor.Step step(String id, Map<String,Object> params) { return new Processor.Step(id,params); }
    String run(String input,String id,Map<String,Object> params) { return processor.execute(input,List.of(step(id,params))).output(); }
    @Test void orderedComposition() {
        var result=processor.execute("Fake Python",List.of(step("replace",Map.of("old","Fake","new","Real")),step("append",Map.of("text","!"))));
        assertEquals("Real Python!",result.output()); assertEquals("Real Python",result.steps().getFirst().output());
    }
    @Test void identityAndEmptyValues() {
        assertEquals("",processor.execute("",List.of()).output());
        assertEquals(" \n",processor.execute(" \n",List.of()).output());
        assertEquals("xx",run("xxx","replace",Map.of("old","xx","new","x")));
        assertEquals("",run("hi","replace",Map.of("old","hi","new","")));
        assertEquals("a",run("a","append",Map.of("text","")));
    }
    @Test void eachBuiltin() {
        assertEquals("hi!",run("hi","append",Map.of()));
        assertEquals("Hello, world",run("world","prepend",Map.of()));
        assertEquals("😀",run("a😀b","substring",Map.of("start",1,"end",2)));
        assertEquals("",run("abc","substring",Map.of("start",0,"end",0)));
        assertEquals("AB\nCD",run("ab\ncd","case",Map.of("mode","UPPERCASE")));
        assertEquals("ab",run("AB","case",Map.of("mode","lowercase")));
        assertEquals("hello",run(" \u2003hello\n","trim",Map.of()));
        assertEquals("12\n34",run("a12 b34","regex",Map.of("pattern","[0-9]+")));
        assertEquals("",run("abc","regex",Map.of("pattern","[0-9]+")));
    }
    @Test void rejectsInvalidValuesWithLocation() {
        var e=assertThrows(TransformException.class,()->run("x","replace",Map.of("old","")));
        assertEquals(0,e.getStep()); assertEquals("old",e.getParameter());
        assertThrows(TransformException.class,()->run("abc","substring",Map.of("start",0.5)));
        assertThrows(TransformException.class,()->run("abc","substring",Map.of("start",3,"end",2)));
        assertThrows(TransformException.class,()->run("abc","substring",Map.of("end",99)));
        assertThrows(TransformException.class,()->run("x","append",Map.of("unknown",true)));
        assertThrows(TransformException.class,()->run("x","append",Map.of("text",false)));
        assertThrows(TransformException.class,()->run("x","case",Map.of("mode","other")));
        assertThrows(TransformException.class,()->run("x","missing",Map.of()));
        assertThrows(TransformException.class,()->run("x","regex",Map.of("pattern","(?=x)")));
        assertThrows(TransformException.class,()->processor.execute(null,List.of()));
        assertThrows(TransformException.class,()->processor.execute("x",null));
        assertThrows(TransformException.class,()->run("x","append",null));
    }
    @Test void limits() {
        assertThrows(TransformException.class,()->processor.execute("x".repeat(100001),List.of()));
        assertThrows(TransformException.class,()->processor.execute("x",Collections.nCopies(31,step("trim",Map.of()))));
        assertThrows(TransformException.class,()->run("x".repeat(100000),"append",Map.of("text","!")));
    }
    @Test void backendOnlyExtensionAndRepeatedSteps() {
        Transformation reverse = new Transformation() {
            public Descriptor descriptor() { return new Descriptor("reverse","Reverse","Reverse text",List.of()); }
            public String apply(String input,Map<String,Object> params) { return new StringBuilder(input).reverse().toString(); }
        };
        var extended=new ArrayList<>(Builtins.all()); extended.add(reverse);
        var custom=new Processor(extended);
        assertEquals("reverse",custom.descriptors().getLast().id());
        assertEquals("cba",custom.execute("abc",List.of(step("reverse",Map.of()))).output());
        assertEquals("abc",custom.execute("abc",List.of(step("reverse",Map.of()),step("reverse",Map.of()))).output());
    }
}
