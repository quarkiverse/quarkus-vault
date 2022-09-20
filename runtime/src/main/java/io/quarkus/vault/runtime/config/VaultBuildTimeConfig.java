package io.quarkus.vault.runtime.config;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "quarkus.vault")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface VaultBuildTimeConfig {
    /**
     * Health check configuration.
     */
    HealthConfig health();

    /**
     * Dev services configuration.
     */
    @WithName("devservices")
    DevServicesConfig devServices();

    @Override
    String toString();
}
