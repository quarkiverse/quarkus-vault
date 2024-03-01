package io.quarkus.vault.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.vault.client.common.VaultRequest;
import io.quarkus.vault.client.common.VaultVoidResultExtractor;
import io.quarkus.vault.client.http.VaultHttpClient;
import io.quarkus.vault.client.logging.LogConfidentialityLevel;

public class VaultRequestTest {

    @Test
    public void testBuilderBasics() throws Exception {

        var request = VaultRequest.post("Test")
                .baseUrl(new URL("https://example.com:8200"))
                .apiVersion("v2")
                .path("/test")
                .body("{}")
                .token("test")
                .namespace("test-ns")
                .wrapTTL(Duration.ofSeconds(42))
                .header("foo", "bar")
                .queryParam("baz", "qux")
                .logConfidentialityLevel(LogConfidentialityLevel.MEDIUM)
                .timeout(Duration.ofSeconds(23))
                .expectOkOrAcceptedStatus()
                .build();

        assertThat(request.getMethod())
                .isEqualTo(VaultRequest.Method.POST);
        assertThat(request.getOperation())
                .isEqualTo("Test");
        assertThat(request.getBaseUrl())
                .isEqualTo(new URL("https://example.com:8200"));
        assertThat(request.getApiVersion())
                .isEqualTo("v2");
        assertThat(request.getPath())
                .isEqualTo("test");
        assertThat(request.getHeaders())
                .containsEntry("foo", "bar");
        assertThat(request.getQueryParams())
                .containsEntry("baz", "qux");
        assertThat(request.hasBody())
                .isTrue();
        assertThat(request.getBody())
                .isEqualTo(Optional.of("{}"));
        assertThat(request.hasToken())
                .isTrue();
        assertThat(request.getToken())
                .isEqualTo(Optional.of("test"));
        assertThat(request.hasNamespace())
                .isTrue();
        assertThat(request.getNamespace())
                .isEqualTo(Optional.of("test-ns"));
        assertThat(request.hasWrapTTL())
                .isTrue();
        assertThat(request.getWrapTTL())
                .isEqualTo(Optional.of(Duration.ofSeconds(42)));
        assertThat(request.getLogConfidentialityLevel())
                .isEqualTo(LogConfidentialityLevel.MEDIUM);
        assertThat(request.getTimeout())
                .isEqualTo(Duration.ofSeconds(23));
        assertThat(request.getExpectedStatusCodes())
                .isEqualTo(VaultRequest.OK_OR_ACCEPTED_STATUS);
        assertThat(request.getResultExtractor())
                .isInstanceOf(VaultVoidResultExtractor.class);
        assertThat(request.getUrl())
                .isEqualTo(new URL("https://example.com:8200/v2/test?baz=qux"));
        assertThat(request.getHTTPHeaders())
                .containsEntry(VaultHttpClient.X_VAULT_TOKEN, "test")
                .containsEntry(VaultHttpClient.X_VAULT_NAMESPACE, "test-ns")
                .containsEntry(VaultHttpClient.X_VAULT_NAMESPACE, "test-ns")
                .containsEntry("foo", "bar");
    }

    @Test
    public void testBuilderMethods() {
        assertThat(VaultRequest.get("Test").build().getMethod())
                .isEqualTo(VaultRequest.Method.GET);
        assertThat(VaultRequest.put("Test").build().getMethod())
                .isEqualTo(VaultRequest.Method.PUT);
        assertThat(VaultRequest.post("Test").build().getMethod())
                .isEqualTo(VaultRequest.Method.POST);
        assertThat(VaultRequest.patch("Test").build().getMethod())
                .isEqualTo(VaultRequest.Method.PATCH);
        assertThat(VaultRequest.delete("Test").build().getMethod())
                .isEqualTo(VaultRequest.Method.DELETE);
        assertThat(VaultRequest.head("Test").build().getMethod())
                .isEqualTo(VaultRequest.Method.HEAD);
        assertThat(VaultRequest.list("Test").build().getMethod())
                .isEqualTo(VaultRequest.Method.LIST);
    }

