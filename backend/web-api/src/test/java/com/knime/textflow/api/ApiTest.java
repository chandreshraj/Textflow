package com.knime.textflow.api;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import java.net.*;
import java.net.http.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiTest {
    @LocalServerPort int port;
    HttpClient client=HttpClient.newHttpClient();
    HttpResponse<String> post(String json) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/transform")).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json)).build(),HttpResponse.BodyHandlers.ofString());
    }
    @Test void discovery() throws Exception {
        var response=client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/transformations")).build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(200,response.statusCode()); assertTrue(response.body().contains("defaultValue")); assertTrue(response.body().contains("substring"));
    }
    @Test void execution() throws Exception {
        var r=post("""
            {"input":"Fake Python","pipeline":[{"transformation":"replace","parameters":{"old":"Fake","new":"Real"}},{"transformation":"append","parameters":{"text":"!"}}]}
            """);
        assertEquals(200,r.statusCode()); assertTrue(r.body().contains("Real Python!"));
    }
    @Test void rejectsOversizedBodyAndWrongInputType() throws Exception {
        assertEquals(413,post("x".repeat(1_048_577)).statusCode());
        assertEquals(400,post("{\"input\":123,\"pipeline\":[]}").statusCode());
    }
    @Test void malformedAndValidation() throws Exception {
        assertEquals(400,post("{").statusCode());
        assertEquals(400,post("{}").statusCode());
        assertEquals(400,post("{\"input\":\"x\",\"pipeline\":{}}").statusCode());
        var r=post("{\"input\":\"x\",\"pipeline\":[{\"transformation\":\"replace\",\"parameters\":{\"old\":\"\"}}]}");
        assertEquals(400,r.statusCode()); assertTrue(r.body().contains("\"step\":0")); assertTrue(r.body().contains("\"parameter\":\"old\""));
    }
}
