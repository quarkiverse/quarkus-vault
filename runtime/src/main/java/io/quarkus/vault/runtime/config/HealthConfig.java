package io.quarkus.vault.runtime.config;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface HealthConfig {
    /**
     * Whether or not an health check is published in case the smallrye-health extension is present.
     */
    @WithDefault("false")
    boolean enabled();

    /**
     * Specifies if being a standby should still return the active status code instead of the standby status code.
     */
    @WithDefault("false")
    boolean standByOk();

    /**
     * Specifies if being a performance standby should still return the active status code instead of the performance
     * standby
     * status code.
     */
    @WithDefault("false")
    boolean performanceStandByOk();

    @Override
    String toString();
}
