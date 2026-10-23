package com.knime.textflow.api;
import com.knime.textflow.core.*;
import java.util.List;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api")
public class TransformController {
    private final Processor processor=new Processor(TransformationDiscovery.load());
    public record Request(Object input, List<Processor.Step> pipeline) {}
    @GetMapping("/transformations")
    public List<Transformation.Descriptor> transformations() { return processor.descriptors(); }
    @PostMapping("/transform")
    public Processor.Result transform(@RequestBody Request request) { if (!(request.input() instanceof String input)) throw new TransformException("Input must be a string.",null,"input"); return processor.execute(input,request.pipeline()); }
}
