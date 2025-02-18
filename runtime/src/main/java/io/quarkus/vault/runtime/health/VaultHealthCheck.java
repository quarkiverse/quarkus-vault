package io.quarkus.vault.runtime.health;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

import io.quarkus.vault.client.VaultClient;
import io.quarkus.vault.runtime.config.VaultBuildTimeConfig;

@Readiness
@Singleton
public class VaultHealthCheck implements HealthCheck {

    @Inject
    VaultClient client;

    @Inject
    VaultBuildTimeConfig buildTimeConfig;

    @Override
    public HealthCheckResponse call() {

        final HealthCheckResponseBuilder builder = HealthCheckResponse.named("Vault connection health check");

        try {
            boolean isStandByOk = this.buildTimeConfig.health().standByOk();
            boolean isPerfStandByOk = this.buildTimeConfig.health().performanceStandByOk();

            var status = client.sys().health().status(isStandByOk, isPerfStandByOk)
                    .toCompletableFuture().get();

            switch (status) {
                case INITIALIZED_UNSEALED_ACTIVE:
                    builder.up();
                    break;
                case UNSEALED_STANDBY:
                    builder.down().withData("reason", "Unsealed and Standby");
                    break;
                case RECOVERY_REPLICATION_SECONDARY_ACTIVE:
                    builder.down().withData("reason", "Disaster recovery mode replication secondary and active");
                    break;
                case PERFORMANCE_STANDBY:
                    builder.down().withData("reason", "Performance standby");
                    break;
                case NOT_INITIALIZED:
                    builder.down().withData("reason", "Not initialized");
                    break;
                case SEALED:
                    builder.down().withData("reason", "Sealed");
                    break;
            }

            return builder.build();

        } catch (Exception e) {
            return builder.down().withData("reason", e.getMessage()).build();
        }
    }
}
