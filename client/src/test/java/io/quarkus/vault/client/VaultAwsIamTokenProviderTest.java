package io.quarkus.vault.client;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.InstantSource;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import io.quarkus.vault.client.api.auth.aws.VaultAuthAwsLoginParams;
import io.quarkus.vault.client.auth.VaultAuthRequest;
import io.quarkus.vault.client.auth.VaultAwsIamAuthOptions;
import io.quarkus.vault.client.auth.VaultAwsIamTokenProvider;
import io.quarkus.vault.client.common.VaultRequest;
import io.quarkus.vault.client.common.VaultRequestExecutor;
import io.quarkus.vault.client.common.VaultResponse;

class VaultAwsIamTokenProviderTest {

    private static final String LOGIN_RESPONSE = "{" +
            "\"request_id\":\"req\",\"lease_id\":\"\",\"renewable\":false,\"lease_duration\":0,\"data\":null," +
            "\"auth\":{\"client_token\":\"vault-token\",\"renewable\":true,\"lease_duration\":3600,\"num_uses\":0," +
            "\"policies\":[],\"metadata\":{}}}";

    @Test
    void buildsSignedGetCallerIdentityLoginRequest() throws Exception {
        var captured = new AtomicReference<VaultRequest<?>>();
        VaultRequestExecutor executor = new VaultRequestExecutor() {
            @SuppressWarnings({ "unchecked", "rawtypes" })
            @Override
            public <T> CompletionStage<VaultResponse<T>> execute(VaultRequest<T> request) {
                captured.set(request);
                return CompletableFuture.completedStage(
                        new VaultResponse(request, 200, List.of(), LOGIN_RESPONSE.getBytes(UTF_8)));
            }
        };

        var options = VaultAwsIamAuthOptions.builder()
                .role("my-role")
                .region("us-east-1")
                .vaultServerId("vault.example.com")
                .staticCredentials("AKIAEXAMPLE", "secretkey")
                .build();

        var token = new VaultAwsIamTokenProvider(options)
                .apply(VaultAuthRequest.of(executor, null, InstantSource.system()))
                .toCompletableFuture().get();

        assertThat(token.getClientToken()).isEqualTo("vault-token");

        var params = (VaultAuthAwsLoginParams) captured.get().getBody().orElseThrow();
        assertThat(params.getRole()).isEqualTo("my-role");
        assertThat(params.getIamHttpRequestMethod()).isEqualTo("POST");
        assertThat(decode(params.getIamRequestUrl())).isEqualTo("https://sts.amazonaws.com");
        assertThat(decode(params.getIamRequestBody())).isEqualTo("Action=GetCallerIdentity&Version=2011-06-15");

        var headers = decode(params.getIamRequestHeaders());
        assertThat(headers)
                .contains("\"Authorization\"")
                .contains("AWS4-HMAC-SHA256")
                .contains("Credential=AKIAEXAMPLE/")
                .contains("/us-east-1/sts/aws4_request")
                .contains("\"X-Vault-AWS-IAM-Server-ID\":[\"vault.example.com\"]");
    }

    private static String decode(String base64) {
        return new String(Base64.getDecoder().decode(base64), UTF_8);
    }
}
