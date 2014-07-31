package beast.util;


import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

/**
 * some useful methods
 *
 * @author Remco Bouckaert
 * @author Walter Xie
 */
public class OutputUtils {
    public final static String SPACE = " ";

    public static String format(String s) {
        return format(s, SPACE);
    }
    public static String format(String s, String space) {
        while (s.length() < 8) {
            s += " ";
        }
        return s + space;
    }

    public static String format(Double d) {
        if (Double.isNaN(d)) {
            return "NaN     ";
        }
        if (Math.abs(d) > 1e-4 || d == 0) {
            DecimalFormat f = new DecimalFormat("#0.######", new DecimalFormatSymbols(Locale.US));
            String sStr = f.format(d);
            if (sStr.length() > 8) {
                sStr = sStr.substring(0, 8);
            }
            while (sStr.length() < 8) {
                sStr += " ";
            }
            return sStr;
        } else {
            DecimalFormat f = new DecimalFormat("0.##E0", new DecimalFormatSymbols(Locale.US));
            String sStr = f.format(d);
            if (sStr.length() > 8) {
                String [] sStrs = sStr.split("E");
                sStr =  sStrs[0].substring(0, 8 - sStrs[1].length() - 1) + "E" + sStrs[1];
            }
            while (sStr.length() < 8) {
                sStr += " ";
            }
            return sStr;
        }
    }

    public static String toString(List list) {
        String s = "";
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) s += ", ";
            Object o = list.get(i);
            s += o.toString();
        }
        return s;
    }
}
