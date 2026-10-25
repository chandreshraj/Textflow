package com.knime.textflow.api;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Logs API activity without reading or recording user text, headers or
 * parameters.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class ApiRequestLogging extends OncePerRequestFilter {
    private static final Logger LOG = LoggerFactory.getLogger(ApiRequestLogging.class);

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.equals("/api/transform") && !path.equals("/api/transformations");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        long started = System.nanoTime();
        boolean completed = false;
        try {
            chain.doFilter(request, response);
            completed = true;
        } finally {
            LOG.info("{} {} status={} durationMs={}", request.getMethod(), request.getRequestURI(),
                    completed ? response.getStatus() : 500,
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started));
        }
    }
}
