package gg.jos.deathandtaxes.config;

/**
 * Available strategies for computing the death tax.
 */
public enum TaxMode {
    PERCENTAGE,
    FIXED;

    /**
     * Attempts to resolve a configuration value into a {@link TaxMode} instance.
     *
     * @param value raw configuration value
     * @return matching {@link TaxMode}, or {@code null} if no match exists
     */
    static TaxMode fromConfigValue(String value) {
        if (value == null) {
            return null;
        }

        try {
            return TaxMode.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /**
     * returning the provided default when
     * the value is missing or invalid for percents & values
     *
     * @param value raw configuration value
     * @param defaultValue value to return when parsing fails
     * @return parsed {@link TaxMode} or {@code defaultValue}
     */
    public static TaxMode fromString(String value, TaxMode defaultValue) {
        TaxMode parsed = fromConfigValue(value);
        return parsed != null ? parsed : defaultValue;
    }
}
