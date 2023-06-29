package io.quarkus.vault.runtime.client.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.vault.runtime.Base64String;
import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultAwsIamAuthBody implements VaultModel {

    public String role;

    @JsonProperty("iam_http_request_method")
    public String requestMethod;

    @JsonProperty("iam_request_url")
    public Base64String requestUrl;

    @JsonProperty("iam_request_body")
    public Base64String requestBody;

    @JsonProperty("iam_request_headers")
    public Base64String requestHeaders;

    public VaultAwsIamAuthBody(
            final String role,
            final String requestMethod,
            final Base64String requestUrl,
            final Base64String requestBody,
            final Base64String requestHeaders
    ) {
        this.role = role;
        this.requestMethod = requestMethod;
        this.requestUrl = requestUrl;
        this.requestBody = requestBody;
        this.requestHeaders = requestHeaders;
    }
}
