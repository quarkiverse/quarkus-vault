package io.quarkus.vault.deployment;

import org.jboss.jandex.DotName;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigBuilderBuildItem;
import io.quarkus.deployment.builditem.SslNativeConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;
import io.quarkus.vault.VaultAppRoleAuthService;
import io.quarkus.vault.VaultKVSecretEngine;
import io.quarkus.vault.VaultKubernetesAuthService;
import io.quarkus.vault.VaultPKISecretEngine;
import io.quarkus.vault.VaultSystemBackendEngine;
import io.quarkus.vault.VaultTOTPSecretEngine;
import io.quarkus.vault.VaultTransitSecretEngine;
import io.quarkus.vault.client.common.VaultModel;
import io.quarkus.vault.runtime.*;
import io.quarkus.vault.runtime.client.VaultClientProducer;
import io.quarkus.vault.runtime.config.VaultBuildTimeConfig;
import io.quarkus.vault.runtime.config.VaultConfigSourceFactoryBuilder;
import io.quarkus.vault.runtime.health.VaultHealthCheck;

public class VaultProcessor {

    private static final String FEATURE = "vault";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void build(
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            CombinedIndexBuildItem combinedIndexBuildItem,
            SslNativeConfigBuildItem sslNativeConfig,
            BuildProducer<ExtensionSslNativeSupportBuildItem> sslNativeSupport) {

        final String[] modelClasses = combinedIndexBuildItem.getIndex()
                .getAllKnownImplementors(DotName.createSimple(VaultModel.class.getName()))
                .stream()
                .map(c -> c.name().toString())
                .toArray(String[]::new);
        reflectiveClasses.produce(ReflectiveClassBuildItem.weakClass(modelClasses));

        sslNativeSupport.produce(new ExtensionSslNativeSupportBuildItem(Feature.VAULT));
    }

    @BuildStep
    AdditionalBeanBuildItem registerAdditionalBeans() {
        return new AdditionalBeanBuildItem.Builder()
                .setUnremovable()
                .addBeanClass(VaultCredentialsProvider.class)
                .addBeanClass(VaultKvManager.class)
                .addBeanClass(VaultKVSecretEngine.class)
                .addBeanClass(VaultTransitManager.class)
                .addBeanClass(VaultTransitSecretEngine.class)
                .addBeanClass(VaultTOTPManager.class)
                .addBeanClass(VaultTOTPSecretEngine.class)
                .addBeanClass(VaultSystemBackendManager.class)
                .addBeanClass(VaultSystemBackendEngine.class)
                .addBeanClass(VaultAppRoleAuthManager.class)
                .addBeanClass(VaultAppRoleAuthService.class)
                .addBeanClass(VaultKubernetesAuthManager.class)
                .addBeanClass(VaultKubernetesAuthService.class)
                .addBeanClass(VaultClientProducer.class)
                .addBeanClass(VaultDynamicCredentialsManager.class)
                .addBeanClass(VaultConfigHolder.class)
                .addBeanClass(VaultPKIManager.class)
                .addBeanClass(VaultPKISecretEngine.class)
                .addBeanClass(VaultPKIManagerFactory.class)
                .build();
    }

    @BuildStep
    void vaultConfigFactory(BuildProducer<RunTimeConfigBuilderBuildItem> runTimeConfigBuilder) {
        runTimeConfigBuilder.produce(new RunTimeConfigBuilderBuildItem(VaultConfigSourceFactoryBuilder.class.getName()));
    }

    @BuildStep
    HealthBuildItem addHealthCheck(VaultBuildTimeConfig config) {
        return new HealthBuildItem(VaultHealthCheck.class.getName(), config.health().enabled());
    }
}
