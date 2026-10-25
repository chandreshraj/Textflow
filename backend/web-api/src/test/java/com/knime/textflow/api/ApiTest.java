package com.knime.textflow.api;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiTest {
    @LocalServerPort
    int port;
    HttpClient client = HttpClient.newHttpClient();

    HttpResponse<String> post(String json) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/transform"))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(json)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void discovery() throws Exception {
        var response = client.send(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/transformations")).build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("defaultValue"));
        assertTrue(response.body().contains("substring"));
        assertTrue(response.body().contains("normalize-whitespace"));
        assertTrue(response.body().contains("url-codec"));
        assertTrue(response.body().contains("split-join"));
        assertFalse(response.body().contains("test-decorate"));
    }

    @Test
    void normalizeWhitespace() throws Exception {
        var response = post("""
                {"input":"  Hello   world  ","pipeline":[{"transformation":"normalize-whitespace","parameters":{}}]}
                """);
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"output\":\"Hello world\""));
    }

    @Test
    void splitJoinAndUrlCodec() throws Exception {
        var response = post(
                """
                        {"input":"a,b,c","pipeline":[{"transformation":"split-join","parameters":{"joinSeparator":" "}},{"transformation":"url-codec","parameters":{}}]}
                        """);
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"output\":\"a%20b%20c\""));
        var invalid = post("""
                {"input":"%GG","pipeline":[{"transformation":"url-codec","parameters":{"mode":"Decode"}}]}
                """);
        assertEquals(400, invalid.statusCode());
        assertTrue(invalid.body().contains("\"step\":0"));
        assertTrue(invalid.body().contains("\"parameter\":\"input\""));
    }

    @Test
    void compressesLargePreviewResponses() throws Exception {
        String steps = java.util.stream.IntStream.range(0, 30)
                .mapToObj(i -> "{\"transformation\":\"trim\",\"parameters\":{}}")
                .collect(java.util.stream.Collectors.joining(","));
        String body = "{\"input\":\"" + "a".repeat(80000) + "\",\"pipeline\":[" + steps + "]}";
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/transform"))
                .header("Content-Type", "application/json").header("Accept-Encoding", "gzip")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        assertEquals(200, response.statusCode());
        assertEquals("gzip", response.headers().firstValue("Content-Encoding").orElseThrow());
        try (var stream = new java.util.zip.GZIPInputStream(new java.io.ByteArrayInputStream(response.body()))) {
            byte[] json = stream.readAllBytes();
            assertTrue(json.length > 2_000_000);
            assertTrue(response.body().length < json.length / 10);
            assertTrue(new String(json, java.nio.charset.StandardCharsets.UTF_8)
                    .contains("\"output\":\"" + "a".repeat(80000)));
        }
    }

    @Test
    void execution() throws Exception {
        var r = post(
                """
                        {"input":"Fake Python","pipeline":[{"transformation":"replace","parameters":{"old":"Fake","new":"Real"}},{"transformation":"append","parameters":{"text":"!"}}]}
                        """);
        assertEquals(200, r.statusCode());
        assertTrue(r.body().contains("Real Python!"));
    }

    @Test
    @org.junit.jupiter.api.extension.ExtendWith(org.springframework.boot.test.system.OutputCaptureExtension.class)
    void logsRequestsWithoutUserContent(org.springframework.boot.test.system.CapturedOutput logs) throws Exception {
        String body = """
                {"input":"private-input-value","pipeline":[{"transformation":"append","parameters":{"text":"private-parameter-value"}}]}
                """;
        var request = HttpRequest
                .newBuilder(URI.create("http://127.0.0.1:" + port + "/api/transform?debug=private-query-value"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer private-header-value")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        assertEquals(200, client.send(request, HttpResponse.BodyHandlers.ofString()).statusCode());
        assertEquals(400, post("{").statusCode());
        assertEquals(413, post("x".repeat(1_048_577)).statusCode());
        String output = logs.getOut();
        assertTrue(output.contains("POST /api/transform status=200 durationMs="));
        assertTrue(output.contains("POST /api/transform status=400 durationMs="));
        assertTrue(output.contains("POST /api/transform status=413 durationMs="));
        assertFalse(output.contains("private-input-value"));
        assertFalse(output.contains("private-parameter-value"));
        assertFalse(output.contains("private-query-value"));
        assertFalse(output.contains("private-header-value"));
    }

    @Test
    void rejectsOversizedBodyAndWrongInputType() throws Exception {
        assertEquals(413, post("x".repeat(1_048_577)).statusCode());
        assertEquals(400, post("{\"input\":123,\"pipeline\":[]}").statusCode());
    }

    @Test
    void malformedAndValidation() throws Exception {
        assertEquals(400, post("{").statusCode());
        assertEquals(400, post("{}").statusCode());
        assertEquals(400, post("{\"input\":\"x\",\"pipeline\":{}}").statusCode());
        var r = post("{\"input\":\"x\",\"pipeline\":[{\"transformation\":\"replace\",\"parameters\":{\"old\":\"\"}}]}");
        assertEquals(400, r.statusCode());
        assertTrue(r.body().contains("\"step\":0"));
        assertTrue(r.body().contains("\"parameter\":\"old\""));
    }
}
