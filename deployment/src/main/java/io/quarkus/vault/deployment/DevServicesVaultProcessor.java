package io.quarkus.vault.deployment;

import static io.quarkus.devservices.common.ConfigureUtil.configureSharedServiceLabel;
import static io.quarkus.devservices.common.ContainerLocator.locateContainerWithLabels;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import org.jboss.logging.Logger;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.vault.VaultContainer;

import io.quarkus.deployment.IsDevServicesSupportedByLaunchMode;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.DevServicesComposeProjectBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.DevServicesSharedNetworkBuildItem;
import io.quarkus.deployment.builditem.DockerStatusBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.Startable;
import io.quarkus.deployment.dev.devservices.DevServicesConfig;
import io.quarkus.devservices.common.ComposeLocator;
import io.quarkus.devservices.common.ConfigureUtil;
import io.quarkus.devservices.common.ContainerLocator;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.quarkus.vault.runtime.VaultVersions;
import io.quarkus.vault.runtime.config.VaultBuildTimeConfig;

@BuildSteps(onlyIf = { IsDevServicesSupportedByLaunchMode.class, DevServicesConfig.Enabled.class })
public class DevServicesVaultProcessor {
    private static final Logger log = Logger.getLogger(DevServicesVaultProcessor.class);
    private static final String VAULT_IMAGE = "hashicorp/vault:" + VaultVersions.VAULT_TEST_VERSION;
    private static final String DEV_SERVICE_LABEL = "quarkus-dev-service-vault";
    private static final String DEV_SERVICE_TOKEN = "root";
    private static final int VAULT_EXPOSED_PORT = 8200;
    private static final String CONFIG_PREFIX = "quarkus.vault.";
    private static final String URL_CONFIG_KEY = CONFIG_PREFIX + "url";
    private static final String AUTH_CONFIG_PREFIX = CONFIG_PREFIX + "authentication.";
    private static final String CLIENT_TOKEN_CONFIG_KEY = AUTH_CONFIG_PREFIX + "client-token";
    private static final ContainerLocator vaultContainerLocator = locateContainerWithLabels(VAULT_EXPOSED_PORT,
            DEV_SERVICE_LABEL);

    @BuildStep
    public void startVaultContainers(
            LaunchModeBuildItem launchMode,
            DockerStatusBuildItem dockerStatusBuildItem,
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            List<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem,
            VaultBuildTimeConfig config,
            DevServicesConfig devServicesConfig,
            BuildProducer<DevServicesResultBuildItem> devServices) {

        io.quarkus.vault.runtime.config.DevServicesConfig vaultDevServicesConfig = config.devServices();

        if (!vaultDevServicesConfig.enabled()) {
            log.debug("Not starting devservices for Vault as it has been disabled in the config");
            return;
        }

        if (ConfigUtils.isPropertyPresent(URL_CONFIG_KEY)) {
            log.debug("Not starting devservices for default Vault client as url has been provided");
            return;
        }

        if (!dockerStatusBuildItem.isContainerRuntimeAvailable()) {
            log.warn("Please configure Vault URL or get a working docker instance");
            return;
        }

        boolean useSharedNetwork = DevServicesSharedNetworkBuildItem.isSharedNetworkRequired(devServicesConfig,
                devServicesSharedNetworkBuildItem);

        Optional<DevServicesResultBuildItem> discovered = vaultContainerLocator
                .locateContainer(vaultDevServicesConfig.serviceName(), vaultDevServicesConfig.shared(),
                        launchMode.getLaunchMode())
                .or(() -> ComposeLocator.locateContainer(composeProjectBuildItem,
                        List.of(vaultDevServicesConfig.imageName().orElse(VAULT_IMAGE)),
                        VAULT_EXPOSED_PORT, launchMode.getLaunchMode(), useSharedNetwork))
                .map(containerAddress -> DevServicesResultBuildItem.discovered()
                        .feature("vault")
                        .containerId(containerAddress.getId())
                        .config(Map.of(
                                URL_CONFIG_KEY, "http://" + containerAddress.getHost() + ":" + containerAddress.getPort(),
                                CLIENT_TOKEN_CONFIG_KEY, DEV_SERVICE_TOKEN))
                        .build());

        if (discovered.isPresent()) {
            devServices.produce(discovered.get());
            return;
        }

        DockerImageName dockerImageName = DockerImageName.parse(vaultDevServicesConfig.imageName().orElse(VAULT_IMAGE))
                .asCompatibleSubstituteFor(VAULT_IMAGE);
        devServices.produce(DevServicesResultBuildItem.<ConfiguredVaultContainer> owned()
                .feature("vault")
                .serviceName(vaultDevServicesConfig.serviceName())
                .serviceConfig(vaultDevServicesConfig.toString())
                .startable(() -> new ConfiguredVaultContainer(dockerImageName, vaultDevServicesConfig,
                        composeProjectBuildItem.getDefaultNetworkId(), useSharedNetwork, devServicesConfig.timeout())
                        .withSharedServiceLabel(launchMode.getLaunchMode(), vaultDevServicesConfig.serviceName()))
                .configProvider(Map.of(
                        URL_CONFIG_KEY, s -> "http://" + s.getHost() + ":" + s.getPort(),
                        CLIENT_TOKEN_CONFIG_KEY, s -> DEV_SERVICE_TOKEN))
                .build());
    }

