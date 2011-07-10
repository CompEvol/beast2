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

	/** maps most significant digit to nr of ticks on graph **/ 
	final static int [] NR_OF_TICKS = new int [] {0,10,8,6,8,10,6,7,8,9, 10};

	/* class for drawing information for a parametric distribution **/
    class PDPanel extends JPanel {
    	int m_nTicks;
		private static final long serialVersionUID = 1L;

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
			while (f < 1) {
				f *= 10;
				f2 *= 10;
				k--;
			}
			f = Math.ceil(f);
			f2 = Math.floor(f2);
			final int NR_OF_TICKS_X = NR_OF_TICKS[(int) f];
			for (int i = 0; i < k; i++) {
				f *= 10;
				f2 *= 10;
			}
			for (int i = k; i < 0; i++) {
				f /= 10;
				f2 /= 10;
			}
			double fAdjXRange = f;

			fMinValue = f2; fXRange = fAdjXRange;
			
			double fYMax = 0;
			for (int i = 0; i < nPoints; i++) {
				xPoints[i] = graphoffset + nGraphWidth * i / nPoints;
				try {
					fyPoints[i] = m_distr.density(fMinValue + (fXRange * i)/nPoints);
				} catch (Exception e) {
					fyPoints[i] = 0;
				}
				if (Double.isInfinite(fyPoints[i]) || Double.isNaN(fyPoints[i])) {
					fyPoints[i] = 0;
				}
				//fyPoints[i] = Math.exp(m_distr.logDensity(fMinValue + (fXRange * i)/nPoints)); 
				fYMax = Math.max(fYMax, fyPoints[i]);
			}

			fYMax = adjust(fYMax);
			final int NR_OF_TICKS_Y = m_nTicks;			
			
			for (int i = 0; i < nPoints; i++) {
				yPoints[i] = 1+(int)(graphoffset + nGraphHeight - nGraphHeight * fyPoints[i]/fYMax);
			}				
			g.drawPolyline(xPoints, yPoints, nPoints);

			// draw ticks on edge
			Font smallFont = new Font(font.getName(), font.getStyle(), 8);
			g.setFont(smallFont);
			DecimalFormat myFormatter = new DecimalFormat("##.##");
			fMinValue += m_distr.m_offset.get();
			for (int i = 0; i <= NR_OF_TICKS_X; i++) {
				int x = graphoffset + i * nGraphWidth / NR_OF_TICKS_X;
				g.drawLine(x, graphoffset + nGraphHeight, x, graphoffset + nGraphHeight + 5);
				g.drawString(myFormatter.format(fMinValue + fXRange * i / NR_OF_TICKS_X), x + 2, graphoffset + nGraphHeight + 5 + 2);
			}
			for (int i = 0; i <= NR_OF_TICKS_Y; i++) {
					int y = graphoffset + nGraphHeight - i * nGraphHeight / NR_OF_TICKS_Y;
				g.drawLine(graphoffset - 5, y, graphoffset, y);
				g.drawString(myFormatter.format(fYMax * i / NR_OF_TICKS_Y), 0, y + 3);
			}
				
			g.setFont(new Font(font.getName(), font.getStyle(), 10));
			try {
				FontMetrics fontMetrics = g.getFontMetrics();
				String [] sStrs = new String[] {"2.5% Quantile", "5% Quantile", "Median" , "95% Quantile", "97.5% Quantile"};
				Double [] fQuantiles = new Double[] {0.025, 0.05, 0.5, 0.95, 0.975};
				for (k = 0; k < 5; k++) {
					g.drawString(myFormatter.format(m_distr.inverseCumulativeProbability(fQuantiles[k])), nGraphWidth / 2 + graphoffset, graphoffset + nGraphHeight + 20 + k * 10);
					g.drawString(sStrs[k], nGraphWidth/2 - fontMetrics.stringWidth(sStrs[k]), graphoffset + nGraphHeight + 20 + k * 10);
				}
			} catch (MathException e) {
				g.drawString("Quantiles not available", graphoffset, graphoffset + nGraphHeight + 20);
			}
		};
    
		private double adjust(double fYMax) {
			// adjust fYMax so that the ticks come out right
			int k = 0;
			double fY = fYMax;
			while (fY > 10) {
				fY /= 10;
				k++;
			}
			while (fY < 1) {
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
