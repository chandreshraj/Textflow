package com.knime.textflow.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.knime.textflow.core.Processor;
import com.knime.textflow.core.Transformation;

@RestController
@RequestMapping("/api")
public class TransformController {
    private final Processor processor;

    public TransformController(Processor processor) {
        this.processor = processor;
    }

    @GetMapping("/transformations")
    public List<Transformation.Descriptor> transformations() {
        return processor.descriptors();
    }

    @PostMapping("/transform")
    public Processor.Result transform(@RequestBody TransformRequest request) {
        return processor.execute(request.inputText(), request.pipeline());
    }
}
