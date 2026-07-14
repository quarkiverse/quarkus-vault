package io.quarkus.vault.runtime.config;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface DevServicesConfig {
    /**
     * If DevServices has been explicitly enabled or disabled. DevServices is generally enabled
     * by default, unless there is an existing configuration present.
     * <p>
     * When DevServices is enabled Quarkus will attempt to automatically configure and start
     * a vault instance when running in Dev or Test mode and when Docker is running.
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * The container image name to use, for container based DevServices providers.
     */
    Optional<String> imageName();

    /**
     * Indicates if the Vault instance managed by Quarkus Dev Services is shared.
     * When shared, Quarkus looks for running containers using label-based service discovery.
     * If a matching container is found, it is used, and so a second one is not started.
     * Otherwise, Dev Services for Vault starts a new container.
     * <p>
     * The discovery uses the {@code quarkus-dev-service-vault} label.
     * The value is configured using the {@code service-name} property.
     * <p>
     * Container sharing is only used in dev mode.
     */
    @WithDefault("true")
    boolean shared();

    /**
     * The value of the {@code quarkus-dev-service-vault} label attached to the started container.
     * This property is used when {@code shared} is set to {@code true}.
     * In this case, before starting a container, Dev Services for Vault looks for a container with the
     * {@code quarkus-dev-service-vault} label
     * set to the configured value. If found, it will use this container instead of starting a new one. Otherwise it
     * starts a new container with the {@code quarkus-dev-service-vault} label set to the specified value.
     * <p>
     * This property is used when you need multiple shared Vault instances.
     */
    @WithDefault("vault")
    String serviceName();

    /**
     * Optional fixed port the dev service will listen to.
     * <p>
     * If not defined, the port will be chosen randomly.
     */
    OptionalInt port();

    /**
     * Should the Transit secret engine be enabled
     */
    @WithDefault("false")
    boolean transitEnabled();

    /**
     * Should the PKI secret engine be enabled
     */
    @WithDefault("false")
    boolean pkiEnabled();

    /**
     * Custom container initialization commands.
     * <p>
     * Each command is executed inside the started container with the Vault CLI, once the server
     * is up and running. Since commands are passed directly to the {@code vault} executable,
     * the {@code vault} prefix must be omitted. For instance, to create a Transit key,
     * use {@code write -f transit/keys/my-key} instead of {@code vault write -f transit/keys/my-key}.
     * <p>
     * Commands are executed in the order they are defined, and are authenticated with the root token.
     */
    Optional<List<String>> initCommands();

    @Override
    String toString();
}
