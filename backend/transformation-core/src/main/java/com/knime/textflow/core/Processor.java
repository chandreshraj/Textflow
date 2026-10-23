package com.knime.textflow.core;
import java.util.*;
import static com.knime.textflow.core.Transformation.*;

public final class Processor {
    public static final int MAX_TEXT = 100000, MAX_STEPS = 30;
    public record Step(String transformation, Map<String,Object> parameters) {}
    public record StepResult(int index, String transformation, String output) {}
    public record Result(String output, List<StepResult> steps) {}
    private final Map<String,Transformation> registry = new LinkedHashMap<>();
    public Processor(List<Transformation> transformations) {
        for(var t:transformations) if(registry.putIfAbsent(t.descriptor().id(),t)!=null)
            throw new IllegalArgumentException("Duplicate transformation ID");
    }
    public List<Descriptor> descriptors() { return registry.values().stream().map(Transformation::descriptor).toList(); }
    public Result execute(String input, List<Step> pipeline) {
        if(input==null || input.length()>MAX_TEXT) throw new TransformException("Input is required and must be at most 100000 UTF-16 code units.",null,"input");
        if(pipeline==null || pipeline.size()>MAX_STEPS) throw new TransformException("Pipeline is required and may contain at most 30 steps.",null,"pipeline");
        var results=new ArrayList<StepResult>(); String current=input;
        for(int i=0;i<pipeline.size();i++) {
            Step step=pipeline.get(i);
            if(step==null || step.transformation()==null || !registry.containsKey(step.transformation())) throw new TransformException("Unknown or missing transformation.",i,null);
            var t=registry.get(step.transformation());
            try {
                Map<String,Object> params=validate(t.descriptor(),step.parameters());
                current=t.apply(current,params);
                if(current==null || current.length()>MAX_TEXT) throw new TransformException("Step output exceeds 100000 UTF-16 code units.",null,null);
            } catch(TransformException e) { throw new TransformException(e.getMessage(),i,e.getParameter()); }
            results.add(new StepResult(i,step.transformation(),current));
        }
        return new Result(current,List.copyOf(results));
    }
    private Map<String,Object> validate(Descriptor d, Map<String,Object> supplied) {
        if(supplied==null) throw new TransformException("Parameters must be an object.",null,"parameters");
        var validated=new HashMap<String,Object>();
        for(String key:supplied.keySet()) if(d.parameters().stream().noneMatch(p -> p.id().equals(key))) throw new TransformException("Unknown parameter: " + key,null,key);
        for(var p:d.parameters()) {
            Object v=supplied.containsKey(p.id()) ? supplied.get(p.id()) : p.defaultValue();
            if(v==null) { if(p.required()) throw new TransformException("A value is required.",null,p.id()); else continue; }
            boolean valid=switch(p.type()) {
                case "string", "multiline" -> v instanceof String;
                case "boolean" -> v instanceof Boolean;
                case "enum" -> v instanceof String && p.options().contains(v);
                case "number" -> v instanceof Number n && Double.isFinite(n.doubleValue()) && n.doubleValue()==Math.rint(n.doubleValue()) && (p.min()==null || n.doubleValue()>=p.min()) && (p.max()==null || n.doubleValue()<=p.max());
                default -> false;
            };
            if(!valid) throw new TransformException("Invalid value for " + p.label() + ".",null,p.id());
            validated.put(p.id(),v);
        }
        return validated;
    }
}
