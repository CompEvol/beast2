package beast.app.beauti;

import java.util.Arrays;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import beast.evolution.alignment.Alignment;
import beast.evolution.datatype.DataType;
import beast.util.NexusParser;
import java.beans.*;

import javax.swing.*;
import javax.swing.event.*;

public class AlignmentViewer extends JPanel {
	private static final long serialVersionUID = 1L;

	/*
	 *  Prevent the specified number of columns from scrolling horizontally in
	 *  the scroll pane. The table must already exist in the scroll pane.
	 *
	 *  The functionality is accomplished by creating a second JTable (fixed)
	 *  that will share the TableModel and SelectionModel of the main table.
	 *  This table will be used as the row header of the scroll pane.
	 */
	public class FixedColumnTable implements ChangeListener, PropertyChangeListener
	{
		private JTable main;
		private JTable fixed;
		private JScrollPane scrollPane;

		/*
		 *  Specify the number of columns to be fixed and the scroll pane
		 *  containing the table.
		 */
		public FixedColumnTable(int fixedColumns, JScrollPane scrollPane)
		{
			this.scrollPane = scrollPane;

			main = ((JTable)scrollPane.getViewport().getView());
			main.setAutoCreateColumnsFromModel( false );
			main.addPropertyChangeListener( this );

			//  Use the existing table to create a new table sharing
			//  the DataModel and ListSelectionModel

			fixed = new JTable();
			fixed.setAutoCreateColumnsFromModel( false );
			fixed.setModel( main.getModel() );
			fixed.setSelectionModel( main.getSelectionModel() );
			fixed.setFocusable( false );

			//  Remove the fixed columns from the main table
			//  and add them to the fixed table
			for (int i = 0; i < fixedColumns; i++)
			{
		        TableColumnModel columnModel = main.getColumnModel();
		        TableColumn column = columnModel.getColumn( 0 );
	    	    columnModel.removeColumn( column );
				fixed.getColumnModel().addColumn( column );
			}

			//  Add the fixed table to the scroll pane
	        fixed.setPreferredScrollableViewportSize(fixed.getPreferredSize());
			scrollPane.setRowHeaderView( fixed );
			scrollPane.setCorner(JScrollPane.UPPER_LEFT_CORNER, fixed.getTableHeader());

			// Synchronize scrolling of the row header with the main table
			scrollPane.getRowHeader().addChangeListener( this );
		}

		@Override
		public void stateChanged(ChangeEvent e)
		{
			//  Sync the scroll pane scrollbar with the row header

			JViewport viewport = (JViewport) e.getSource();
			scrollPane.getVerticalScrollBar().setValue(viewport.getViewPosition().y);
		}

		@Override
		public void propertyChange(PropertyChangeEvent e)
		{
			//  Keep the fixed table in sync with the main table

			if ("selectionModel".equals(e.getPropertyName()))
			{
				fixed.setSelectionModel( main.getSelectionModel() );
			}

			if ("model".equals(e.getPropertyName()))
			{
				fixed.setModel( main.getModel() );
			}
		}
	}

	public AlignmentViewer(Alignment data) throws Exception {
		int nSites = data.getSiteCount();
		int nTaxa = data.getNrTaxa();
		
		// set up table content
		Object[][] tableData = new Object[nTaxa][nSites+1];
		DataType dataType = data.getDataType();
		for (int i = 0; i < nSites; i++) {
			int iPattern = data.getPatternIndex(i);
			int [] pattern = data.getPattern(iPattern);
			String sPattern = dataType.state2string(pattern);
			for (int j = 0; j < nTaxa; j++) {
				tableData[j][i+1] = sPattern.charAt(j);
			}
		}
		
		// set up row labels
		for (int i = 0; i < nTaxa; i++) {
			tableData[i][0] = data.getTaxaNames().get(i);
		}
		
		// set up column labels
		Object[] columnData = new Object[nSites+1];
		Arrays.fill(columnData, '.');
		columnData[0] = "taxon name";
		for (int i = 0; i < nSites; i+= 10) {
			String s = i + "";
			for (int j = 0; j < s.length(); j++) {
				columnData[i + j + 1] = s.charAt(j);
			}
		}
		JTable table = new JTable(tableData, columnData);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		table.setShowGrid(false);
		
		TableColumn col = table.getColumnModel().getColumn(0);
		col.setPreferredWidth(200);
		for (int i = 0; i < nSites; i++) {
			col = table.getColumnModel().getColumn(i+1);
			col.setPreferredWidth(10);		
		}
		JScrollPane horizontalScrollPane = new JScrollPane(table);

		add(horizontalScrollPane);
		horizontalScrollPane.addPropertyChangeListener(new FixedColumnTable(1, horizontalScrollPane));
		
	}

	public static void main(String[] args) {
		try {
			NexusParser parser = new NexusParser();
			parser.parseFile(args[0]);
			Alignment data = parser.m_alignment;
			AlignmentViewer panel = new AlignmentViewer(data);
			JFrame frame = new JFrame("Alignment Inspector");
			frame.getContentPane().add(panel);
			frame.setSize(1024, 768);
			panel.setPreferredSize(frame.getSize());
			frame.setVisible(true);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
