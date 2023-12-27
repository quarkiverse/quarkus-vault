package io.quarkus.vault.client;

import java.time.InstantSource;

public class VaultClientConfiguration {

    public static final InstantSource instantSource = InstantSource.system();

    private VaultClientConfiguration() {
    }
}
