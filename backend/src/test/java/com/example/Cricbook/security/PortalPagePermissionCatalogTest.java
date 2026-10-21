package com.example.Cricbook.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortalPagePermissionCatalogTest {

    @Test
    void exposesWalletPermissionsForPlayerAndOwnerPortals() {
        var walletPages = PortalPagePermissionCatalog.pages().stream()
                .filter(page -> page.label().equals("Wallet"))
                .toList();

        assertEquals(2, walletPages.size());
        assertTrue(walletPages.stream().anyMatch(page ->
                page.portal().equals("PLAYER")
                        && page.route().equals("/wallet")
                        && page.key().equals("PLAYER_WALLET")));
        assertTrue(walletPages.stream().anyMatch(page ->
                page.portal().equals("OWNER")
                        && page.route().equals("/owner/wallet")
                        && page.key().equals("OWNER_WALLET")));
    }
}
