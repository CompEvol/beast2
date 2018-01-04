package beast.app.beauti;


import java.awt.*;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.UIManager;

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
    public void init(Input<?> input, BEASTInterface beastObject, int itemNr, ExpandOption isExpandOption, boolean addButtons) {
        useDefaultBehavior = !((beastObject instanceof beast.math.distributions.Prior) || beastObject instanceof MRCAPrior || beastObject instanceof TreeDistribution);

//    	if (useDefaultBehavior && false) {
//    		super.init(input, beastObject, isExpandOption, addButtons);
//    	} else {
        m_bAddButtons = addButtons;
        m_input = input;
        m_beastObject = beastObject;
		this.itemNr = itemNr;
        if (input.get() != null) {
            super.init(input, beastObject, itemNr, ExpandOption.TRUE, addButtons);
        }
        add(createGraph());
//    	}
    } // init


    @Override
    /** suppress combobox **/
    protected void addComboBox(JComponent box, Input<?> input, BEASTInterface beastObject) {
        if (useDefaultBehavior) {
            super.addComboBox(box, input, beastObject);
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

            // Record current font, since drawError can take over part-way
            // through the call to drawGraph, which alters the graphics font size.
            Font originalFont = g.getFont();

            ParametricDistribution m_distr = (ParametricDistribution)m_input.get();
            if (m_distr == null) {
                drawError(g);
            } else {
                try {
                    m_distr.initAndValidate();
                    drawGraph(m_distr, 50, g);
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                    ex.printStackTrace();
                    g.setFont(originalFont);
                    drawError(g);
                }
            }

        }

        private void drawError(Graphics g) {
            Font oldFont = g.getFont();
            Font newFont = new Font(oldFont.getName(), oldFont.getStyle(), oldFont.getSize());
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(Color.BLACK);
            g.drawRect(0, 0, getWidth()-1, getHeight()-1);
            FontMetrics fm = g.getFontMetrics();

            String errorString = "Cannot display distribution.";

            int stringWidth = fm.stringWidth(errorString);
            g.drawString("Cannot display distribution.",
                    (getWidth() - stringWidth)/2,
                    (getHeight() - fm.getHeight())/2);
        }

        private void drawGraph(ParametricDistribution m_distr, int labelOffset, Graphics g) {
            final int width = getWidth();
            final int height = getHeight();

            double minValue = 0.1;
            double maxValue = 1;
            try {
                minValue = m_distr.inverseCumulativeProbability(0.01);
                maxValue = m_distr.inverseCumulativeProbability(0.99);
            } catch (Exception e) {
                // use defaults
            }
            double xRange = maxValue - minValue;
            // adjust yMax so that the ticks come out right
            double x0 = minValue;
            int k = 0;
            double f = xRange;
            double f2 = x0;
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
            //double adjXRange = f;

            xRange = xRange + minValue - f2;
            xRange = adjust(xRange);
            final int NR_OF_TICKS_X = m_nTicks;

            minValue = f2; //xRange = adjXRange;

            int points;
            if (!m_distr.isIntegerDistribution()) {
                points = 100;
            } else {
                points = (int) (xRange);
            }
            int[] xPoints = new int[points];
            int[] yPoints = new int[points];
            double[] fyPoints = new double[points];
            double yMax = 0;
            for (int i = 0; i < points; i++) {
                //try {
                    fyPoints[i] = getDensityForPlot(m_distr, minValue + (xRange * i) / points);
                //}
                if (Double.isInfinite(fyPoints[i]) || Double.isNaN(fyPoints[i])) {
                    fyPoints[i] = 0;
                }
                //fyPoints[i] = Math.exp(m_distr.logDensity(minValue + (xRange * i)/points));
                yMax = Math.max(yMax, fyPoints[i]);
            }

            yMax = adjust(yMax);
            final int NR_OF_TICKS_Y = m_nTicks;

            // draw ticks on edge
            Font font = g.getFont();
            Font smallFont = new Font(font.getName(), font.getStyle(), font.getSize() * 2/3);
            g.setFont(smallFont);

            // collect the ylabels and the maximum label width in small font
            String[] ylabels = new String[NR_OF_TICKS_Y+1];
            int maxLabelWidth = 0;
            FontMetrics sfm = getFontMetrics(smallFont);
            for (int i = 0; i <= NR_OF_TICKS_Y; i++) {
                ylabels[i] = format(yMax * i / NR_OF_TICKS_Y);
                int stringWidth = sfm.stringWidth(ylabels[i]);
                if (stringWidth > maxLabelWidth) maxLabelWidth = stringWidth;
            }

            // collect the xlabels
            String[] xlabels = new String[NR_OF_TICKS_X+1];
            for (int i = 0; i <= NR_OF_TICKS_X; i++) {
                xlabels[i] = format(minValue + xRange * i / NR_OF_TICKS_X);
            }
            int maxLabelHeight = sfm.getMaxAscent()+sfm.getMaxDescent();

            int leftMargin = maxLabelWidth + TICK_LENGTH + 1 + MARGIN_LEFT_OF_Y_LABELS;
            int bottomMargin = maxLabelHeight + TICK_LENGTH + 1;

            int graphWidth = width - leftMargin - RIGHT_MARGIN;
            int graphHeight = height - TOP_MARGIN - bottomMargin - labelOffset;

            // DRAW GRAPH PAPER
            g.setColor(Color.WHITE);
            g.fillRect(leftMargin, TOP_MARGIN, graphWidth, graphHeight);
            g.setColor(Color.BLACK);
            g.drawRect(leftMargin, TOP_MARGIN, graphWidth, graphHeight);

            for (int i = 0; i < points; i++) {
                xPoints[i] = leftMargin + graphWidth * i / points;
                yPoints[i] = 1 + (int) (TOP_MARGIN + graphHeight - graphHeight * fyPoints[i] / yMax);
            }
            if (!m_distr.isIntegerDistribution()) {
                g.drawPolyline(xPoints, yPoints, points);
            } else {
                int y0 = 1 + TOP_MARGIN + graphHeight;
                int dotDiameter = graphHeight/20;
                for (int i=0; i<points; i++) {
                    g.drawLine(xPoints[i], y0, xPoints[i], yPoints[i]);
                    g.fillOval(xPoints[i]-dotDiameter/2, yPoints[i]-dotDiameter/2, dotDiameter, dotDiameter);
                }
            }

            for (int i = 0; i <= NR_OF_TICKS_X; i++) {
                int x = leftMargin + i * graphWidth / NR_OF_TICKS_X;
                g.drawLine(x, TOP_MARGIN + graphHeight, x, TOP_MARGIN + graphHeight + TICK_LENGTH);
                g.drawString(xlabels[i], x-sfm.stringWidth(xlabels[i])/2, TOP_MARGIN + graphHeight + TICK_LENGTH + 1 + sfm.getMaxAscent());
            }

            // draw the y labels and ticks
            for (int i = 0; i <= NR_OF_TICKS_Y; i++) {
                int y = TOP_MARGIN + graphHeight - i * graphHeight / NR_OF_TICKS_Y;
                g.drawLine(leftMargin - TICK_LENGTH, y, leftMargin, y);
                g.drawString(ylabels[i], leftMargin - TICK_LENGTH - 1 - sfm.stringWidth(ylabels[i]), y + 3);
            }

            int fontHeight = font.getSize() * 10 / 12;
            g.setFont(new Font(font.getName(), font.getStyle(), fontHeight));

            FontMetrics fontMetrics = g.getFontMetrics();
            String[] strs = new String[]{"2.5% Quantile", "5% Quantile", "Median", "95% Quantile", "97.5% Quantile"};
            Double[] quantiles = new Double[]{0.025, 0.05, 0.5, 0.95, 0.975};
            mayBeUnstable = false;
            for (k = 0; k < 5; k++) {

                int y = TOP_MARGIN + graphHeight + bottomMargin + g.getFontMetrics().getMaxAscent() + k * fontHeight;

                try {
                    g.drawString(format(m_distr.inverseCumulativeProbability(quantiles[k])), graphWidth / 2 + leftMargin, y);
                } catch (MathException e) {
                    g.drawString("not available", graphWidth / 2 + leftMargin, y);
                }
                g.drawString(strs[k], graphWidth / 2 - fontMetrics.stringWidth(strs[k]) + leftMargin - fontHeight, y);
            }
            if (mayBeUnstable) {
                int x = graphWidth * 3/ 4 + leftMargin; int y =TOP_MARGIN + graphHeight + bottomMargin + fontHeight;
                g.drawString("* numbers", x, y + 2*fontHeight);
                g.drawString("may not be", x, y + 3*fontHeight);
                g.drawString("accurate", x, y + 4*fontHeight);
            }
            try {
                g.drawString("mean " + format(m_distr.getMean()), graphWidth * 3/ 4 + leftMargin, TOP_MARGIN + graphHeight + bottomMargin + fontHeight);
            } catch (RuntimeException e) {
                // catch in case it is not implemented.
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
        
        private double adjust(double yMax) {
            // adjust yMax so that the ticks come out right
            int k = 0;
            double y = yMax;
            while (y > 10) {
                y /= 10;
                k++;
            }
            while (y < 1 && y > 0) {
                y *= 10;
                k--;
            }
            y = Math.ceil(y);
            m_nTicks = NR_OF_TICKS[(int) y];
            for (int i = 0; i < k; i++) {
                y *= 10;
            }
            for (int i = k; i < 0; i++) {
                y /= 10;
            }
            return y;
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
        int fsize = UIManager.getFont("Label.font").getSize();
        Dimension size = new Dimension(200 * fsize / 13, 200 * fsize / 13);
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
