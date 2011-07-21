package beast.app.beauti2.alignmentviewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 *
 * @author Andrew Rambaut
 * @version $Id: AlignmentViewer.java,v 1.2 2005/11/11 16:40:41 rambaut Exp $
 */
public class AlignmentViewer extends JPanel {
	private TaxonPane taxonPane;
    private JScrollPane taxonScrollPane;

    private RulerPane rulerPane;
    private JScrollPane rulerScrollPane;

	private PlotPane plotPane;
	private JScrollPane plotScrollPane;

    private AlignmentPane alignmentPane;
    private JScrollPane alignmentScrollPane;

    private JSplitPane splitPane;

	/** Creates new AlignmentPanel */
	public AlignmentViewer() {
		this(null);
	}

    /** Creates new AlignmentPanel */
    public AlignmentViewer(PlotPane plotPane) {

        setOpaque(false);
        setMinimumSize(new Dimension(300,150));
        setLayout(new BorderLayout(6,6));

        taxonPane = new TaxonPane();
        taxonScrollPane = new JScrollPane(taxonPane, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        taxonScrollPane.setBorder(null);

        JPanel leftPanel = new JPanel(new BorderLayout(6,6));
        leftPanel.add(taxonScrollPane, BorderLayout.CENTER);
        JPanel emptyPanel = new JPanel();
        emptyPanel.setPreferredSize(new Dimension(16, 16));
        leftPanel.add(emptyPanel, BorderLayout.NORTH);

        rulerPane = new RulerPane();
        rulerPane.setOpaque(false);
        rulerScrollPane = new JScrollPane(rulerPane, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        rulerScrollPane.setBorder(null);

        alignmentPane = new AlignmentPane(taxonPane, rulerPane);
        alignmentScrollPane = new JScrollPane(alignmentPane, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        alignmentScrollPane.setBorder(null);

        taxonScrollPane.getVerticalScrollBar().setModel(alignmentScrollPane.getVerticalScrollBar().getModel());
        rulerScrollPane.getHorizontalScrollBar().setModel(alignmentScrollPane.getHorizontalScrollBar().getModel());

        JPanel rightPanel = new JPanel(new BorderLayout(6,6));
        rightPanel.add(alignmentScrollPane, BorderLayout.CENTER);
        rightPanel.add(rulerScrollPane, BorderLayout.NORTH);

	    if (plotPane != null) {
		    plotScrollPane = new JScrollPane(plotPane, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		    plotScrollPane.getHorizontalScrollBar().setModel(alignmentScrollPane.getHorizontalScrollBar().getModel());
		    rightPanel.add(plotScrollPane, BorderLayout.SOUTH);
	    }

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setDividerLocation(120);

        add(splitPane, BorderLayout.CENTER);


        alignmentPane.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent mouseEvent) {
            }

            public void mousePressed(MouseEvent mouseEvent) {
                // This is used for drag-to-scroll in combination with mouseDragged
                // in the MouseMotionListener, below.
                dragPoint = mouseEvent.getPoint();
            }

        });

        alignmentPane.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(MouseEvent mouseEvent) {
            }

            public void mouseDragged(MouseEvent mouseEvent) {
                // Calculate how far the mouse has been dragged from the point clicked in
                // mousePressed, above.
                int deltaX = mouseEvent.getX() - dragPoint.x;
                int deltaY = mouseEvent.getY() - dragPoint.y;

                // Get the currently visible window
                Rectangle visRect = alignmentPane.getVisibleRect();

                // Calculate how much we need to scroll
                if (deltaX > 0) {
                    deltaX = visRect.x - deltaX;
                } else {
                    deltaX = visRect.x + visRect.width - deltaX;
                }

                if (deltaY > 0) {
                    deltaY = visRect.y - deltaY;
                } else {
                    deltaY = visRect.y + visRect.height - deltaY;
                }

                // Scroll the visible region
                Rectangle r = new Rectangle(deltaX, deltaY, 1, 1);
                alignmentPane.scrollRectToVisible(r);
            }
        });
    }

    public void setAlignmentBuffer(AlignmentBuffer alignmentBuffer) {
        rulerPane.setAlignmentBuffer(alignmentBuffer);
        taxonPane.setAlignmentBuffer(alignmentBuffer);
        alignmentPane.setAlignmentBuffer(alignmentBuffer);
    }

    public void setRowDecorator(RowDecorator rowDecorator) {
        alignmentPane.setRowDecorator(rowDecorator);
    }

    public void setColumnDecorator(ColumnDecorator columnDecorator) {
        alignmentPane.setColumnDecorator(columnDecorator);
        rulerPane.setColumnDecorator(columnDecorator);
    }

    public void setCellDecorator(CellDecorator cellDecorator) {
        alignmentPane.setCellDecorator(cellDecorator);
    }

	public void addHorizontalScrollbarListener(AdjustmentListener adjustmentListener) {
		alignmentScrollPane.getHorizontalScrollBar().addAdjustmentListener(adjustmentListener);
	}

	public void addVerticalScrollbarListener(AdjustmentListener adjustmentListener) {
		alignmentScrollPane.getVerticalScrollBar().addAdjustmentListener(adjustmentListener);
	}

	public void addComponentListener(ComponentListener componentListener) {
		alignmentScrollPane.addComponentListener(componentListener);
	}

	public Rectangle getTaxonPaneBounds() {
		return taxonScrollPane.getViewportBorderBounds();
	}

	public Rectangle getAlignmentPaneBounds() {
		return alignmentScrollPane.getViewportBorderBounds();
	}

	public Rectangle getVisibleArea() {
		return alignmentPane.getVisibleArea();
	}

	public void setTopRow(int row) {
		alignmentPane.setTopRow(row);
	}

	public void setLeftColumn(int col) {
		alignmentPane.setLeftColumn(col);
	}

	public void setCentreColumn(int col) {
		alignmentPane.setCentreColumn(col);
	}

	public void setRightColumn(int col) {
		alignmentPane.setRightColumn(col);
	}

    private Point dragPoint = null;
}
