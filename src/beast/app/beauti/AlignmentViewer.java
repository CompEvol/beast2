package beast.app.beauti;


import java.util.Arrays;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import beast.evolution.alignment.Alignment;
import beast.evolution.datatype.DataType;
import beast.util.NexusParser;

import java.awt.Dimension;

public class AlignmentViewer extends JPanel {
	private static final long serialVersionUID = 1L;

	Object[][] tableData;
	Object[] columnData;

	public AlignmentViewer(Alignment data) throws Exception {
		int nSites = data.getSiteCount();
		int nTaxa = data.getNrTaxa();
		
		// set up table content
		tableData = new Object[nTaxa][nSites+1];
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
		columnData = new Object[nSites+1];
		Arrays.fill(columnData, '.');
		columnData[0] = "taxon name";
		for (int i = 0; i < nSites; i+= 10) {
			String s = i + "";
			for (int j = 0; j < s.length(); j++) {
				columnData[i + j + 1] = s.charAt(j);
			}
		}
		
	    final TableModel fixedColumnModel = new AbstractTableModel() {
	        public int getColumnCount() {
	          return 1;
	        }

	        public String getColumnName(int column) {
	          return columnData[column] + "";
	        }

	        public int getRowCount() {
	          return tableData.length;
	        }

	        public Object getValueAt(int row, int column) {
	          return tableData[row][column];
	        }
	      };

	      final TableModel mainModel = new AbstractTableModel() {
	        public int getColumnCount() {
	          return columnData.length - 1;
	        }

	        public String getColumnName(int column) {
	          return columnData[column + 1] + "";
	        }

	        public int getRowCount() {
	          return tableData.length;
	        }

	        public Object getValueAt(int row, int column) {
	          return tableData[row][column + 1];
	        }
	      };

	      JTable fixedTable = new JTable(fixedColumnModel);
	      fixedTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

	      JTable mainTable = new JTable(mainModel);
	      mainTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

	      ListSelectionModel model = fixedTable.getSelectionModel();
	      mainTable.setSelectionModel(model);

	      JScrollPane scrollPane = new JScrollPane(mainTable);
	      Dimension fixedSize = fixedTable.getPreferredSize();
	      JViewport viewport = new JViewport();
	      viewport.setView(fixedTable);
	      viewport.setPreferredSize(fixedSize);
	      viewport.setMaximumSize(fixedSize);
	      scrollPane.setCorner(JScrollPane.UPPER_LEFT_CORNER, fixedTable.getTableHeader());
	      scrollPane.setRowHeaderView(viewport);

		add(scrollPane);
	}

	public static void main(String[] args) {
		try {
			NexusParser parser = new NexusParser();
			parser.parseFile(args[0]);
			Alignment data = parser.m_alignment;
			AlignmentViewer panel = new AlignmentViewer(data);
			JFrame frame = new JFrame("Alignment Viewer");
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
