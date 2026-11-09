package org.springframework.content.encryption;

import org.testcontainers.vault.VaultContainer;

public class VaultContainerSupport {

    private static VaultContainer vaultContainer = null;

    public static VaultContainer getVaultContainer() {

        if (vaultContainer == null) {
            vaultContainer = new VaultContainer("hashicorp/vault:1.1.3")
                    .withVaultToken("my-root-token")
                    .withInitCommand("secrets enable transit");

            vaultContainer.start();
        }

        return vaultContainer;
    }
}
