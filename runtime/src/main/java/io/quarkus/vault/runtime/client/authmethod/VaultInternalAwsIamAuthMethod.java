package io.quarkus.vault.runtime.client.authmethod;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.stream.Collectors;

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

@Singleton
public class VaultInternalAwsIamAuthMethod extends VaultInternalBase {

  private static final String GET_CALLER_IDENTITY_REQUEST_BODY = "Action=GetCallerIdentity&Version=2011-06-15";
  @Inject
  private VaultConfigHolder vaultConfigHolder;

  @Override
  protected String opNamePrefix() {
    return super.opNamePrefix() + " [AUTH (aws iam)]";
  }

  /**
   * curl -X POST "http://127.0.0.1:8200/v1/auth/aws/login" -d '{
   *   "role":"dev",
   *   "iam_http_request_method": "POST",
   *   "iam_request_url": "aHR0cHM6Ly9zdHMuYW1hem9uYXdzLmNvbS8=",
   *   "iam_request_body": "QWN0aW9uPUdldENhbGxlcklkZW50aXR5JlZlcnNpb249MjAxMS0wNi0xNQ==",
   *   "iam_request_headers": "eyJDb250ZW50LUxlbmd0aCI6IFsiNDMiXSwgIlVzZXItQWdlbnQiOiBbImF3cy1zZGstZ28vMS40LjEyIChnbzEuNy4xOyBsaW51eDsgYW1kNjQpIl0sICJYLVZhdWx0LUFXU0lBTS1TZXJ2ZXItSWQiOiBbInZhdWx0LmV4YW1wbGUuY29tIl0sICJYLUFtei1EYXRlIjogWyIyMDE2MDkzMFQwNDMxMjFaIl0sICJDb250ZW50LVR5cGUiOiBbImFwcGxpY2F0aW9uL3gtd3d3LWZvcm0tdXJsZW5jb2RlZDsgY2hhcnNldD11dGYtOCJdLCAiQXV0aG9yaXphdGlvbiI6IFsiQVdTNC1ITUFDLVNIQTI1NiBDcmVkZW50aWFsPWZvby8yMDE2MDkzMC91cy1lYXN0LTEvc3RzL2F3czRfcmVxdWVzdCwgU2lnbmVkSGVhZGVycz1jb250ZW50LWxlbmd0aDtjb250ZW50LXR5cGU7aG9zdDt4LWFtei1kYXRlO3gtdmF1bHQtc2VydmVyLCBTaWduYXR1cmU9YTY5ZmQ3NTBhMzQ0NWM0ZTU1M2UxYjNlNzlkM2RhOTBlZWY1NDA0N2YxZWI0ZWZlOGZmYmM5YzQyOGMyNjU1YiJdfQ==" }'
   *
   *
   *   Config PARAMS:
   *   - region
   *   - sts url
   *   - X-Vault-AWS-IAM-Server-ID
   */
  public Uni<VaultAwsIamAuth> login(final VaultClient vaultClient) {
    final SdkHttpFullRequest getCallerIdentityRequest;
    try {
      getCallerIdentityRequest = buildGetCallerIdentityRequest();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
    final AwsCredentials creds = getAwsCredentials();
    final SdkHttpFullRequest signedRequest = signRequest(getCallerIdentityRequest, creds);

    final VaultAwsIamAuthBody vaultRequestBody = buildVaultRequestBody(signedRequest);

    return vaultClient.post(opName("Login"), "auth/aws/login", null, vaultRequestBody,
      VaultAwsIamAuth.class);
  }

  private VaultAwsIamAuthBody buildVaultRequestBody(final SdkHttpFullRequest signedRequest) {
    final String headersString = "{"
      + signedRequest.headers().entrySet().stream()
      .map(entry -> "\"" + entry.getKey() + "\":["
        + entry.getValue().stream().map(value -> "\"" + value + "\"").collect(Collectors.joining(","))
        + "]")
      .collect(Collectors.joining(","))
      + "}";

    return new VaultAwsIamAuthBody(
      vaultConfigHolder.getVaultBootstrapConfig().authentication.awsIam.role,
      "POST",
      Base64String.from(signedRequest.getUri().toString()),
      Base64String.from(GET_CALLER_IDENTITY_REQUEST_BODY),
      Base64String.from(headersString)
    );
  }

  private SdkHttpFullRequest signRequest(
    final SdkHttpFullRequest getCallerIdentityRequest,
    final AwsCredentials creds
  ) {
    final Region region = Region.of(vaultConfigHolder.getVaultBootstrapConfig().authentication.awsIam.region);
    Aws4SignerParams params = Aws4SignerParams.builder()
      .awsCredentials(creds)
      .signingName("sts")
      .signingRegion(region)
      .build();
    return Aws4Signer.create().sign(getCallerIdentityRequest, params);
  }

  private AwsCredentials getAwsCredentials() {
    final VaultAwsIamAuthenticationConfig awsIam = vaultConfigHolder.getVaultBootstrapConfig().authentication.awsIam;

    if (awsIam.awsAccessKey.isPresent() && awsIam.awsSecretKey.isPresent()) {
      return AwsBasicCredentials.create(awsIam.awsAccessKey.get(), awsIam.awsSecretKey.get());
    } else {
      try (DefaultCredentialsProvider defaultCredentialsProvider = DefaultCredentialsProvider.create()) {
        return defaultCredentialsProvider.resolveCredentials();
      }
    }
  }

  private SdkHttpFullRequest buildGetCallerIdentityRequest() throws URISyntaxException {
    final SdkHttpFullRequest.Builder builder = SdkHttpFullRequest.builder()
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

    SdkHttpFullRequest request = builder.build();
    // log request
    return request;
  }
}
