package io.quarkus.vault.deployment.devui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;

class VaultDevUIProcessorTest {

    @Test
    void cardHasNoRootTokenWithoutVaultDevServices() {
        CardPageBuildItem card = new VaultDevUIProcessor().vaultCard(List.of());

        assertEquals(2, card.getPages().size());
        assertTrue(card.getCardTexts().isEmpty());
    }

    @Test
    void cardShowsRootTokenWithVaultDevServices() {
        DevServicesResultBuildItem vault = DevServicesResultBuildItem.discovered()
                .feature("vault")
                .containerId("vault-container")
                .config(Map.of())
                .build();

        CardPageBuildItem card = new VaultDevUIProcessor().vaultCard(List.of(vault));

        assertEquals(2, card.getPages().size());
        assertEquals(1, card.getCardTexts().size());
        assertEquals("Root token", card.getCardTexts().get(0).getTitle());
        assertEquals("root", card.getCardTexts().get(0).getStaticText());
    }
}
