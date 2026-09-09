package gg.jos.deathandtaxes.config;

/**
 * Destination that receives successfully collected death taxes.
 */
public record TaxAccount(boolean enabled, String name, boolean refundOnDepositFailure) {

    /**
     * @return whether tax-account deposits are enabled and have a recipient
     */
    public boolean isEnabled() {
        return enabled && !name.isBlank();
    }
}
