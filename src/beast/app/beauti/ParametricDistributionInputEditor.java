package beast.app.beauti;


import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.apache.commons.math.MathException;

import beast.app.draw.BEASTObjectInputEditor;
import beast.core.BEASTInterface;
import beast.core.Input;
import beast.evolution.tree.TreeDistribution;
import beast.math.distributions.MRCAPrior;
import beast.math.distributions.ParametricDistribution;

public class ParametricDistributionInputEditor extends BEASTObjectInputEditor {

    public ParametricDistributionInputEditor(BeautiDoc doc) {
		super(doc);
	}

	private static final long serialVersionUID = 1L;
    boolean useDefaultBehavior;
	boolean mayBeUnstable;

    @Override
    public Class<?> type() {
        //return ParametricDistributionInputEditor.class;
        return ParametricDistribution.class;
    }

    @Override
    public void init(Input<?> input, BEASTInterface plugin, int itemNr, ExpandOption bExpandOption, boolean bAddButtons) {
        useDefaultBehavior = !((plugin instanceof beast.math.distributions.Prior) || plugin instanceof MRCAPrior || plugin instanceof TreeDistribution);

//    	if (useDefaultBehavior && false) {
//    		super.init(input, plugin, bExpandOption, bAddButtons);
//    	} else {
        m_bAddButtons = bAddButtons;
        m_input = input;
        m_plugin = plugin;
		this.itemNr = itemNr;
        if (input.get() != null) {
            super.init(input, plugin, itemNr, ExpandOption.TRUE, bAddButtons);
        }
        add(createGraph());
//    	}
    } // init


    @Override
    /** suppress combobox **/
    protected void addComboBox(JComponent box, Input<?> input, BEASTInterface plugin) {
        if (useDefaultBehavior) {
            super.addComboBox(box, input, plugin);
        }
    }

    @Override
    /** suppress input label**/
    protected void addInputLabel() {
        if (useDefaultBehavior) {
            super.addInputLabel();
        }
    }

    /**
     * maps most significant digit to nr of ticks on graph *
     */
    final static int[] NR_OF_TICKS = new int[]{5, 10, 8, 6, 8, 10, 6, 7, 8, 9, 10};

    /* class for drawing information for a parametric distribution **/
    class PDPanel extends JPanel {
        // the length in pixels of a tick
        private static final int TICK_LENGTH = 5;

        // the right margin
        private static final int RIGHT_MARGIN = 20;

        // the margin to the left of y-labels
        private static final int MARGIN_LEFT_OF_Y_LABELS = 5;

        // the top margin
        private static final int TOP_MARGIN = 10;

        int m_nTicks;
        private static final long serialVersionUID = 1L;

