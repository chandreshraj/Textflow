package com.knime.textflow.core;
public class TransformException extends RuntimeException {
    private final Integer step;
    private final String parameter;
    public TransformException(String message, Integer step, String parameter) {
        super(message); this.step = step; this.parameter = parameter;
    }
    public Integer getStep() { return step; }
    public String getParameter() { return parameter; }
}
