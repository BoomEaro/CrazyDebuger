package ru.boomearo.crazydebuger.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

public class NumberUtils {
    static final NumberFormat PRETTY_FORMAT = NumberFormat.getInstance(Locale.US);
    static {
        PRETTY_FORMAT.setRoundingMode(RoundingMode.FLOOR);
        PRETTY_FORMAT.setGroupingUsed(true);
        PRETTY_FORMAT.setMinimumFractionDigits(2);
        PRETTY_FORMAT.setMaximumFractionDigits(2);
    }

    public static String formatAsPrettyCurrency(BigDecimal value) {
        String str = PRETTY_FORMAT.format(value);
        if (str.endsWith(".00")) {
            str = str.substring(0, str.length() - 3);
        }
        return str;
    }

    public static String displayCurrency(final double value) {
		BigDecimal bd = new BigDecimal(value);
        String currency = formatAsPrettyCurrency(bd);
        if (bd.signum() < 0) {
            currency = currency.substring(1);
        }
        return currency;
    }

}