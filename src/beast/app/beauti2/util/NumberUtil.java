package beast.app.beauti2.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * @author Walter Xie
 */
public class NumberUtil {

    protected static DecimalFormat formatter = new DecimalFormat("0.####E0", new DecimalFormatSymbols(Locale.US));
    protected static DecimalFormat formatter2 = new DecimalFormat("####0.####", new DecimalFormatSymbols(Locale.US));

    public static String formatDecimal(double value, int maxFractionDigits1, int maxFractionDigits2) {
        formatter.setMaximumFractionDigits(maxFractionDigits1);
        formatter2.setMaximumFractionDigits(maxFractionDigits2);

        if (value > 0 && (Math.abs(value) < 0.001 || Math.abs(value) >= 100000.0)) {
            return formatter.format(value);
        } else {
            return formatter2.format(value);
        }
    }
}