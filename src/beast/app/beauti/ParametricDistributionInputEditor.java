package beast.app.beauti;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.text.DecimalFormat;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JPanel;

import org.apache.commons.math.MathException;

import beast.app.draw.PluginInputEditor;
import beast.core.Input;
import beast.core.Plugin;
import beast.math.distributions.ParametricDistribution;

public class ParametricDistributionInputEditor extends PluginInputEditor {

	private static final long serialVersionUID = 1L;

	@Override
	public Class<?> type() {
		return ParametricDistribution.class;
	}
	
    @Override
    public void init(Input<?> input, Plugin plugin, EXPAND bExpand, boolean bAddButtons) {
    	super.init(input, plugin, EXPAND.TRUE, bAddButtons);
    	add(createGraph());
    } // init


	@Override
	/** suppress combobox **/
	protected void addComboBox(Box box, Input<?> input, Plugin plugin) {}    

	@Override
	/** suppress input label**/
	protected void addInputLabel() {}

	/* class for drawing information for a parametric distribution **/
    class PDPanel extends JPanel {

    	@Override
		public void paintComponent(java.awt.Graphics g) {
			ParametricDistribution m_distr = (ParametricDistribution) m_input.get();
			try {
				m_distr.initAndValidate();
			} catch (Exception e1) {
				// ignore
			}
			final int width = getWidth();
			final int height = getHeight();
			final int graphoffset = 20; 
			final int labeloffset = 50;
			int nGraphWidth = width - graphoffset * 2;
			int nGraphHeight = height - graphoffset * 2 - labeloffset;
			g.setColor(Color.WHITE);
			g.fillRect(graphoffset, graphoffset, nGraphWidth, nGraphHeight);
			g.setColor(Color.BLACK);
			g.drawRect(graphoffset, graphoffset, nGraphWidth, nGraphHeight);
			int nPoints = 100;
			int [] xPoints = new int[nPoints];
			int [] yPoints = new int[nPoints];
			double [] fyPoints = new double[nPoints];
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
			double fYMax = 0;
			for (int i = 0; i < nPoints; i++) {
				xPoints[i] = graphoffset + nGraphWidth * i / nPoints;
				fyPoints[i] = Math.exp(m_distr.logDensity(fMinValue + (fXRange * i)/nPoints)); 
				fYMax = Math.max(fYMax, fyPoints[i]);
			}
			for (int i = 0; i < nPoints; i++) {
				yPoints[i] = 1+(int)(graphoffset + nGraphHeight - nGraphHeight * fyPoints[i]/fYMax);
			}				
			g.drawPolyline(xPoints, yPoints, nPoints);

			// draw ticks on edge
			Font smallFont = new Font(font.getName(), font.getStyle(), 8);
			g.setFont(smallFont);
			DecimalFormat myFormatter = new DecimalFormat("##.##");
			final int NR_OF_TICKS = 7;
			fMinValue += m_distr.m_offset.get();
			for (int i = 0; i <= NR_OF_TICKS; i++) {
				int x = graphoffset + i * nGraphWidth / NR_OF_TICKS;
				g.drawLine(x, graphoffset + nGraphHeight, x, graphoffset + nGraphHeight + 5);
				g.drawString(myFormatter.format(fMinValue + fXRange * i / NR_OF_TICKS), x + 2, graphoffset + nGraphHeight + 5 + 2);

				int y = graphoffset + nGraphHeight - i * nGraphHeight / NR_OF_TICKS;
				g.drawLine(graphoffset - 5, y, graphoffset, y);
				g.drawString(myFormatter.format(fYMax * i / NR_OF_TICKS), 0, y + 3);
			}
				
			g.setFont(new Font(font.getName(), font.getStyle(), 10));
			try {
				FontMetrics fontMetrics = g.getFontMetrics();
				String [] sStrs = new String[] {"2.5% Quantile", "5% Quantile", "Median" , "95% Quantile", "97.5% Quantile"};
				Double [] fQuantiles = new Double[] {0.025, 0.05, 0.5, 0.95, 0.975};
				for (int k = 0; k < 5; k++) {
					g.drawString(myFormatter.format(m_distr.inverseCumulativeProbability(fQuantiles[k])), nGraphWidth / 2 + graphoffset, graphoffset + nGraphHeight + 20 + k * 10);
					g.drawString(sStrs[k], nGraphWidth/2 - fontMetrics.stringWidth(sStrs[k]), graphoffset + nGraphHeight + 20 + k * 10);
				}
			} catch (MathException e) {
				g.drawString("Quantiles not available", graphoffset, graphoffset + nGraphHeight + 20);
			}
		};
    }

    private Component createGraph() {
		JPanel panel = new PDPanel();
		Dimension size = new Dimension(200,200);
		panel.setSize(size);
		panel.setPreferredSize(size);
		panel.setMinimumSize(size);
		Box box = Box.createHorizontalBox();
		box.setBorder(BorderFactory.createEtchedBorder());
		box.add(panel);
		return box;
	}

    
    @Override
    public void validate() {
    	super.validate();
    	repaint();
    }
	
}
