package beast.app.draw;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.LookAndFeel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.TableModelEvent;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;


import beast.core.Distribution;
import beast.core.Input;
import beast.core.Logger;
import beast.core.Plugin;
import beast.core.StateNode;
import beast.util.XMLParser;

/** panel that shows a beast model as a spreadsheet **/
public class SpreadSheet extends JPanel {
	private static final long serialVersionUID = 1L;

	/** list of available Plugin objects **/
	List<Plugin> m_plugins;
	/**
	 * maps plugins to locations on the spreadsheet. Locations are encoded as (x
	 * + MAX_ROW * y)
	 **/
	static HashMap<Plugin, Integer> m_pluginLocation;
	/** objects associated with the spread sheet cells **/
	Object[][] m_objects;
	/** string representation of objects **/
	String[][] m_rows;
	/** rendering style for cell **/
	CellFormat [][] m_cellFormat;
	class CellFormat {
		Color m_bgColor;
		Color m_fgColor;
		Font m_font;
		Border m_border;
		int m_alignment = SwingConstants.LEFT;
	}
	/** main table of spreadsheet **/
	JTable m_table;
	/** row labels on the left of the spreadsheet **/
	JTable m_rowHeaderTable;
	/** for managing cancelled edits **/
	String m_sOldValue;
	/** current directory for opening files **/
	String m_sDir = System.getProperty("user.dir");
	
    /** nr of rows and columns in the spreadsheet **/
	static int MAX_ROW = 255;
	int MAX_COL = 32;

