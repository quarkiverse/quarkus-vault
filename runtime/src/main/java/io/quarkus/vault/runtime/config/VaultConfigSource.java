package io.quarkus.vault.runtime.config;

import static io.quarkus.vault.runtime.config.VaultCacheEntry.tryReturnLastKnownValue;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toMap;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.vault.VaultException;
import io.quarkus.vault.VaultKVSecretReactiveEngine;
import io.quarkus.vault.runtime.VaultIOException;
import io.quarkus.vault.runtime.client.Private;
import io.smallrye.mutiny.infrastructure.Infrastructure;

public class VaultConfigSource implements ConfigSource {

    private static final Logger log = Logger.getLogger(VaultConfigSource.class);

    private AtomicReference<VaultCacheEntry<Map<String, String>>> cache = new AtomicReference<>(null);
    private VaultRuntimeConfig vaultRuntimeConfig;
    private volatile boolean firstTime = true;

    public VaultConfigSource(VaultRuntimeConfig vaultRuntimeConfig) {
        this.vaultRuntimeConfig = vaultRuntimeConfig;
    }

    @Override
    public String getName() {
        return VaultRuntimeConfig.NAME;
    }

    @Override
    public int getOrdinal() {
        return vaultRuntimeConfig.configOrdinal();
    }

    /**
     * always return an empty map to protect from accidental properties logging
     *
     * @return empty map
     */
    @Override
    public Map<String, String> getProperties() {
        return emptyMap();
    }

    @Override
    public Set<String> getPropertyNames() {
        return Collections.emptySet();
    }

    @Override
    public String getValue(String propertyName) {
        return vaultRuntimeConfig.url().isPresent() ? getSecretConfig().get(propertyName) : null;
    }

    private Map<String, String> getSecretConfig() {

        VaultCacheEntry<Map<String, String>> cacheEntry = cache.get();
        if (cacheEntry != null && cacheEntry.youngerThan(vaultRuntimeConfig.secretConfigCachePeriod())) {
            return cacheEntry.getValue();
        }

        if (!Infrastructure.canCallerThreadBeBlocked()) {
            // running in a non blocking thread, best effort to return cached values if any
            return cacheEntry != null ? cacheEntry.getValue() : Collections.emptyMap();
        }

        Map<String, String> properties = new HashMap<>();

        if (firstTime) {
            log.debug("fetch secrets first time with attempts = " + vaultRuntimeConfig.mpConfigInitialAttempts());
            fetchSecretsFirstTime(properties);
            firstTime = false;
        } else {
            try {
                fetchSecrets(properties);
                log.debug("refreshed " + properties.size() + " properties from vault");
            } catch (RuntimeException e) {
                return tryReturnLastKnownValue(e, cacheEntry);
            }
        }

        cache.set(new VaultCacheEntry<>(properties));
        return properties;
    }

    private void fetchSecretsFirstTime(Map<String, String> properties) {
        VaultIOException last = null;
        for (int i = 0; i < vaultRuntimeConfig.mpConfigInitialAttempts(); i++) {
            try {
                if (i > 0) {
                    log.debug("retrying to fetch secrets");
                }
                fetchSecrets(properties);
                log.debug("loaded " + properties.size() + " properties from vault");
                return;
            } catch (VaultIOException e) {
                log.debug("attempt " + (i + 1) + " to fetch secrets from vault failed with: " + e);
                last = e;
            }
        }
        if (last == null) {
            throw new VaultException("unexpected");
        } else {
            throw last;
        }
    }

    private void fetchSecrets(Map<String, String> properties) {
        // default kv paths
        vaultRuntimeConfig.secretConfigKvPath().ifPresent(strings -> fetchSecrets(strings, null, properties));

        // prefixed kv paths
        vaultRuntimeConfig.secretConfigKvPathPrefix().forEach((key, value) -> fetchSecrets(value.paths(), key, properties));
    }

    private void fetchSecrets(List<String> paths, String prefix, Map<String, String> properties) {
        paths.forEach(path -> properties.putAll(fetchSecrets(path, prefix)));
    }

    private Map<String, String> fetchSecrets(String path, String prefix) {

        Map<String, Object> secretJson = getVaultKVSecretEngine().readSecretJson(path).await().indefinitely();

        // ignore list and map, honor null, get as string scalar types
        Map<String, String> secret = secretJson.entrySet().stream()
                .filter(entry -> isScalar(entry.getKey(), entry.getValue()))
                // we cannot use toMap because it does not allow null value because it uses Map.merge
                .collect(HashMap::new, (m, v) -> m.put(v.getKey(), toString(v.getValue())), HashMap::putAll);

        return prefixMap(secret, prefix);
    }

    private VaultKVSecretReactiveEngine getVaultKVSecretEngine() {
        return Arc.container().instance(VaultKVSecretReactiveEngine.class, Private.Literal.INSTANCE).get();
    }

    private Map<String, String> prefixMap(Map<String, String> map, String prefix) {
        return prefix == null
                ? map
                : map.entrySet().stream().collect(toMap(entry -> prefix + "." + entry.getKey(), Map.Entry::getValue));
    }

    private boolean isScalar(String key, Object value) {
        boolean isScalar = !(value instanceof List || value instanceof Map);
        if (!isScalar) {
            log.warnv("Unsupported type (List or Map) for key {0}, skipping", key);
        }
        return isScalar;
    }

    private String toString(Object value) {
        return value == null ? null : value.toString();
    }
}
