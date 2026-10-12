package io.quarkus.vault.deployment.devui;

import java.util.List;

import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.devjsonrpc.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.CardText;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.quarkus.vault.deployment.DevServicesVaultProcessor;
import io.quarkus.vault.runtime.dev.ui.VaultJsonRPCService;

public class VaultDevUIProcessor {

    private static final String URL_CONFIG_KEY = "quarkus.vault.url";

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    CardPageBuildItem vaultCard(List<DevServicesResultBuildItem> devServicesResults) {
        CardPageBuildItem card = new CardPageBuildItem();
        List<DevServicesResultBuildItem> vaultDevServices = devServicesResults.stream()
                .filter(result -> "vault".equals(result.getName()))
                .toList();
        if (ConfigUtils.isPropertyPresent(URL_CONFIG_KEY) || !vaultDevServices.isEmpty()) {
            card.addPage(Page.externalPageBuilder("Vault UI")
                    .icon("font-awesome-solid:lock")
                    .dynamicUrlJsonRPCMethodName("getVaultUiUrl")
                    .dynamicLabelJsonRPCMethodName("getServerVersion")
                    .doNotEmbed());
        } else {
            card.addText(CardText.textBuilder()
                    .title("Vault")
                    .staticText("not configured: set " + URL_CONFIG_KEY + " or enable Dev Services")
                    .build());
        }
        if (vaultDevServices.stream().anyMatch(DevServicesResultBuildItem::isStartable)) {
            card.addText(CardText.textBuilder()
                    .title("Root token")
                    .staticText(DevServicesVaultProcessor.DEV_SERVICE_TOKEN)
                    .build());
        }
        card.addPage(Page.webComponentPageBuilder()
                .title("Config sources")
                .icon("font-awesome-solid:key")
                .componentLink("qwc-vault-config-sources.js")
                .dynamicLabelJsonRPCMethodName("getConfigSourcesLabel"));
        return card;
    }

    @BuildStep
    JsonRPCProvidersBuildItem vaultJsonRpcProvider() {
        return new JsonRPCProvidersBuildItem(VaultJsonRPCService.class);
    }
}
