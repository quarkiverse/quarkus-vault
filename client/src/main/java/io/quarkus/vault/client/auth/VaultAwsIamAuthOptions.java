package io.quarkus.vault.client.auth;

import static io.quarkus.vault.client.api.VaultAuthAccessor.DEFAULT_AWS_MOUNT_PATH;
import static io.quarkus.vault.client.auth.VaultCachingTokenProvider.DEFAULT_RENEW_GRACE_PERIOD;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;

public class VaultAwsIamAuthOptions extends VaultAuthOptions {

    public static final String DEFAULT_STS_URL = "https://sts.amazonaws.com";

    public static class Builder {
        private String mountPath = DEFAULT_AWS_MOUNT_PATH;
        private String role;
        private Region region;
        private String stsUrl = DEFAULT_STS_URL;
        private String vaultServerId;
        private AwsCredentialsProvider credentialsProvider;
        private Duration cachingRenewGracePeriod = DEFAULT_RENEW_GRACE_PERIOD;

        public Builder mountPath(String mountPath) {
            this.mountPath = mountPath;
            return this;
        }

        public Builder role(String role) {
            this.role = role;
            return this;
        }

        public Builder region(Region region) {
            this.region = region;
            return this;
        }

        public Builder region(String region) {
            this.region = region != null ? Region.of(region) : null;
            return this;
        }

        public Builder stsUrl(String stsUrl) {
            this.stsUrl = stsUrl;
            return this;
        }

        public Builder vaultServerId(String vaultServerId) {
            this.vaultServerId = vaultServerId;
            return this;
        }

        public Builder credentialsProvider(AwsCredentialsProvider credentialsProvider) {
            this.credentialsProvider = credentialsProvider;
            return this;
        }

        public Builder staticCredentials(String accessKey, String secretKey) {
            this.credentialsProvider = StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
            return this;
        }

        public Builder caching(Duration cachingRenewGracePeriod) {
            this.cachingRenewGracePeriod = cachingRenewGracePeriod;
            return this;
        }

        public Builder noCaching() {
            this.cachingRenewGracePeriod = Duration.ZERO;
            return this;
        }

        public VaultAwsIamAuthOptions build() {
            return new VaultAwsIamAuthOptions(this);
        }
    }

    public final String mountPath;
    public final String role;
    public final Region region;
    public final String stsUrl;
    public final Optional<String> vaultServerId;
    public final AwsCredentialsProvider credentialsProvider;

    private VaultAwsIamAuthOptions(Builder builder) {
        super(builder.cachingRenewGracePeriod);
        this.mountPath = Objects.requireNonNull(builder.mountPath);
        this.role = Objects.requireNonNull(builder.role, "role is required for AWS IAM authentication");
        this.region = Objects.requireNonNull(builder.region, "region is required for AWS IAM authentication");
        this.stsUrl = Objects.requireNonNull(builder.stsUrl);
        this.vaultServerId = Optional.ofNullable(builder.vaultServerId);
        this.credentialsProvider = builder.credentialsProvider != null ? builder.credentialsProvider
                : DefaultCredentialsProvider.create();
    }

    public static Builder builder() {
        return new Builder();
    }

}