        @Override
        public void paintComponent(java.awt.Graphics g) {

            Graphics2D g2d = (Graphics2D)g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            final int width = getWidth();
            final int height = getHeight();
            final int labeloffset = 50;

            ParametricDistribution m_distr = (ParametricDistribution)m_input.get();
            if (m_distr == null) {
                return;
            }
            try {
                m_distr.initAndValidate();
            } catch (Exception e1) {
                // ignore
            }

            Font font = g.getFont();
            double fMinValue = 0.1;
            double fMaxValue = 1;
            try {
                fMinValue = m_distr.inverseCumulativeProbability(0.01);
                fMaxValue = m_distr.inverseCumulativeProbability(0.99);
            } catch (Exception e) {
                // use defaults
            }
            double fXRange = fMaxValue - fMinValue;
            // adjust fYMax so that the ticks come out right
            double fX0 = fMinValue;
            int k = 0;
            double f = fXRange;
            double f2 = fX0;
            while (f > 10) {
                f /= 10;
                f2 /= 10;
                k++;
            }
            while (f < 1 && f > 0) {
                f *= 10;
                f2 *= 10;
                k--;
            }
            f = Math.ceil(f);
            f2 = Math.floor(f2);
//			final int NR_OF_TICKS_X = NR_OF_TICKS[(int) f];
            for (int i = 0; i < k; i++) {
                f *= 10;
                f2 *= 10;
            }
            for (int i = k; i < 0; i++) {
                f /= 10;
                f2 /= 10;
            }
            //double fAdjXRange = f;

            fXRange = fXRange + fMinValue - f2;
            fXRange = adjust(fXRange);
            final int NR_OF_TICKS_X = m_nTicks;

            fMinValue = f2; //fXRange = fAdjXRange;

            int nPoints;
            if (!m_distr.isIntegerDistribution()) {
                nPoints = 100;
            } else {
                nPoints = (int) (fXRange);
            }
            int[] xPoints = new int[nPoints];
            int[] yPoints = new int[nPoints];
            double[] fyPoints = new double[nPoints];
            double fYMax = 0;
            for (int i = 0; i < nPoints; i++) {
                //try {
                    fyPoints[i] = getDensityForPlot(m_distr, fMinValue + (fXRange * i) / nPoints);
                //}
                if (Double.isInfinite(fyPoints[i]) || Double.isNaN(fyPoints[i])) {
                    fyPoints[i] = 0;
                }
                //fyPoints[i] = Math.exp(m_distr.logDensity(fMinValue + (fXRange * i)/nPoints));
                fYMax = Math.max(fYMax, fyPoints[i]);
            }

            fYMax = adjust(fYMax);
            final int NR_OF_TICKS_Y = m_nTicks;

            // draw ticks on edge
            Font smallFont = new Font(font.getName(), font.getStyle(), 8);
            g.setFont(smallFont);

            // collect the ylabels and the maximum label width in small font
            String[] ylabels = new String[NR_OF_TICKS_Y+1];
            int maxLabelWidth = 0;
            FontMetrics sfm = getFontMetrics(smallFont);
            for (int i = 0; i <= NR_OF_TICKS_Y; i++) {
                ylabels[i] = format(fYMax * i / NR_OF_TICKS_Y);
                int stringWidth = sfm.stringWidth(ylabels[i]);
                if (stringWidth > maxLabelWidth) maxLabelWidth = stringWidth;
            }

            // collect the xlabels
            String[] xlabels = new String[NR_OF_TICKS_X+1];
            for (int i = 0; i <= NR_OF_TICKS_X; i++) {
                xlabels[i] = format(fMinValue + fXRange * i / NR_OF_TICKS_X);
            }
            int maxLabelHeight = sfm.getMaxAscent()+sfm.getMaxDescent();

            int leftMargin = maxLabelWidth + TICK_LENGTH + 1 + MARGIN_LEFT_OF_Y_LABELS;
            int bottomMargin = maxLabelHeight + TICK_LENGTH + 1;

            int nGraphWidth = width - leftMargin - RIGHT_MARGIN;
            int nGraphHeight = height - TOP_MARGIN - bottomMargin - labeloffset;

            // DRAW GRAPH PAPER
            g.setColor(Color.WHITE);
            g.fillRect(leftMargin, TOP_MARGIN, nGraphWidth, nGraphHeight);
            g.setColor(Color.BLACK);
            g.drawRect(leftMargin, TOP_MARGIN, nGraphWidth, nGraphHeight);

            for (int i = 0; i < nPoints; i++) {
                xPoints[i] = leftMargin + nGraphWidth * i / nPoints;
                yPoints[i] = 1 + (int) (TOP_MARGIN + nGraphHeight - nGraphHeight * fyPoints[i] / fYMax);
            }
            if (!m_distr.isIntegerDistribution()) {
                g.drawPolyline(xPoints, yPoints, nPoints);
            } else {
                int y0 = 1 + TOP_MARGIN + nGraphHeight;
                int dotDiameter = nGraphHeight/20;
                for (int i=0; i<nPoints; i++) {
                    g.drawLine(xPoints[i], y0, xPoints[i], yPoints[i]);
                    g.fillOval(xPoints[i]-dotDiameter/2, yPoints[i]-dotDiameter/2, dotDiameter, dotDiameter);
                }
            }

            for (int i = 0; i <= NR_OF_TICKS_X; i++) {
                int x = leftMargin + i * nGraphWidth / NR_OF_TICKS_X;
                g.drawLine(x, TOP_MARGIN + nGraphHeight, x, TOP_MARGIN + nGraphHeight + TICK_LENGTH);
                g.drawString(xlabels[i], x-sfm.stringWidth(xlabels[i])/2, TOP_MARGIN + nGraphHeight + TICK_LENGTH + 1 + sfm.getMaxAscent());
            }

            // draw the y labels and ticks
            for (int i = 0; i <= NR_OF_TICKS_Y; i++) {
                int y = TOP_MARGIN + nGraphHeight - i * nGraphHeight / NR_OF_TICKS_Y;
                g.drawLine(leftMargin - TICK_LENGTH, y, leftMargin, y);
                g.drawString(ylabels[i], leftMargin - TICK_LENGTH - 1 - sfm.stringWidth(ylabels[i]), y + 3);
            }

            g.setFont(new Font(font.getName(), font.getStyle(), 10));
            try {
                FontMetrics fontMetrics = g.getFontMetrics();
                String[] sStrs = new String[]{"2.5% Quantile", "5% Quantile", "Median", "95% Quantile", "97.5% Quantile"};
                Double[] fQuantiles = new Double[]{0.025, 0.05, 0.5, 0.95, 0.975};
            	mayBeUnstable = false;
                for (k = 0; k < 5; k++) {

                    int y = TOP_MARGIN + nGraphHeight + bottomMargin + g.getFontMetrics().getMaxAscent() + k * 10;

                	try {
                        g.drawString(format(m_distr.inverseCumulativeProbability(fQuantiles[k])), nGraphWidth / 2 + leftMargin, y);
                    } catch (MathException e) {
                        g.drawString("not available", nGraphWidth / 2 + leftMargin, y);
                    }
                    g.drawString(sStrs[k], nGraphWidth / 2 - fontMetrics.stringWidth(sStrs[k]) + leftMargin - 10, y);
                }
                if (mayBeUnstable) {
                	int x = nGraphWidth * 3/ 4 + leftMargin; int y =TOP_MARGIN + nGraphHeight + bottomMargin + 10;
                    g.drawString("* numbers", x, y + 20); 
                    g.drawString("may not be", x, y + 30);                	
                    g.drawString("accurate", x, y + 40);                	
                }
            } catch (Exception e) {
                // probably something wrong with the parameters of the parametric distribution
                g.drawString("Improper parameters", leftMargin, TOP_MARGIN + nGraphHeight + bottomMargin + g.getFontMetrics().getMaxAscent());
            }
        }
        
