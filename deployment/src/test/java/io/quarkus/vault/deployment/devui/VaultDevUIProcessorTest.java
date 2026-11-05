package io.quarkus.vault.deployment.devui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.CardText;

class VaultDevUIProcessorTest {

    @Test
    void cardExplainsThatNoVaultIsConfigured() {
        CardPageBuildItem card = new VaultDevUIProcessor().vaultCard(List.of());

        assertEquals(List.of("Config sources"), titles(card));
        assertEquals(List.of("Vault"), card.getCardTexts().stream().map(CardText::getTitle).toList());
    }

    @Test
    void cardShowsRootTokenForStartedVaultDevServices() {
        DevServicesResultBuildItem vault = DevServicesResultBuildItem.owned()
                .feature("vault")
                .serviceName("vault")
                .startable(() -> null)
                .configProvider(Map.of())
                .build();

        CardPageBuildItem card = new VaultDevUIProcessor().vaultCard(List.of(vault));

        assertEquals(List.of("Vault UI", "Config sources"), titles(card));
        assertEquals(1, card.getCardTexts().size());
        assertEquals("Root token", card.getCardTexts().get(0).getTitle());
        assertEquals("root", card.getCardTexts().get(0).getStaticText());
    }

    @Test
    void cardHidesRootTokenForDiscoveredVaultContainer() {
        DevServicesResultBuildItem vault = DevServicesResultBuildItem.discovered()
                .feature("vault")
                .containerId("vault-container")
                .config(Map.of())
                .build();

        CardPageBuildItem card = new VaultDevUIProcessor().vaultCard(List.of(vault));

        assertEquals(List.of("Vault UI", "Config sources"), titles(card));
        assertTrue(card.getCardTexts().isEmpty());
    }

    private static List<String> titles(CardPageBuildItem card) {
        return card.getPages().stream().map(page -> page.build().getTitle()).toList();
    }
}
