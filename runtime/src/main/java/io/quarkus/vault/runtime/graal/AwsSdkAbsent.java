package io.quarkus.vault.runtime.graal;

import java.util.function.BooleanSupplier;

/**
 * Detects, at native image build time, whether the (optional) AWS SDK is absent from the classpath.
 */
public class AwsSdkAbsent implements BooleanSupplier {

    private static final String AWS_CREDENTIALS_PROVIDER = "software.amazon.awssdk.auth.credentials.AwsCredentialsProvider";

    @Override
    public boolean getAsBoolean() {
        try {
            Class.forName(AWS_CREDENTIALS_PROVIDER, false, Thread.currentThread().getContextClassLoader());
            return false;
        } catch (ClassNotFoundException e) {
            return true;
        }
    }
}