    @Test
    public void testAcceptedStatuses() {
        assertThat(VaultRequest.get("Test").expectOkStatus().build().getExpectedStatusCodes())
                .isEqualTo(VaultRequest.OK_STATUS);
        assertThat(VaultRequest.get("Test").expectNoContentStatus().build().getExpectedStatusCodes())
                .isEqualTo(VaultRequest.NO_CONTENT_STATUS);
        assertThat(VaultRequest.get("Test").expectAcceptedStatus().build().getExpectedStatusCodes())
                .isEqualTo(VaultRequest.ACCEPTED_STATUS);
        assertThat(VaultRequest.get("Test").expectOkOrAcceptedStatus().build().getExpectedStatusCodes())
                .isEqualTo(VaultRequest.OK_OR_ACCEPTED_STATUS);
        assertThat(VaultRequest.get("Test").expectOkOrNoContentStatus().build().getExpectedStatusCodes())
                .isEqualTo(VaultRequest.OK_OR_NO_CONTENT_STATUS);
        assertThat(VaultRequest.get("Test").expectAnyStatus().build().getExpectedStatusCodes())
                .isEqualTo(List.of());
    }

    @Test
    public void testPathChoiceWithBoolean() {

        var requestTrue = VaultRequest.post("Test")
                .pathChoice(true, new Object[] { "test", "1" }, new Object[] { "test", "2" })
                .build();

        assertThat(requestTrue.getPath())
                .isEqualTo("test/1");

        var requestFalse = VaultRequest.post("Test")
                .pathChoice(false, new Object[] { "test", "1" }, new Object[] { "test", "2" })
                .build();

        assertThat(requestFalse.getPath())
                .isEqualTo("test/2");
    }

    @Test
    public void testPathChoiceWithString() {

        var request1 = VaultRequest.post("Test")
                .pathChoice("1", Map.of("1", new Object[] { "test", "1" }, "2", new Object[] { "test", "2" }))
                .build();

        assertThat(request1.getPath())
                .isEqualTo("test/1");

        var request2 = VaultRequest.post("Test")
                .pathChoice("2", Map.of("1", new Object[] { "test", "1" }, "2", new Object[] { "test", "2" }))
                .build();

        assertThat(request2.getPath())
                .isEqualTo("test/2");
    }

    @Test
    public void testPathFiltersNulls() {

        var request = VaultRequest.post("Test")
                .path("test", null, "test2", null)
                .build();

        assertThat(request.getPath())
                .isEqualTo("test/test2");
    }

    @Test
    public void testQueryParamSelector() {

        var request = VaultRequest.post("Test")
                .queryParam(true, "foo", "bar")
                .queryParam(false, "baz", "qux")
                .build();

        assertThat(request.getQueryParams())
                .containsEntry("foo", "bar")
                .doesNotContainKey("baz");
    }

    @Test
    public void testQueryParamFormatsNullValuesAsFlags() throws Exception {

        var request = VaultRequest.post("Test")
                .baseUrl(new URL("https://example.com:8200"))
                .path("test")
                .queryParam("foo", null)
                .queryParam("bar", "baz")
                .build();

        assertThat(request.getUrl())
                .isEqualTo(new URL("https://example.com:8200/v1/test?foo&bar=baz"));
    }

    @Test
    public void testQueryParamEncodesEnumsWithJsonProperty() throws Exception {

        var request = VaultRequest.post("Test")
                .baseUrl(new URL("https://example.com:8200"))
                .path("test")
                .queryParam("foo", TestEnum.FOO)
                .build();

        assertThat(request.getUrl())
                .isEqualTo(new URL("https://example.com:8200/v1/test?foo=foo_option"));
    }

    @Test
    public void testHeaderSelector() {

        var request = VaultRequest.post("Test")
                .header(true, "foo", "bar")
                .header(false, "baz", "qux")
                .build();

        assertThat(request.getHeaders())
                .containsEntry("foo", "bar")
                .doesNotContainKey("baz");
    }

    @Test
    public void testHeaderEncodesValuesAsString() throws Exception {

        var request = VaultRequest.post("Test")
                .baseUrl(new URL("https://example.com:8200"))
                .path("test")
                .header("foo", 200)
                .build();

        assertThat(request.getHeaders())
                .containsEntry("foo", "200");

        var request2 = VaultRequest.post("Test")
                .baseUrl(new URL("https://example.com:8200"))
                .path("test")
                .header("foo", Duration.ofSeconds(200))
                .build();

        assertThat(request2.getHeaders())
                .containsEntry("foo", "3m20s");
    }

    @Test
    public void testHeaderEncodesEnumsWithJsonProperty() throws Exception {

        var request = VaultRequest.post("Test")
                .baseUrl(new URL("https://example.com:8200"))
                .path("test")
                .header("foo", TestEnum.FOO)
                .build();

        assertThat(request.getHeaders())
                .containsEntry("foo", "foo_option");
    }

    enum TestEnum {
        @JsonProperty("foo_option")
        FOO,
    }

}
