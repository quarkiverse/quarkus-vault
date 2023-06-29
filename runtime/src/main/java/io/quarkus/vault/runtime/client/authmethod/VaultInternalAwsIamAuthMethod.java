package io.quarkus.vault.runtime.client.authmethod;

import io.quarkus.vault.runtime.Base64String;
import io.quarkus.vault.runtime.StringHelper;
import io.quarkus.vault.runtime.VaultConfigHolder;
import io.quarkus.vault.runtime.client.VaultClient;
import io.quarkus.vault.runtime.client.VaultInternalBase;
import io.quarkus.vault.runtime.client.dto.auth.VaultAwsIamAuth;
import io.quarkus.vault.runtime.client.dto.auth.VaultAwsIamAuthBody;
import io.quarkus.vault.runtime.config.VaultAwsIamAuthenticationConfig;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.auth.signer.params.Aws4SignerParams;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.regions.Region;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.joining;

@Singleton
public class VaultInternalAwsIamAuthMethod extends VaultInternalBase {

    private static final String GET_CALLER_IDENTITY_REQUEST_BODY = "Action=GetCallerIdentity&Version=2011-06-15";
    @Inject
    private VaultConfigHolder vaultConfigHolder;

    @Override
    protected String opNamePrefix() {
        return super.opNamePrefix() + " [AUTH (aws iam)]";
    }

    public Uni<VaultAwsIamAuth> login(VaultClient vaultClient) {
        SdkHttpFullRequest getCallerIdentityRequest;
        try {
            getCallerIdentityRequest = buildGetCallerIdentityRequest();
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("STS URI is not correct", ex);
        }
        AwsCredentials awsCredentials = getAwsCredentials();
        SdkHttpFullRequest signedRequest = signRequest(getCallerIdentityRequest, awsCredentials);

        VaultAwsIamAuthBody vaultRequestBody = buildVaultRequestBody(signedRequest);

        return vaultClient.post(opName("Login"), "auth/aws/login", null, vaultRequestBody,
                VaultAwsIamAuth.class);
    }

    private VaultAwsIamAuthBody buildVaultRequestBody(SdkHttpFullRequest signedRequest) {
      String headersString = headersToJsonString(signedRequest.headers());

      return new VaultAwsIamAuthBody(
                vaultConfigHolder.getVaultBootstrapConfig().authentication.awsIam.role,
                "POST",
                Base64String.from(signedRequest.getUri().toString()),
                Base64String.from(GET_CALLER_IDENTITY_REQUEST_BODY),
                Base64String.from(headersString)
        );
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

  private SdkHttpFullRequest signRequest(
            SdkHttpFullRequest getCallerIdentityRequest,
            AwsCredentials awsCredentials
    ) {
        Region region = Region.of(vaultConfigHolder.getVaultBootstrapConfig().authentication.awsIam.region);
        Aws4SignerParams params = Aws4SignerParams.builder()
                .awsCredentials(awsCredentials)
                .signingName("sts")
                .signingRegion(region)
                .build();
        return Aws4Signer.create().sign(getCallerIdentityRequest, params);
    }

    private AwsCredentials getAwsCredentials() {
        VaultAwsIamAuthenticationConfig awsIam = vaultConfigHolder.getVaultBootstrapConfig().authentication.awsIam;

        if (awsIam.awsAccessKey.isPresent() && awsIam.awsSecretKey.isPresent()) {
            return AwsBasicCredentials.create(awsIam.awsAccessKey.get(), awsIam.awsSecretKey.get());
        } else {
            try (DefaultCredentialsProvider defaultCredentialsProvider = DefaultCredentialsProvider.create()) {
                return defaultCredentialsProvider.resolveCredentials();
            }
        }
    }

    private SdkHttpFullRequest buildGetCallerIdentityRequest() throws URISyntaxException {
        SdkHttpFullRequest.Builder builder = SdkHttpFullRequest.builder()
                .method(SdkHttpMethod.POST)
                .uri(new URI(vaultConfigHolder.getVaultBootstrapConfig().authentication.awsIam.stsUrl))
                .appendHeader("Content-Type", "application/x-www-form-urlencoded; charset=utf-8")
                .appendHeader("Content-Length", String.valueOf(GET_CALLER_IDENTITY_REQUEST_BODY.length()))
                .contentStreamProvider(() -> new ByteArrayInputStream(
                        StringHelper.stringToBytes(GET_CALLER_IDENTITY_REQUEST_BODY)
                ));

        vaultConfigHolder.getVaultBootstrapConfig().authentication.awsIam.vaultServerId.ifPresent(
                serverId -> builder.appendHeader("X-Vault-AWS-IAM-Server-ID", serverId)
        );

        return builder.build();
    }
}
