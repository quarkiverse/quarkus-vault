package io.quarkus.vault.runtime;

import io.quarkus.vault.VaultTransitSecretEngine;
import io.quarkus.vault.VaultTransitSecretEngineFactory;
import io.quarkus.vault.VaultTransitSecretReactiveEngine;
import io.quarkus.vault.runtime.client.VaultClient;
import io.quarkus.vault.runtime.client.secretengine.VaultInternalTransitSecretEngine;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class VaultTransitManagerFactory implements VaultTransitSecretEngineFactory {
    @Inject
    protected VaultClient vaultClient;
    @Inject
    protected VaultAuthManager vaultAuthManager;
    @Inject
    protected VaultConfigHolder vaultConfigHolder;
    @Inject
    protected VaultInternalTransitSecretEngine vaultInternalTransitSecretEngine;

    @Override
    public VaultTransitSecretEngine engine(String mount) {
        return new VaultTransitSecretEngine(reactiveEngine(mount));
    }

    @Override
    public VaultTransitSecretReactiveEngine reactiveEngine(String mount) {
        return new VaultTransitManager(vaultClient, mount, vaultAuthManager, vaultConfigHolder,
                vaultInternalTransitSecretEngine);
    }
}
