/*
 * NumberFormatter.java
 *
 * Copyright (C) 2002-2006 Alexei Drummond and Andrew Rambaut
 *
 * This file is part of BEAST.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership and licensing.
 *
 * BEAST is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 *  BEAST is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with BEAST; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA  02110-1301  USA
 */

package test.beast.beast2vs1.trace;

import java.text.DecimalFormat;

/**
 * The world's most intelligent number formatter with the following features :-)
 * <p/>
 * It guarantee's the display of a user-specified number of significant figures, sf <BR>
 * It displays decimal format for numbers with absolute values between 1 and 10^(sf-1) <BR>
 * It displays scientific notation for all other numbers (i.e. really big and really small absolute values) <BR>
 * <b>note</b>: Its display integers for doubles with integer value <BR>
 *
 * @author Alexei Drummond
 */
public class NumberFormatter {

    private int sf;
    private double upperCutoff;
    private double[] cutoffTable;
    private final DecimalFormat decimalFormat = new DecimalFormat();
    private DecimalFormat scientificFormat = null;
    private boolean isPadding = false;
    private int fieldWidth;


    public NumberFormatter(int sf) {
        setSignificantFigures(sf);
    }

    public NumberFormatter(int sf, int fieldWidth) {
        setSignificantFigures(sf);
        setPadding(true);
        setFieldWidth(fieldWidth);
    }

    public void setSignificantFigures(int sf) {
        this.sf = sf;
        upperCutoff = Math.pow(10, sf - 1);
        cutoffTable = new double[sf];
        long num = 10;
        for (int i = 0; i < cutoffTable.length; i++) {
            cutoffTable[i] = num;
            num *= 10;
        }
        decimalFormat.setMinimumIntegerDigits(1);
        decimalFormat.setMaximumFractionDigits(sf - 1);
        decimalFormat.setMinimumFractionDigits(sf - 1);
        decimalFormat.setGroupingUsed(false);
        scientificFormat = new DecimalFormat(getScientificPattern(sf));
        fieldWidth = sf;
    }

    public void setPadding(boolean padding) {
        isPadding = padding;
    }

    public void setFieldWidth(int fw) {
        if (fw < sf + 4) throw new IllegalArgumentException();
        fieldWidth = fw;
    }

    public int getFieldWidth() {
        return fieldWidth;
    }

    public String formatToFieldWidth(String s, int fieldWidth) {
        int size = fieldWidth - s.length();
        StringBuffer buffer = new StringBuffer(s);
        for (int i = 0; i < size; i++) {
            buffer.append(' ');
        }
        return buffer.toString();
    }

    /**
     * @param value             value
     * @param numFractionDigits numFractionDigits
     * @return the given value formatted to have exactly then number of
     *         fraction digits specified.
     */
    public String formatDecimal(double value, int numFractionDigits) {

        decimalFormat.setMaximumFractionDigits(numFractionDigits);
        decimalFormat.setMinimumFractionDigits(Math.min(numFractionDigits, 1));
        return decimalFormat.format(value);
    }

    /**
     * This method formats a number 'nicely': <BR>
     * It guarantee's the display of a user-specified total significant figures, sf <BR>
     * It displays decimal format for numbers with absolute values between 1 and 10^(sf-1) <BR>
     * It displays scientific notation for all other numbers (i.e. really big and really small absolute values) <BR>
     * <b>note</b>: Its display integers for doubles with integer value <BR>
     *
     * @param value value
     * @return a nicely formatted number.
     */
    public String format(double value) {

        StringBuffer buffer = new StringBuffer();

        double absValue = Math.abs(value);

        if ((absValue > upperCutoff) || (absValue < 0.1 && absValue != 0.0)) {
            buffer.append(scientificFormat.format(value));
        } else {
            int numFractionDigits = 0;
            if (value != (int) value) {
                numFractionDigits = getNumFractionDigits(value);
            }
            buffer.append(formatDecimal(value, numFractionDigits));
        }

        if (isPadding) {
            int size = fieldWidth - buffer.length();
            for (int i = 0; i < size; i++) {
                buffer.append(' ');
            }
        }
        return buffer.toString();
    }

    private int getNumFractionDigits(double value) {
        value = Math.abs(value);
        for (int i = 0; i < cutoffTable.length; i++) {
            if (value < cutoffTable[i]) return sf - i - 1;
        }
        return sf - 1;
    }

    private String getScientificPattern(int sf) {
        String pattern = "0.";
        for (int i = 0; i < sf - 1; i++) {
            pattern += "#";
        }
        pattern += "E0";
        return pattern;
    }
}
