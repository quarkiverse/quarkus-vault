package io.quarkus.vault.runtime.graal;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.quarkus.vault.client.VaultClient;
import io.quarkus.vault.client.VaultException;
import io.quarkus.vault.runtime.client.VaultAwsIamAuthConfigurator;
import io.quarkus.vault.runtime.config.VaultRuntimeConfig;

/**
 * Replaces {@link VaultAwsIamAuthConfigurator#configure} when the AWS SDK is not on the classpath.
 * <p>
 * The Vault client producer is always reachable, so without this substitution the native image
 * closed-world analysis would follow the AWS IAM branch into the AWS SDK and fail to link for every
 * application, including those that never use AWS IAM authentication. Substituting the method body
 * removes the only reference to the AWS SDK; applications that do use AWS IAM authentication put the
 * SDK on the classpath, the predicate is then false, and the real implementation is kept.
 */
@TargetClass(value = VaultAwsIamAuthConfigurator.class, onlyWith = AwsSdkAbsent.class)
final class Target_VaultAwsIamAuthConfigurator {

    @Substitute
    public static void configure(VaultClient.Builder builder, VaultRuntimeConfig config) {
        throw new VaultException("AWS IAM authentication requires the AWS SDK, which was not found on the classpath. " +
                "Add the software.amazon.awssdk:auth and software.amazon.awssdk:regions dependencies " +
                "(plus an HTTP client such as software.amazon.awssdk:url-connection-client when not using " +
                "static credentials) to use quarkus.vault.authentication.aws-iam.");
    }
}
