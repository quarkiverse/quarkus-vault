package io.quarkus.vault.runtime.config;

import java.util.Collections;
import java.util.List;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.vault.runtime.VaultConfigHolder;
import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory.ConfigurableConfigSourceFactory;

public class VaultConfigSourceFactory implements ConfigurableConfigSourceFactory<VaultRuntimeConfig> {
    @Override
    public Iterable<ConfigSource> getConfigSources(final ConfigSourceContext context, final VaultRuntimeConfig config) {
        if (config.url().isPresent()) {
            ArcContainer container = Arc.container();
            if (container != null) {
                container.instance(VaultConfigHolder.class).get().setVaultRuntimeConfig(config);
            }
            return List.of(new VaultConfigSource(config));
        } else {
            return Collections.emptyList();
        }
    }
}
