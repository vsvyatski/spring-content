package org.springframework.content.encryption;

import org.testcontainers.vault.VaultContainer;

/**
 * Lazy singleton Vault Testcontainers instance for encryption ITs.
 *
 * @author Vladimir Svyatski
 */
public final class VaultContainerSupport {

    private static volatile VaultContainer vaultContainer;

    private VaultContainerSupport() {
    }

    public static VaultContainer getVaultContainer() {
        VaultContainer existing = vaultContainer;
        if (existing != null) {
            return existing;
        }
        synchronized (VaultContainerSupport.class) {
            if (vaultContainer == null) {
                vaultContainer = startVault();
            }
            return vaultContainer;
        }
    }

    @SuppressWarnings("resource")
    private static VaultContainer startVault() {
        VaultContainer container = new VaultContainer("hashicorp/vault:1.1.3")
                .withVaultToken("my-root-token")
                .withInitCommand("secrets enable transit");
        container.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (container.isRunning()) {
                container.stop();
            }
        }));
        return container;
    }
}