package gg.jos.deathandtaxes.service;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/**
 * Formats currency values using a configurable number of decimal places.
 */
public final class CurrencyFormatter {
    private final DecimalFormat decimalFormat;

    /**
     * @param decimalPlaces number of decimal places the formatter should enforce
     */
    public CurrencyFormatter(int decimalPlaces) {
        StringBuilder pattern = new StringBuilder("#,##0");
        if (decimalPlaces > 0) {
            pattern.append('.');
            pattern.append("0".repeat(decimalPlaces));
        }

        decimalFormat = new DecimalFormat(pattern.toString(), DecimalFormatSymbols.getInstance());
        decimalFormat.setRoundingMode(RoundingMode.HALF_UP);
    }

    /**
     * Formats the provided amount using the configured decimal places.
     *
     * @param amount value to format
     * @return formatted currency string for display
     */
    public String format(double amount) {
        return decimalFormat.format(amount);
    }
}
