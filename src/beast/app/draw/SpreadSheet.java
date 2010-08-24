package beast.app.draw;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.UIManager;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.TableModelEvent;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

import beast.core.Input;
import beast.core.Logger;
import beast.core.Plugin;
import beast.util.XMLParser;

/** panel that shows a beast model as a spreadsheet **/
public class SpreadSheet extends JPanel {
	private static final long serialVersionUID = 1L;

	List<Plugin> m_plugins;
	JTable m_table;
	String[][] m_rows;
	Object[][] m_objects;
	// PluginRenderer m_pluginRenderer;

	int MAX_ROW = 255;
	int MAX_COL = 32;

	void init() {
		// m_pluginRenderer = new PluginRenderer();
		m_rows = new String[MAX_ROW][MAX_COL];
		m_objects = new Object[MAX_ROW][MAX_COL];
		String[] headers = new String[MAX_COL];
		String abc = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		for (int i = 0; i < MAX_COL; i++) {
			headers[i] = (i >= 26 ? abc.charAt(i / 26 - 1) : ' ') + ""
					+ abc.charAt(i % 26) + "";
		}
		for (int i = 0; i < m_plugins.size(); i++) {
			Plugin plugin = m_plugins.get(i);
			m_rows[i][0] = plugin.getID();
			m_objects[i][1] = m_rows[i][0];
			if (m_rows[i][0] == null || m_rows[i][0].equals("")) {
				m_rows[i][0] = plugin
						.getClass()
						.getName()
						.substring(
								plugin.getClass().getName().lastIndexOf('.') + 1);
			}
			m_rows[i][1] = toString(plugin);// .toString();
			m_objects[i][1] = plugin;
		}

		m_table = new JTable(m_rows, headers) {
		 private static final long serialVersionUID = 1L;
		
		 public void tableChanged(TableModelEvent e) {
			 super.tableChanged(e);
			 int iRow = getSelectedRow();
			 int iCol = getSelectedColumn();
			 if (iRow < 0 || iCol < 0) {
				 return;
			 }
			 if (m_objects[iRow][iCol] instanceof Plugin) {
				 m_rows[iRow][iCol] = SpreadSheet.toString((Plugin) m_objects[iRow][iCol]);	 
			 }
			  
		 }
//		 public TableCellRenderer getCellRenderer(int row, int column) {
//		 // if (m_rows[row][column] instanceof Plugin) {
//		 // return m_pluginRenderer;
//		 // }
//		 // else...
//		 return super.getCellRenderer(row, column);
//		 }
		
		 };
		 m_table.setDragEnabled(true);
		HeightListener heightLtr = new HeightListener();  
		m_table.addMouseListener(heightLtr);  
		m_table.addMouseMotionListener(heightLtr);  
		m_table.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {
				edit();
			}

			@Override
			public void keyReleased(KeyEvent e) {
			}

			@Override
			public void keyPressed(KeyEvent e) {
				//System.err.println(e.getKeyCode());
			}
		});
		
		m_table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		JScrollPane scrollPane = new JScrollPane(m_table);

		m_table.getColumnModel().getColumn(0).setPreferredWidth(100);
		m_table.getColumnModel().getColumn(1).setPreferredWidth(400);

		// set up row labels
		ListModel lm = new AbstractListModel() {
			private static final long serialVersionUID = 1L;
			String headers[];
			{
				headers = new String[MAX_ROW];
				for (int i = 0; i < MAX_ROW; i++) {
					headers[i] = (i + 1) + "";
				}
			}

			public int getSize() {
				return headers.length;
			}

			public Object getElementAt(int index) {
				return headers[index];
			}
		};
		JList rowHeader = new JList(lm);
		rowHeader.setFixedCellWidth(50);
		rowHeader.setFixedCellHeight(m_table.getRowHeight());
		rowHeader.setCellRenderer(new RowHeaderRenderer(m_table));
		scrollPane.setRowHeaderView(rowHeader);
		this.setLayout(new BorderLayout());
		this.add(scrollPane, BorderLayout.CENTER);
		
	} // init

	
	
	void edit() {
		int iCol = m_table.getSelectedColumn();
		int iRow = m_table.getSelectedRow();
		Object o = m_objects[iRow][iCol];
		if (o instanceof Plugin) {
			Plugin plugin = (Plugin) o;
			PluginDialog dlg = new PluginDialog(plugin, plugin.getClass());
			dlg.setVisible(true);
			if (dlg.getOK()) {
				plugin = dlg.panel.m_plugin;
				m_rows[iRow][iCol] = toString(plugin);
				m_objects[iRow][iCol] = plugin;
				m_table.repaint();
			}
		}
	}

	void setCurrentValue(Object o) {
		int iSelectedRow = m_table.getSelectedRow();
		int iSelectedColumn = m_table.getSelectedColumn();
		if (iSelectedRow == -1 || iSelectedColumn == -1) {
			return;
		}
		m_objects[iSelectedRow][iSelectedColumn] = o;
		if (o instanceof Plugin) {
			m_rows[iSelectedRow][iSelectedColumn] = toString((Plugin) o);
		} else {
			m_rows[iSelectedRow][iSelectedColumn] = (String) o;
		}
		m_table.repaint();
	}

	void readXML(String sFile) throws Exception {
		XMLParser parser = new XMLParser();
		Plugin plugin = null;
		plugin = parser.parseFile(sFile);
		// collect all objects and store in m_objects
		m_plugins = new ArrayList<Plugin>();
		collectPlugins(plugin);
		m_plugins.add(plugin);
	} // readXML

	void collectPlugins(Plugin plugin) throws Exception {
		for (Plugin plugin2 : plugin.listActivePlugins()) {
			if (!m_plugins.contains(plugin2)) {
				m_plugins.add(plugin2);
				collectPlugins(plugin2);
			}
		}
	} // collectPlugins

	// public class PluginRenderer extends JLabel implements TableCellRenderer {
	// private static final long serialVersionUID = 1L;
	//
	// public PluginRenderer() {
	// setOpaque(true); //MUST do this for background to show up.
	// }
	//
	// public Component getTableCellRendererComponent(
	// JTable table, Object color,
	// boolean isSelected, boolean hasFocus,
	// int row, int col) {
	// Plugin plugin = (Plugin) m_rows[row][col];
	// String sText = toString(plugin);
	// System.err.println(sText);
	// setText(sText);
	// setToolTipText(plugin.getDescription());
	// return this;
	// }
	//
	//
	// } // class PluginRenderer

	static String toString(Plugin plugin) {
		String sClass = plugin.getClass().getName();
		String sStr = sClass.substring(sClass.lastIndexOf('.') + 1);
		sStr += "(";
		try {
			List<Input<?>> inputs = plugin.listInputs();
			for (int i = 0; i < inputs.size(); i++) {
				@SuppressWarnings("rawtypes")
				Input input = inputs.get(i);
				if (input.get() == null) {
					sStr += "null";
				} else if (input.get() instanceof Plugin) {
					sStr += "$" + ((Plugin) input.get()).getID();
				} else if (input.get() instanceof List) {
					sStr += "[";
					@SuppressWarnings("rawtypes")
					List list = (List) input.get();
					for (int j = 0; j < list.size(); j++) {
						Object o = list.get(j);
						if (o instanceof Plugin) {
							sStr += "$" + ((Plugin) o).getID();
						} else {
							sStr += o;
						}
						if (j < list.size() - 1) {
							sStr += ",";
						}
					}
					sStr += "]";
				} else {
					sStr += input.get().toString();
				}
				if (i < inputs.size() - 1) {
					sStr += ",";
				}
			}
		} catch (Exception e) {
			// ignore
		}
		sStr += ")";
		return sStr;
	} // toString


	 class HeightListener extends MouseInputAdapter {  
		     Point first , last;  
				@Override
		     public void mouseDragged(MouseEvent e) {  
		       if(first == null) {  
		         first = e.getPoint();  
		         m_table.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));  
		       }  
		       last = e.getPoint();  
		     }  
				@Override
		     public void mouseReleased(MouseEvent e) {  
		       if(first == null) return;  
		       int height = (int) (last.getY() - first.getY());  
		       if(height == 0) {  
		         m_table.setCursor(Cursor.getDefaultCursor());  
		         first = null;  
		         return;  
		       }  
		       int row = m_table.rowAtPoint(first);  
		       int rowHeight = m_table.getRowHeight(row);  
		       m_table.setRowHeight(row, rowHeight + height);  
		       m_table.setCursor(Cursor.getDefaultCursor());  
		       first = null;  
		     }           
				@Override
				public void mouseClicked(MouseEvent e) {
					edit();
				}
		   }  
	class RowHeaderRenderer extends JLabel implements ListCellRenderer {
		private static final long serialVersionUID = 1L;
	
		RowHeaderRenderer(JTable table) {
			JTableHeader header = table.getTableHeader();
			setOpaque(true);
			setBorder(UIManager.getBorder("TableHeader.cellBorder"));
			setHorizontalAlignment(CENTER);
			setForeground(header.getForeground());
			setBackground(header.getBackground());
			setFont(header.getFont());
		}
	
		public Component getListCellRendererComponent(JList list, Object value,
				int index, boolean isSelected, boolean cellHasFocus) {
			setText((value == null) ? "" : value.toString());
			return this;
		}
	}

	/** Rudimentary test of this panel, takes a Beast II xml file as argument and opens it **/
	public static void main(String args[]) {
		try {
			Logger.FILE_MODE = Logger.FILE_OVERWRITE;
			SpreadSheet spreadSheet = new SpreadSheet();
			spreadSheet.setSize(2048, 2048);
			spreadSheet.readXML(args[0]);
			spreadSheet.init();

			JFrame frame = new JFrame("Label Header");
			frame.getContentPane().add(spreadSheet, BorderLayout.CENTER);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setSize(800, 600);
			frame.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	} // main
} // class SpreadSheet
