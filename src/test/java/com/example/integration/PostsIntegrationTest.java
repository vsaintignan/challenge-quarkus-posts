package com.example.integration;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import com.github.tomakehurst.wiremock.WireMockServer;
import static com.github.tomakehurst.wiremock.client.WireMock.*;

@QuarkusTest
public class PostsIntegrationTest {

    static WireMockServer wm;

    @BeforeAll
    static void setup() {
        wm = new WireMockServer(9099);
        wm.start();
        configureFor("localhost", 9099);
        stubFor(get(urlEqualTo("/posts")).willReturn(okJson("[{\"userId\":1,\"id\":1,\"title\":\"t\",\"body\":\"b\"}]")));
        stubFor(get(urlEqualTo("/users/1")).willReturn(okJson("{\"id\":1,\"name\":\"User\",\"email\":\"u@x.com\"}")));
        stubFor(get(urlEqualTo("/posts/1/comments")).willReturn(okJson("[{\"postId\":1,\"id\":1,\"email\":\"c@x.com\",\"body\":\"c\"}]")));
        System.setProperty("jsonplaceholder.base-url", "http://localhost:9099");
        System.setProperty("security.api-key","");
    }

    @Test
    public void shouldMerge() {
        given()
                .when().get("/posts?limit=1&includeComments=true")
                .then().statusCode(200)
                .body("[0].author.name", equalTo("User"))
                .body("[0].commentCount", equalTo(1));
    }
}
