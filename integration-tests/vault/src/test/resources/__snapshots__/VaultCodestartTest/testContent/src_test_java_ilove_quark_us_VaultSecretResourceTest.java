package ilove.quark.us;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class VaultSecretResourceTest {

    @Test
    void testSecretFromConfig() {
        given()
            .when().get("/vault/config")
            .then()
            .statusCode(200)
            .body(is("hello-from-vault"));
    }

    @Test
    void testSecretFromEngine() {
        given()
            .when().get("/vault/engine")
            .then()
            .statusCode(200)
            .body(is("hello-from-vault"));
    }
}