    private static class ConfiguredVaultContainer extends VaultContainer<ConfiguredVaultContainer> implements Startable {
        private final OptionalInt fixedExposedPort;
        private final boolean useSharedNetwork;
        private final Optional<Duration> startupTimeout;
        private final String hostName;

        public ConfiguredVaultContainer(DockerImageName dockerImageName,
                io.quarkus.vault.runtime.config.DevServicesConfig devServicesConfig,
                String defaultNetworkId,
                boolean useSharedNetwork,
                Optional<Duration> startupTimeout) {
            super(dockerImageName);
            this.fixedExposedPort = devServicesConfig.port();
            this.useSharedNetwork = useSharedNetwork;
            this.startupTimeout = startupTimeout;
            withVaultToken(DEV_SERVICE_TOKEN);
            if (devServicesConfig.transitEnabled()) {
                withInitCommand("secrets enable transit");
            }
            if (devServicesConfig.pkiEnabled()) {
                withInitCommand("secrets enable pki");
            }
            devServicesConfig.initCommands().ifPresent(cmds -> cmds.forEach(this::withInitCommand));
            this.hostName = ConfigureUtil.configureNetwork(this, defaultNetworkId, useSharedNetwork, "vault");
        }

        public ConfiguredVaultContainer withSharedServiceLabel(LaunchMode launchMode, String serviceName) {
            return configureSharedServiceLabel(this, launchMode, DEV_SERVICE_LABEL, serviceName);
        }

        @Override
        protected void configure() {
            super.configure();
            if (useSharedNetwork) {
                return;
            }
            if (fixedExposedPort.isPresent()) {
                addFixedExposedPort(fixedExposedPort.getAsInt(), VAULT_EXPOSED_PORT);
            } else {
                addExposedPort(VAULT_EXPOSED_PORT);
            }
        }

        @Override
        public void start() {
            startupTimeout.ifPresent(this::withStartupTimeout);
            super.start();
        }

        public int getPort() {
            if (useSharedNetwork) {
                return VAULT_EXPOSED_PORT;
            }
            if (fixedExposedPort.isPresent()) {
                return fixedExposedPort.getAsInt();
            }
            return super.getFirstMappedPort();
        }

        @Override
        public String getHost() {
            return useSharedNetwork ? hostName : super.getHost();
        }

        @Override
        public String getConnectionInfo() {
            return getHost() + ":" + getPort();
        }

        @Override
        public void close() {
            super.close();
        }
    }
}