        private String format(double value) {
            StringWriter writer = new StringWriter();
            PrintWriter pw = new PrintWriter(writer);
            pw.printf("%.3g", value);
            if (value != 0.0 && Math.abs(value) / 1000 < 1e-320) { // 2e-6 = 2 * AbstractContinuousDistribution.solverAbsoluteAccuracy
            	mayBeUnstable = true;
            	pw.printf("*");
            }
            pw.flush();
            return writer.toString();
        }
        
        private double adjust(double fYMax) {
            // adjust fYMax so that the ticks come out right
            int k = 0;
            double fY = fYMax;
            while (fY > 10) {
                fY /= 10;
                k++;
            }
            while (fY < 1 && fY > 0) {
                fY *= 10;
                k--;
            }
            fY = Math.ceil(fY);
            m_nTicks = NR_OF_TICKS[(int) fY];
            for (int i = 0; i < k; i++) {
                fY *= 10;
            }
            for (int i = k; i < 0; i++) {
                fY /= 10;
            }
            return fY;
        }
    }
    
    /**
     * Returns the density of pDistr at x when pDistr is a density of a
     * continuous variable, but returns the probability of the closest
     * integer when pDistr is a probability distribution over an integer-valued
     * parameter.
     * 
     * @param pDistr
     * @param x
     * @return density at x or probability of closest integer to x
     */
    private double getDensityForPlot(ParametricDistribution pDistr, double x) {
        if (pDistr.isIntegerDistribution()) {
            return pDistr.density((int) Math.round(x));
        } else {
            return pDistr.density(x);
        }
    }

    private Component createGraph() {
        JPanel panel = new PDPanel();
        Dimension size = new Dimension(200, 200);
        panel.setSize(size);
        panel.setPreferredSize(size);
        panel.setMinimumSize(size);
        Box box = Box.createHorizontalBox();
        box.setBorder(BorderFactory.createEmptyBorder());
        box.add(panel);
        return box;
    }

    @Override
    public void validate() {
        super.validate();
        repaint();
    }

}
