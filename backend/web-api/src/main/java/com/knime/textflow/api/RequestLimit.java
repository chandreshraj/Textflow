package com.knime.textflow.api;
import java.io.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Bound JSON before deserialization, including chunked requests. */
@Component
public class RequestLimit extends OncePerRequestFilter {
    private static final int MAX_BYTES=1_048_576;
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        if(!request.getRequestURI().equals("/api/transform") || !request.getMethod().equals("POST")) { chain.doFilter(request,response); return; }
        byte[] body=request.getInputStream().readNBytes(MAX_BYTES+1);
        if(body.length>MAX_BYTES) {
            response.setStatus(413); response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Request exceeds 1 MiB.\",\"step\":null,\"parameter\":null}"); return;
        }
        var wrapped=new HttpServletRequestWrapper(request) {
            @Override public ServletInputStream getInputStream() {
                var stream=new ByteArrayInputStream(body);
                return new ServletInputStream() {
                    public int read() { return stream.read(); }
                    public boolean isFinished() { return stream.available()==0; }
                    public boolean isReady() { return true; }
                    public void setReadListener(ReadListener listener) { throw new UnsupportedOperationException("Synchronous endpoint"); }
                };
            }
        };
        chain.doFilter(wrapped,response);
    }
}