	SpreadSheet() {
		m_rows = new String[MAX_ROW][MAX_COL];
		m_objects = new Object[MAX_ROW][MAX_COL];
		m_cellFormat = new CellFormat[MAX_ROW][MAX_COL];
		String[] headers = new String[MAX_COL];
		String abc = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		for (int i = 0; i < MAX_COL; i++) {
			headers[i] = (i >= 26 ? abc.charAt(i / 26 - 1) : ' ') + "" + abc.charAt(i % 26) + "";
		}
		m_table = new JTable(m_rows, headers) {
			private static final long serialVersionUID = 1L;

			@Override
			public void tableChanged(TableModelEvent e) {
				System.err.println("tableChanged");
				super.tableChanged(e);
				int iRow = getSelectedRow();
				int iCol = getSelectedColumn();
				if (iRow < 0 || iCol < 0) {
					return;
				}
				if (m_objects[iRow][iCol] instanceof Plugin) {
					m_rows[iRow][iCol] = SpreadSheet.toString((Plugin) m_objects[iRow][iCol]);
				} else if (m_rows[iRow][iCol] == null) {
					return;
				} else if (m_rows[iRow][iCol].startsWith("=")) {
					m_objects[iRow][iCol] = new FormulaCell(m_rows[iRow][iCol]);
					m_rows[iRow][iCol] = ((FormulaCell)m_objects[iRow][iCol]).toString();
				} else {
					m_objects[iRow][iCol] = m_rows[iRow][iCol]; 
				}

			}
			@Override
			public void editingCanceled(ChangeEvent e) {
				System.err.println("editingCanceled " + e);
				int iRow = getSelectedRow();
				int iCol = getSelectedColumn();
				if (iRow < 0 || iCol < 0) {
					return;
				}
				m_rows[iRow][iCol] = m_sOldValue;
				repaint();
			}
			@Override
			public void editingStopped(ChangeEvent e) {
				System.err.println("editingStopped " + e);
				super.editingStopped(e);
			}
			@Override
			public String getToolTipText(MouseEvent e) {
				Point p = e.getPoint();
				int iRow = rowAtPoint(p);
				int iCol = columnAtPoint(p);
				if (iRow < 0 || iCol < 0) {
					return null;
				}
				Object o = m_objects[iRow][iCol];
				if (o instanceof Plugin) {
					return ((Plugin)o).getDescription();
				} else if (o instanceof FormulaCell) {
					return ((FormulaCell)o).m_sMessage;
				} else {
					return null;
				}
			}
			 
			// public TableCellRenderer getCellRenderer(int row, int column) {
			// // if (m_rows[row][column] instanceof Plugin) {
			// // return m_pluginRenderer;
			// // }
			// // else...
			// return super.getCellRenderer(row, column);
			// }

		};

		m_table.addMouseListener(new MouseListener() {
			@Override
			public void mouseReleased(MouseEvent e) {
			}
			@Override
			public void mousePressed(MouseEvent e) {
			}
			@Override
			public void mouseExited(MouseEvent e) {
			}
			@Override
			public void mouseEntered(MouseEvent e) {
			}
			@Override
			public void mouseClicked(MouseEvent e) {
				edit();
			}
		});
		
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
				if (e.getKeyCode() == 113) {
					edit();
				}
				//System.err.println(e.getKeyCode());
			}
		});

		m_table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		m_table.setDefaultRenderer(Object.class, new ColoredTableCellRenderer());
		JScrollPane scrollPane = new JScrollPane(m_table);

		m_table.getColumnModel().getColumn(0).setPreferredWidth(100);
		m_table.getColumnModel().getColumn(1).setPreferredWidth(400);

		// set up row labels
		DefaultTableModel headerData = new DefaultTableModel(0, 1);
		for (int i = 0; i < MAX_ROW; i++) {
			headerData.addRow(new Object[] { (i + 1) + "" });
		}
		m_rowHeaderTable = new JTable(headerData);
		LookAndFeel.installColorsAndFont(m_rowHeaderTable, "TableHeader.background", "TableHeader.foreground",
				"TableHeader.font");
		m_rowHeaderTable.setIntercellSpacing(new Dimension(0, 0));
		Dimension d = m_rowHeaderTable.getPreferredScrollableViewportSize();
		d.width = m_rowHeaderTable.getPreferredSize().width;
		m_rowHeaderTable.setPreferredScrollableViewportSize(d);
		m_rowHeaderTable.setRowHeight(m_table.getRowHeight());
		scrollPane.setRowHeaderView(m_rowHeaderTable);

		// height listener handles resizing of row heights
		RowHeightListener heightLtr = new RowHeightListener();
		m_rowHeaderTable.addMouseListener(heightLtr);
		m_rowHeaderTable.addMouseMotionListener(heightLtr);

		this.setLayout(new BorderLayout());
		this.add(scrollPane, BorderLayout.CENTER);
		JToolBar toolBar = createToolBar();
		this.add(toolBar, BorderLayout.NORTH);
	} // c'tor
	
	void init() {
		for (int i = 0; i < MAX_ROW; i++) {
    		for (int j = 0; j < MAX_COL; j++) {
    			m_rows[i][j] = null;
    			m_objects[i][j] = null;
    			m_cellFormat[i][j] = null;
    		}
		}
		// m_pluginRenderer = new PluginRenderer();
		m_pluginLocation = new HashMap<Plugin, Integer>();
		for (int i = 0; i < m_plugins.size(); i++) {
			Plugin plugin = m_plugins.get(i);
			m_rows[i][0] = plugin.getID();
			m_objects[i][1] = m_rows[i][0];
			if (m_rows[i][0] == null || m_rows[i][0].equals("")) {
				m_rows[i][0] = plugin.getClass().getName().substring(plugin.getClass().getName().lastIndexOf('.') + 1);
			}
			m_objects[i][1] = plugin;
			m_pluginLocation.put(plugin, i + MAX_ROW);
			if (plugin instanceof StateNode || plugin instanceof Distribution) {
				m_objects[i][2] = new FormulaCell("=$B"+(i+1));
				m_rows[i][2] = ((FormulaCell)m_objects[i][2]).toString();
			}
		}
		for (int i = 0; i < m_plugins.size(); i++) {
			Plugin plugin = m_plugins.get(i);
			m_rows[i][1] = toString(plugin);// .toString();
		}
		m_table.repaint();
	} // init
	

	JToolBar createToolBar() {
		JToolBar toolBar = new JToolBar();
		toolBar.add(new MyAction("Load", "Load Graph", "open", "ctrl O") {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent ae) {
	            JFileChooser fc = new JFileChooser(m_sDir);
				fc.addChoosableFileFilter(new FileFilter() {
					public boolean accept(File f) {
						return f.isDirectory() || f.getName().toLowerCase().endsWith(getExtention());
					}
					public String getExtention(){return ".xml";}
					public String getDescription() {return "Beast II xml files";}
				});
				fc.setDialogTitle("Load Beast II file");
				int rval = fc.showOpenDialog(null);
				if (rval == JFileChooser.APPROVE_OPTION) {
					String sFileName = fc.getSelectedFile().toString();
					if (sFileName.lastIndexOf('/') > 0) {
						m_sDir = sFileName.substring(0, sFileName.lastIndexOf('/'));
					}
					try {
						readXML(sFileName);
						init();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
	        }
		});
		toolBar.add(new MyAction("New", "New", "new", "ctrl N") {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent ae) {
	    		m_pluginLocation = new HashMap<Plugin, Integer>();
	    		m_plugins = new ArrayList<Plugin>();
	        	init();
	        }
		});
		toolBar.addSeparator();
		toolBar.add(new MyAction("Background", "Background color", "color", "") {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent ae) {
	        	int iRow = m_table.getSelectedRow();
	        	int iCol = m_table.getSelectedColumn();
	        	if (iRow < 0 || iCol < 0) {
	        		return;
	        	}
	        	Color initialColor = Color.white;
	        	if (m_cellFormat[iRow][iCol] != null) {
	        		initialColor = m_cellFormat[iRow][iCol].m_bgColor;
	        	}
	            Color color = JColorChooser.showDialog(null, "Select Background color", initialColor);
	            if (color != null) {
		        	if (m_cellFormat[iRow][iCol] == null) {
		        		m_cellFormat[iRow][iCol] = new CellFormat();
		        	}
		        	m_cellFormat[iRow][iCol].m_bgColor = color;
	            }
	        }
		});
		toolBar.add(new MyAction("Color", "Text color", "tcolor", "") {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent ae) {
	        	int iRow = m_table.getSelectedRow();
	        	int iCol = m_table.getSelectedColumn();
	        	if (iRow < 0 || iCol < 0) {
	        		return;
	        	}
	        	Color initialColor = Color.white;
	        	if (m_cellFormat[iRow][iCol] != null) {
	        		initialColor = m_cellFormat[iRow][iCol].m_fgColor;
	        	}
	            Color color = JColorChooser.showDialog(null, "Select Text color", initialColor);
	            if (color != null) {
		        	if (m_cellFormat[iRow][iCol] == null) {
		        		m_cellFormat[iRow][iCol] = new CellFormat();
		        	}
		        	m_cellFormat[iRow][iCol].m_fgColor = color;
	            }
	        }
		});
		toolBar.addSeparator();
		toolBar.add(new MyAction("Bold", "Toggle boldness of text", "bold", "") {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent ae) {
	        	toggleFontProperty(Font.BOLD);
	        }
		});
		toolBar.add(new MyAction("Italic", "Toggle italicness of text", "italic", "") {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent ae) {
	        	toggleFontProperty(Font.ITALIC);
	        }
		});
		toolBar.add(new MyAction("Bigger", "Increase size of font", "bigger", "") {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent ae) {
	        	toggleFontProperty(-1);
	        }
		});
		toolBar.add(new MyAction("Smaller", "Decrease size of font", "smaller", "") {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent ae) {
	        	toggleFontProperty(-3);
	        }
		});
		toolBar.add(new MyAction("Font", "Set font", "font", "") {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent ae) {
	        	setFont();
	        }
		});
		toolBar.addSeparator();
		toolBar.add(new MyAction("Left", "Align left", "left", "") {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent ae) {
	        	setAlignemnt(SwingConstants.LEFT);
	        }
		});
		toolBar.add(new MyAction("Right", "Align right", "right", "") {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent ae) {
	        	setAlignemnt(SwingConstants.RIGHT);
	        }
		});
		toolBar.add(new MyAction("Center", "Align center", "center", "") {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent ae) {
	        	setAlignemnt(SwingConstants.CENTER);
	        }
		});
		return toolBar;
	} // createToolBar

	void setAlignemnt(int nAlignment) {
    	int iRow = m_table.getSelectedRow();
    	int iCol = m_table.getSelectedColumn();
    	if (iRow < 0 || iCol < 0) {
    		return;
    	}
       	if (m_cellFormat[iRow][iCol] == null) {
       		m_cellFormat[iRow][iCol] = new CellFormat();
       	}
       	m_cellFormat[iRow][iCol].m_alignment = nAlignment;
	} // setAlignment

	void setFont() {
    	int iRow = m_table.getSelectedRow();
    	int iCol = m_table.getSelectedColumn();
    	if (iRow < 0 || iCol < 0) {
    		return;
    	}
    	Font initialFont = null;
    	if (m_cellFormat[iRow][iCol] != null) {
    		initialFont = m_cellFormat[iRow][iCol].m_font;
    	}
	    JFontChooser fontChooser = new JFontChooser();
	    fontChooser.setFont(initialFont);
	    int result = fontChooser.showDialog(null);
	    if (result == JFontChooser.OK_OPTION)
	    {
	       	if (m_cellFormat[iRow][iCol] == null) {
	       		m_cellFormat[iRow][iCol] = new CellFormat();
	       	}
	       	m_cellFormat[iRow][iCol].m_font = fontChooser.getSelectedFont();
	    }
	    m_table.repaint();
	} // setFont

	void toggleFontProperty(int nStyle) {
		int iRow = m_table.getSelectedRow();
		int iCol = m_table.getSelectedColumn();
		if (iRow < 0 || iCol < 0) {
			return;
		}
       	if (m_cellFormat[iRow][iCol] == null) {
       		m_cellFormat[iRow][iCol] = new CellFormat();
       	}
   		if (m_cellFormat[iRow][iCol].m_font == null) {
   	       	Font font = m_table.getFont(); 
   			m_cellFormat[iRow][iCol].m_font = new Font(font.getName(), font.getStyle(), font.getSize());
   		}
       	Font font = m_cellFormat[iRow][iCol].m_font; 
       	if (nStyle > 0) {
       		m_cellFormat[iRow][iCol].m_font = new Font(font.getName(), font.getStyle()^nStyle, font.getSize());
       	} else {
       		// change font size
       		m_cellFormat[iRow][iCol].m_font = new Font(font.getName(), font.getStyle(), font.getSize() + 2 + nStyle);
       	}
	    m_table.repaint();
	} // toggleFontProperty
	
	/** edit a cell using the appropriate editor **/
	void edit() {
		int iCol = m_table.getSelectedColumn();
		int iRow = m_table.getSelectedRow();
		if (iCol < 0 || iRow < 0) {
			return;
		}
		m_sOldValue = m_rows[iRow][iCol];
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
		} else 	if (o instanceof FormulaCell) {
			m_rows[iRow][iCol] = "="+((FormulaCell)o).m_sFormula;
			m_table.repaint();
		}

	} // edit

	void setCurrentValue(Object o) {
		int iRow = m_table.getSelectedRow();
		int iCol = m_table.getSelectedColumn();
		if (iRow == -1 || iCol == -1) {
			return;
		}
		m_objects[iRow][iCol] = o;
		if (o instanceof Plugin) {
			m_rows[iRow][iCol] = toString((Plugin) o);
		} else if (o instanceof String && ((String)o).startsWith("=")) {
			m_objects[iRow][iCol] = new FormulaCell((String)o);
			m_rows[iRow][iCol] = ((FormulaCell)m_objects[iRow][iCol]).toString();
		} else {
			m_rows[iRow][iCol] = (String) o;
		}
		m_table.repaint();
	} // setCurrentValue

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
					sStr += getLocation((Plugin) input.get());
				} else if (input.get() instanceof List) {
					sStr += "[";
					@SuppressWarnings("rawtypes")
					List list = (List) input.get();
					for (int j = 0; j < list.size(); j++) {
						Object o = list.get(j);
						if (o instanceof Plugin) {
							sStr += getLocation((Plugin) o);
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
			e.printStackTrace();
		}
		sStr += ")";
		return sStr;
	} // toString

	static String getLocation(Plugin plugin) {
		int iLocation = m_pluginLocation.get(plugin);
		char a = (char) ('A' + (iLocation / MAX_ROW));
		return "$" + a + (1 + iLocation % MAX_ROW);
	}

	/** class for resizing row height by dragging the mouse over the row header **/
	class RowHeightListener extends MouseInputAdapter {
		Point first, last;

		@Override
		public void mouseMoved(MouseEvent e) {
			Point point = e.getPoint();
			int iRow = m_rowHeaderTable.rowAtPoint(point);
			Rectangle rect = m_rowHeaderTable.getCellRect(iRow, 0, true);
			if (rect.y + rect.height - point.y < 5) {
				m_rowHeaderTable.setCursor(Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR));
			} else {
				m_rowHeaderTable.setCursor(Cursor.getDefaultCursor());
			}
		}

		@Override
		public void mouseDragged(MouseEvent e) {
			if (first == null) {
				first = e.getPoint();
				int iRow = m_rowHeaderTable.rowAtPoint(first);
				Rectangle rect = m_rowHeaderTable.getCellRect(iRow, 0, true);
				if (rect.y + rect.height - first.y < 5) {
				} else {
					first = null;
					return;
				}
			} else {
				last = e.getPoint();
				if (last == null) {
					System.err.println("last = 0");
					return;
				}
				int height = (int) (last.y - first.y);
				if (height == 0) {
					System.err.println("height = 0");
					return;
				}
				int row = m_rowHeaderTable.rowAtPoint(first);
				int rowHeight = m_rowHeaderTable.getRowHeight(row);
				m_table.setRowHeight(row, Math.max(1, rowHeight + height));
				m_rowHeaderTable.setRowHeight(row, Math.max(1, rowHeight + height));
				first.y = last.y;
				System.err.println("rowheight = " + Math.max(1, rowHeight + height));
			}
			last = e.getPoint();
		}

		@Override
		public void mouseReleased(MouseEvent e) {
		}

		@Override
		public void mouseClicked(MouseEvent e) {
		}
	} // class RowHeightListener

    // a JavaScript engine
    static ScriptEngine m_engine;
    /** class representing a cell containing a formula, which is evaluated using the JavaScript engine **/
	class FormulaCell {
	    {
			// create a script engine manager
		    ScriptEngineManager factory = new ScriptEngineManager();
		    // create a JavaScript engine
		    m_engine = factory.getEngineByName("JavaScript");
	    }
		// formula string represented by this cell
		String m_sFormula;
		String m_sValue;
		String m_sMessage;
		// formula split up in tokens consisting of cell references and non-cell reference parts
		List<String> m_sTokens;
		List<Integer> m_nTokens;
		
		FormulaCell(String sFormula) {
			m_sFormula = sFormula.substring(1);
			try {
				parse(m_sFormula);
				m_sValue = value().toString();
			} catch (Exception e) {
				m_sMessage = e.getMessage();
				m_sValue = "NaN";
			}
		}
			
		Object getCellRef(int nCode) {
			int iRow = nCode & MAX_ROW;
			int iCol = nCode / MAX_ROW;
			Object o = m_objects[iRow][iCol];
			if (o instanceof FormulaCell) {
				return ((FormulaCell)o).value();
			} else if (o instanceof StateNode) {
				return ((StateNode)o).getArrayValue();
			} else if (o instanceof Distribution) {
				return ((Distribution)o).getCurrentLogP();
			} else if (o instanceof String) {
				return Double.parseDouble((String) o);
			}
			return Double.NaN;
		}
			
		Object value() {
			String sFormula = "with (Math) {";
			for (int i = 0; i < m_nTokens.size(); i++) {
				if (m_nTokens.get(i) >= 0) {
					sFormula += getCellRef(m_nTokens.get(i));
				} else {
					sFormula += m_sTokens.get(i);
				}
			}
			sFormula += "}";
			System.err.println("parsing " + sFormula);
			try {
				Object o = m_engine.eval(sFormula);
				return o;
			} catch (javax.script.ScriptException es) {
				return es.getMessage();
			}
		}
		
		public String toString() {
			return m_sValue;
		}
		
		/**splits the formula in tokens of cell-refs and non-cell refs **/
		void parse(String sFormula) throws Exception {
			m_sTokens = new ArrayList<String>();
			m_nTokens = new ArrayList<Integer>();
	    	Pattern pattern = Pattern.compile("([^\\$]*)\\$([A-Z0-9]*)(.*)");
	    	Pattern cellPattern = Pattern.compile("([A-Z]+)([0-9]+)");
			while (sFormula.length() > 0) {
				Matcher matcher = pattern.matcher(sFormula);
				if (matcher.matches()) {
					String sPre = matcher.group(1);
					if (sPre.length() > 0) {
						m_sTokens.add(sPre);
						m_nTokens.add(-1);
					}
					String sCellRef = matcher.group(2);
					Matcher cellMatcher = cellPattern.matcher(sCellRef);
					if (cellMatcher.matches()) {
						String sCol = cellMatcher.group(1);
						String sRow = cellMatcher.group(2);
						int nCellReff = sCol.charAt(0) - 'A'; 
						for (int i = 1;i < sCol.length(); i++) {
							nCellReff = (nCellReff+1) * 26 + sCol.charAt(i) - 'A'; 
						}
						nCellReff = nCellReff * (MAX_ROW+1) + Integer.parseInt(sRow) - 1;
						m_sTokens.add(sCellRef);
						m_nTokens.add(nCellReff);
					} else {
						throw new Exception("Not a valid cell reference '$" + sCellRef + "'");
					}
					sFormula = matcher.group(3);
				} else {
					m_sTokens.add(sFormula);
					m_nTokens.add(-1);
					sFormula = "";
				}
			}
		} // parse
	} // class FormulaCell

	class ColoredTableCellRenderer extends DefaultTableCellRenderer {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public Component getTableCellRendererComponent (JTable table, Object value, boolean bSelected, boolean bFocused, int iRow, int iCol) {

			setEnabled(table == null || table.isEnabled()); // see question above

	        if (m_cellFormat[iRow][iCol] != null) {
	            setBackground(m_cellFormat[iRow][iCol].m_bgColor);
	            setForeground(m_cellFormat[iRow][iCol].m_fgColor);
	            setHorizontalAlignment(m_cellFormat[iRow][iCol].m_alignment);
	            setBorder(m_cellFormat[iRow][iCol].m_border);
			} else {
	            setBackground(null);
	            setForeground(null);
	            setHorizontalAlignment(SwingConstants.LEFT);
	            setBorder(null);
			}

	        super.getTableCellRendererComponent(table, value, bSelected, bFocused, iRow, iCol);

	        if (m_cellFormat[iRow][iCol] != null) {
	            setFont(m_cellFormat[iRow][iCol].m_font);
			} else {
	            setFont(null);
			}
	        return this;
	    }
	} // class ColoredTableCellRenderer
	
	/**
	 * Rudimentary test of this panel, takes a Beast II xml file as argument and
	 * opens it
	 **/
	public static void main(String args[]) {
		try { 

			Logger.FILE_MODE = Logger.FILE_OVERWRITE;
			SpreadSheet spreadSheet = new SpreadSheet();
			spreadSheet.setSize(2048, 2048);
			spreadSheet.readXML(args[0]);
			spreadSheet.init();

			JFrame frame = new JFrame("Beast II Calculator");
			frame.getContentPane().add(spreadSheet, BorderLayout.CENTER);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setSize(800, 600);
			frame.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	} // main

} // class SpreadSheet
