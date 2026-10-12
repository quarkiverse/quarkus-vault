package io.quarkus.vault.deployment.devui;

import java.util.List;

import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.devjsonrpc.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.CardText;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.vault.deployment.DevServicesVaultProcessor;
import io.quarkus.vault.runtime.dev.ui.VaultJsonRPCService;

public class VaultDevUIProcessor {

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    CardPageBuildItem vaultCard(List<DevServicesResultBuildItem> devServicesResults) {
        CardPageBuildItem card = new CardPageBuildItem();
        card.addPage(Page.externalPageBuilder("Vault UI")
                .icon("font-awesome-solid:lock")
                .dynamicUrlJsonRPCMethodName("getVaultUiUrl")
                .dynamicLabelJsonRPCMethodName("getServerVersion")
                .doNotEmbed());
        if (isProvidedByDevServices(devServicesResults)) {
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

    private static boolean isProvidedByDevServices(List<DevServicesResultBuildItem> devServicesResults) {
        return devServicesResults.stream().anyMatch(result -> "vault".equals(result.getName()));
    }

    @BuildStep
    JsonRPCProvidersBuildItem vaultJsonRpcProvider() {
        return new JsonRPCProvidersBuildItem(VaultJsonRPCService.class);
    }
}
