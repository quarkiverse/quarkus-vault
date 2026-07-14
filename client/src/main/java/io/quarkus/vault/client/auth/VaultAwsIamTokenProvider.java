package io.quarkus.vault.client.auth;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import io.quarkus.vault.client.api.auth.aws.VaultAuthAws;
import io.quarkus.vault.client.common.VaultResponse;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.auth.signer.params.Aws4SignerParams;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.regions.Region;

/**
 * Authenticates against Vault's AWS auth method using the {@code iam} workflow: a
 * {@code sts:GetCallerIdentity} request is signed with AWS SigV4 credentials and handed to Vault,
 * which replays it against STS to verify the caller's identity.
 *
 * @see <a href="https://developer.hashicorp.com/vault/api-docs/auth/aws">Vault AWS auth method</a>
 */
public class VaultAwsIamTokenProvider implements VaultTokenProvider {

    private static final String GET_CALLER_IDENTITY_REQUEST_BODY = "Action=GetCallerIdentity&Version=2011-06-15";
    private static final String VAULT_AWS_IAM_SERVER_ID_HEADER = "X-Vault-AWS-IAM-Server-ID";

    private final String mountPath;
    private final String role;
    private final Region region;
    private final String stsUrl;
    private final Optional<String> vaultServerId;
    private final AwsCredentialsProvider credentialsProvider;

    public VaultAwsIamTokenProvider(String mountPath, String role, Region region, String stsUrl,
            Optional<String> vaultServerId, AwsCredentialsProvider credentialsProvider) {
        this.mountPath = mountPath;
        this.role = role;
        this.region = region;
        this.stsUrl = stsUrl;
        this.vaultServerId = vaultServerId;
        this.credentialsProvider = credentialsProvider;
    }

    public VaultAwsIamTokenProvider(VaultAwsIamAuthOptions options) {
        this(options.mountPath, options.role, options.region, options.stsUrl, options.vaultServerId,
                options.credentialsProvider);
    }

    @Override
    public CompletionStage<VaultToken> apply(VaultAuthRequest authRequest) {
        var executor = authRequest.getExecutor();

        // Building/signing the request resolves AWS credentials, which may perform blocking network
        // calls (IMDS, STS, ...), so keep it off the calling (event loop) thread.
        return CompletableFuture.supplyAsync(this::buildSignedRequest)
                .thenCompose(signedRequest -> {
                    var requestUrl = base64(signedRequest.getUri().toString());
                    var requestBody = base64(GET_CALLER_IDENTITY_REQUEST_BODY);
                    var requestHeaders = base64(headersToJsonString(signedRequest.headers()));

                    return executor.execute(
                            VaultAuthAws.FACTORY.login(mountPath, role, SdkHttpMethod.POST.name(), requestUrl,
                                    requestBody, requestHeaders));
                })
                .thenApply(VaultResponse::getResult)
                .thenApply(res -> {
                    var auth = res.getAuth();
                    return VaultToken.from(auth.getClientToken(), auth.isRenewable(), auth.getLeaseDuration(),
                            auth.getNumUses(), authRequest.getInstantSource());
                });
    }

    private SdkHttpFullRequest buildSignedRequest() {
        var credentials = credentialsProvider.resolveCredentials();
        return signRequest(buildGetCallerIdentityRequest(), credentials);
    }

    private SdkHttpFullRequest buildGetCallerIdentityRequest() {
        var builder = SdkHttpFullRequest.builder()
                .method(SdkHttpMethod.POST)
                .uri(URI.create(stsUrl))
                .appendHeader("Content-Type", "application/x-www-form-urlencoded; charset=utf-8")
                .appendHeader("Content-Length", String.valueOf(GET_CALLER_IDENTITY_REQUEST_BODY.length()))
                .contentStreamProvider(() -> new ByteArrayInputStream(GET_CALLER_IDENTITY_REQUEST_BODY.getBytes(UTF_8)));

        vaultServerId.ifPresent(serverId -> builder.appendHeader(VAULT_AWS_IAM_SERVER_ID_HEADER, serverId));

        return builder.build();
    }

    private SdkHttpFullRequest signRequest(SdkHttpFullRequest request, AwsCredentials credentials) {
        var params = Aws4SignerParams.builder()
                .awsCredentials(credentials)
                .signingName("sts")
                .signingRegion(region)
                .build();
        return Aws4Signer.create().sign(request, params);
    }

    private static String base64(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(UTF_8));
    }

    private static String headersToJsonString(Map<String, List<String>> headers) {
        return "{"
                + headers.entrySet().stream()
                        .map(entry -> "\"" + entry.getKey() + "\":["
                                + entry.getValue().stream().map(value -> "\"" + value + "\"").collect(joining(","))
                                + "]")
                        .collect(joining(","))
                + "}";
    }
}
