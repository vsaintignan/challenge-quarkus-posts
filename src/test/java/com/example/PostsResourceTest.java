package com.example;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
public class PostsResourceTest {

    @Test
    public void testGetPostsEndpointLoads() {
        given()
          .when().get("/posts?limit=1&includeComments=false")
          .then()
             .statusCode(anyOf(is(200), is(502))); // 502 if external is unreachable in tests
    }
}
