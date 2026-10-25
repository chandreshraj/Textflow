package com.knime.textflow.api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.knime.textflow.core.Processor;
import com.knime.textflow.core.TransformationDiscovery;

/**
 * Application wiring is separate from HTTP handling and library implementation.
 */
@Configuration
public class ProcessorConfiguration {
    @Bean
    Processor processor() {
        return new Processor(TransformationDiscovery.load());
    }
}
