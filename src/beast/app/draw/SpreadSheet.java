package beast.app.draw;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.swing.Action;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.LookAndFeel;
import javax.swing.SwingConstants;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.event.TableModelEvent;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;

import beast.core.Distribution;
import beast.core.Input;
import beast.core.Logger;
import beast.core.Operator;
import beast.core.Plugin;
import beast.core.StateNode;
import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.Sequence;
import beast.util.ClassDiscovery;

import beast.util.XMLParser;
import beast.util.XMLProducer;

// Wishlist:
// TODO update all cells when a cell is changed
// TODO allow plugin text to be edited, dlg as backup
// TODO save/saveAs
// TODO cut/copy/paste/delete
// TODO undo/redo

/** panel that shows a beast model as a spreadsheet **/
@SuppressWarnings("serial")
public class SpreadSheet extends JPanel implements ClipboardOwner {
	int DEFAULT_COLUMN_WIDTH = 75;

	/** list of available Plugin objects **/
	List<Plugin> m_plugins;
	List<FormulaCell> m_formulas;
	/** maps plugins to locations on the spreadsheet. Locations are encoded as (x + MAX_ROW * y) **/
	static HashMap<Plugin, Integer> m_pluginLocation;
	static HashMap<FormulaCell, Integer> m_formulaLocation;
	/** objects associated with the spread sheet cells **/
	Object[][] m_objects;
	/** rendering style for cell **/
	CellFormat [][] m_cellFormat;

	/** link to toolbar, for toggeling visibility **/
	JToolBar m_toolBar;
	/** main table of spreadsheet **/
	JTable m_table;
	/** row labels on the left of the spreadsheet **/
	JTable m_rowHeaderTable;
	String[] headers;

	/** current directory for opening files **/
	String m_sDir = System.getProperty("user.dir");
	/** name of the current spreadsheet file **/
	String m_sSpreadsheetFileName = null;
	/** list of borders to choose from **/
	Border [] m_borders;
	Icon [] m_borderIcons;
	
    /** nr of rows and columns in the spreadsheet **/
	static int MAX_ROW = 255;
	int MAX_COL = 32;
	String [] m_sPlugInNames;

	/** undo stack **/
	List<UndoAction> m_actions;
	int m_iTopUndoAction;
	/** string for recording edit actions **/
	String m_sCellSpecs;
	/** for recording row, column and type of action, while edit action is in progress **/ 
	ArrayList<Integer> m_iEditRows;
	ArrayList<Integer> m_iEditCols;
	/** encodes type of edit action 'o' for object, 'f' for cell format, 'h' for row height, 'w' for column width **/
	ArrayList<String> m_iEditActions; 
	
	/** constructor **/
	SpreadSheet() {
        List<String> sPlugInNames = ClassDiscovery.find(beast.core.Plugin.class, ClassDiscovery.IMPLEMENTATION_DIR);
        m_sPlugInNames = sPlugInNames.toArray(new String[0]);
        headers = new String[MAX_COL];
		m_objects = new Object[MAX_ROW][MAX_COL];
		m_cellFormat = new CellFormat[MAX_ROW][MAX_COL];
		String abc = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		for (int i = 0; i < MAX_COL; i++) {
			headers[i] = (i >= 26 ? abc.charAt(i / 26 - 1) : ' ') + "" + abc.charAt(i % 26) + "";
		}
		m_table = new JTable(m_objects, headers) {
			@Override
			public void tableChanged(TableModelEvent e) {
				System.err.println("tableChanged");
				super.tableChanged(e);
				int iRow = getSelectedRow();
				int iCol = getSelectedColumn();
				if (iRow < 0 || iCol < 0) {
					return;
				}
				System.err.println(m_objects[iRow][iCol].getClass().getName());
				System.err.println(m_objects[iRow][iCol].toString());
			}
			@Override
			public void editingCanceled(ChangeEvent e) {
				System.err.println("editingCanceled " + e);
				int iRow = getSelectedRow();
				int iCol = getSelectedColumn();
				if (iRow < 0 || iCol < 0) {
					return;
				}
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
				if (e.getButton() == MouseEvent.BUTTON3) {
					int iRow = m_table.rowAtPoint(e.getPoint());
					int iCol = m_table.columnAtPoint(e.getPoint());
					if (iRow < 0 || iCol < 0) {
						return;
					}
					beginEditAction();
					editObject(iRow, iCol);
					selectCell(iRow, iCol);
                    String sClassName = (String) JOptionPane.showInputDialog(m_table.getParent(), "Select a constant",
                            "select", JOptionPane.PLAIN_MESSAGE,  null, m_sPlugInNames, null);
                    if (sClassName != null) {
                    	try {
                    		Plugin plugin = (beast.core.Plugin) Class.forName(sClassName).newInstance();
                    		m_objects[iRow][iCol] = plugin;
                			m_pluginLocation.put(plugin, iRow + iCol * MAX_ROW);
                			m_plugins.add(plugin);
                    		endEditAction();
                    	} catch (Exception ex) {
							ex.printStackTrace();
						}
                    	m_table.editCellAt(iRow, iCol);
                    }
				}
			}
		});
		
//		m_table.addKeyListener(new KeyListener() {
//			@Override
//			public void keyTyped(KeyEvent e) {
//			}
//
//			@Override
//			public void keyReleased(KeyEvent e) {
//			}
//
//			@Override
//			public void keyPressed(KeyEvent e) {
//				if (e.getModifiers() == KeyEvent.VK_CONTROL) {//e.getKeyCode() == 113) {
//					System.err.println(e.getKeyCode());
//				}
//			}
//		});

		m_table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		m_table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		m_table.setCellSelectionEnabled(true);
		m_table.setDefaultRenderer(Object.class, new MyTableCellRenderer());
		m_table.setDefaultEditor(Object.class, new TableCellEditor() {
			JComponent component = new JTextField();
			int m_iRow, m_iCol;
			@Override
			public boolean stopCellEditing() {
				Object o = m_objects[m_iRow][m_iCol];
				System.err.println("stopCellEditing "+ (o==null? "null":o.getClass().getName() + " " + o) + "  => " + ((JTextField)component).getText());
				String sText = ((JTextField)component).getText();
				if (m_objects[m_iRow][m_iCol] instanceof FormulaCell) {
					m_formulas.remove(m_objects[m_iRow][m_iCol]);
					m_formulaLocation.remove(m_objects[m_iRow][m_iCol]);
				}
				if (m_objects[m_iRow][m_iCol] == null || !(m_objects[m_iRow][m_iCol] instanceof Plugin)) {
					if (sText.startsWith("=")) {
						FormulaCell formula = new FormulaCell(sText);
						m_objects[m_iRow][m_iCol] = formula;
						m_formulas.add(formula);
						m_formulaLocation.put(formula, m_iRow + m_iCol * MAX_ROW);
					} else {
						m_objects[m_iRow][m_iCol] = sText;
					}
				} else {
					// it is a Plugin
				}
				m_table.removeEditor();
				endEditAction();
				return true;
			}
			
			@Override
			public boolean shouldSelectCell(EventObject anEvent) {
				System.err.println("shouldSelectCell");
				return true;
			}
			
			@Override
			public void removeCellEditorListener(CellEditorListener l) {
				System.err.println("removeCellEditorListener");
			}
			
			@Override
			public boolean isCellEditable(EventObject anEvent) {
				System.err.println("isCellEditable");
				return true;
			}
			
			@Override
			public Object getCellEditorValue() {
				System.err.println("getCellEditorValue");
				return null;
			}
			
			@Override
			public void cancelCellEditing() {
				System.err.println("cancelCellEditing");
			}
			
			@Override
			public void addCellEditorListener(CellEditorListener l) {
				System.err.println("addCellEditorListener");
			}
			
			@Override
			public Component getTableCellEditorComponent(JTable table, Object value,
					boolean isSelected, int row, int column) {
				if (!isSelected) {
					return null;
				}
				m_iRow = row;
				m_iCol = column;
				beginEditAction();
				editObject(m_iRow, m_iCol);
				selectCell(m_iRow, m_iCol);
				if (value == null) {
					((JTextField)component).setText("");
				} else if (value instanceof Plugin) {
					Plugin plugin = (Plugin) value;
					PluginDialog dlg = new PluginDialog(plugin, plugin.getClass());
					dlg.setVisible(true);
					if (dlg.getOK()) {
						plugin = dlg.panel.m_plugin;
						m_objects[m_iRow][m_iCol] = plugin;
						m_table.repaint();
					}
					return null;
				
				
				} else if (value instanceof FormulaCell) {
					((JTextField)component).setText("=" + ((FormulaCell)value).m_sFormula);
				} else if (value instanceof String) {
					((JTextField)component).setText((String)value);
				}
				return component; 
			}
		});
		JScrollPane scrollPane = new JScrollPane(m_table);

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

		// set up list of possible border configurations to choose from
		m_borders = new Border[30];
		int k = 0;
		m_borders[k++] = new CellBorder(0, 0, 0, 0);
		m_borders[k++] = new CellBorder(1, 1, 1, 1);
		m_borders[k++] = new CellBorder(1, 1, 1, 0);
		m_borders[k++] = new CellBorder(1, 1, 0, 1);
		m_borders[k++] = new CellBorder(1, 0, 1, 1);
		m_borders[k++] = new CellBorder(0, 1, 1, 1);
		m_borders[k++] = new CellBorder(1, 0, 1, 0);
		m_borders[k++] = new CellBorder(0, 1, 0, 1);
		m_borders[k++] = new CellBorder(1, 1, 0, 0);
		m_borders[k++] = new CellBorder(1, 0, 0, 1);
		m_borders[k++] = new CellBorder(0, 0, 1, 1);
		m_borders[k++] = new CellBorder(0, 1, 1, 0);
		m_borders[k++] = new CellBorder(1, 0, 0, 0);
		m_borders[k++] = new CellBorder(0, 1, 0, 0);
		m_borders[k++] = new CellBorder(0, 0, 1, 0);
		m_borders[k++] = new CellBorder(0, 0, 0, 1);
		m_borders[k++] = new CellBorder(2, 2, 2, 2);
		m_borders[k++] = new CellBorder(2, 2, 2, 0);
		m_borders[k++] = new CellBorder(2, 2, 0, 2);
		m_borders[k++] = new CellBorder(2, 0, 2, 2);
		m_borders[k++] = new CellBorder(0, 2, 2, 2);
		m_borders[k++] = new CellBorder(0, 2, 0, 2);
		m_borders[k++] = new CellBorder(2, 2, 0, 0);
		m_borders[k++] = new CellBorder(2, 0, 0, 2);
		m_borders[k++] = new CellBorder(0, 0, 2, 2);
		m_borders[k++] = new CellBorder(0, 2, 2, 0);
		m_borders[k++] = new CellBorder(2, 0, 0, 0);
		m_borders[k++] = new CellBorder(0, 2, 0, 0);
		m_borders[k++] = new CellBorder(0, 0, 2, 0);
		m_borders[k++] = new CellBorder(0, 0, 0, 2);
		m_borderIcons = new Icon[m_borders.length];
		for (int i = 0; i < m_borders.length; i++) {
			BufferedImage image = new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB);
			Graphics2D g = image.createGraphics();
			g.clearRect(0, 0, 20, 20);
			g.setColor(Color.white);
			g.fillRect(0, 0, 20, 20);
			m_borders[i].paintBorder(null, g, 1, 1, 18, 18);
			m_borderIcons[i] = new ImageIcon(image);
		}

		this.setLayout(new BorderLayout());
		this.add(scrollPane, BorderLayout.CENTER);
		m_toolBar = createToolBar();
		this.add(m_toolBar, BorderLayout.NORTH);
		
		// handle change of selection
		m_table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				updateActions();
			}
		});
		m_table.getColumnModel().addColumnModelListener(new TableColumnModelListener() {
			@Override
			public void columnSelectionChanged(ListSelectionEvent e) {
				updateActions();
			}
			@Override
			public void columnRemoved(TableColumnModelEvent e) {}
			@Override
			public void columnMoved(TableColumnModelEvent e) {}
			@Override
			public void columnMarginChanged(ChangeEvent e) {}
			@Override
			public void columnAdded(TableColumnModelEvent e) {}
		});	
		
		// override m_table actions
		m_table.getActionMap().put("copy",a_copy);
		m_table.getActionMap().put("paste",a_paste);
		m_table.getActionMap().put("cut",a_cut);
		m_table.getActionMap().put("delete",a_del);
		m_table.getActionMap().put("undo",a_undo);
		m_table.getActionMap().put("redo",a_redo);
		DEFAULT_COLUMN_WIDTH = m_table.getColumn(headers[0]).getWidth();
		newSpreadsheet();
	} // c'tor

	/**************************************************************************************/
	/** recalculation of cell contents when spreadsheet changes **/
	void recalculate() {
		System.err.println("recalculate");
		// first, collect all cells that need calculating
		boolean [] bDone = new boolean[m_formulas.size()];
		int nDone = 0;
		while (nDone < bDone.length) {
			boolean bProgress = false;
			for (int iFormula = 0; iFormula < m_formulas.size(); iFormula++) {
				if (!bDone[iFormula]) {
					// check if we can calculate iFormula yet
					FormulaCell formula = m_formulas.get(iFormula);
					boolean bCanCalculate = true;
					for (int iCell : formula.m_nTokens) {
						if (iCell >= 0) {
							Object o = m_objects[iCell%MAX_ROW][iCell/MAX_ROW];
							if (o instanceof FormulaCell) {
								int iFormula2 = m_formulas.indexOf(o);
								if (!bDone[iFormula2]) {
									bCanCalculate = false;
									break;
								}
							}
						}
					}
					if (bCanCalculate) {
						formula.value();
						bDone[iFormula] = true;
						bProgress = true;
						nDone++;
					}
				}
			}
			
			if (!bProgress) {
				System.err.println("Cycle in cell references. Cannot recalculate!");
				m_table.repaint();
			}
		}
		m_table.repaint();
	} // recalculate
	
	/**************************************************************************************/
	/** undo/redo stuff **/
	class UndoAction {
		UndoAction(String sOldCellSpecs, String sNewCellSpecs) {
			m_sOldCellSpecs = sOldCellSpecs;
			m_sNewCellSpecs = sNewCellSpecs;
		}
		String m_sOldCellSpecs;
		String m_sNewCellSpecs;
	} // class UndoAction

	/** start recording edit actions **/
	void beginEditAction() {
		m_sCellSpecs = "";
		m_iEditRows = new ArrayList<Integer>();
		m_iEditCols = new ArrayList<Integer>();
		m_iEditActions = new ArrayList<String>();
	}
	
	void editObject(int iRow, int iCol) {
		m_sCellSpecs += getCellAsText(iRow, iCol);
		if (m_iEditActions != null) {
			m_iEditActions.add("o");
			m_iEditRows.add(iRow);
			m_iEditCols.add(iCol);
		}
	}
	
	void editFormat(int iRow, int iCol) {
		m_sCellSpecs += iRow + " " + iCol + " {} y:";
		if (m_cellFormat[iRow][iCol] == null) {
			m_sCellSpecs += "null\n";
		} else {
			m_sCellSpecs += m_cellFormat[iRow][iCol].toString() + "\n";
		}
		if (m_iEditActions != null) {
			m_iEditActions.add("f");
			m_iEditRows.add(iRow);
			m_iEditCols.add(iCol);
		}
	}
	
	void editWidth(int iCol) {
		int nWidth = m_table.getColumn(headers[iCol]).getWidth();
		m_sCellSpecs += nWidth + " " + iCol + " {} w:\n";
		if (m_iEditActions != null) {
			m_iEditActions.add("w");
			m_iEditRows.add(-1);
			m_iEditCols.add(iCol);
		}
	}
	
	void editHeight(int iRow) {
		int nHeight = m_rowHeaderTable.getRowHeight(iRow);
		m_sCellSpecs += iRow + " " + nHeight + " {} h:\n";
		if (m_iEditActions != null) {
			m_iEditActions.add("h");
			m_iEditRows.add(iRow);
			m_iEditCols.add(-1);
		}
	}
	
	/** end recording edit actions, and create new action on undo stack **/
	void endEditAction() {
		while (m_iTopUndoAction > m_actions.size()) {
			m_actions.remove(m_iTopUndoAction);
		}
		String sOldSpecs = m_sCellSpecs;
		m_sCellSpecs = "";
		ArrayList<Integer> iEditRows = m_iEditRows;
		m_iEditRows = null;
		ArrayList<Integer> iEditCols = m_iEditCols;
		m_iEditCols = null;
		ArrayList<String> iEditActions = m_iEditActions;
		m_iEditActions = null;
		for (int i = 0; i < iEditRows.size(); i++) {
			switch (iEditActions.get(i).charAt(0)) {
			case 'f':
				editFormat(iEditRows.get(i), iEditCols.get(i));
				break;
			case 'o':
				editObject(iEditRows.get(i), iEditCols.get(i));
				break;
			case 'w':
				editWidth(iEditCols.get(i));
				break;
			case 'h':
				editHeight(iEditRows.get(i));
				break;
			}
		}
		String sNewSpecs = m_sCellSpecs;
		if (!sOldSpecs.equals(sNewSpecs)) {
			m_actions.add(new UndoAction(sOldSpecs, sNewSpecs));
			m_iTopUndoAction++;
		}
		updateActions();
		recalculate();
	} // endEditAction
	
	void undo() {
		if (m_iTopUndoAction == 0) {
			return;
		}
		m_iTopUndoAction--;
		String sOldSpecs = m_actions.get(m_iTopUndoAction).m_sOldCellSpecs;
		processSpreadSheetAsText(sOldSpecs, 0, 0);
		updateActions();
	} // undo
			
	void redo() {
		if (m_iTopUndoAction >= m_actions.size()) {
			return;
		}
		String sNewSpecs = m_actions.get(m_iTopUndoAction).m_sNewCellSpecs;
		processSpreadSheetAsText(sNewSpecs, 0, 0);
		m_iTopUndoAction++;
		updateActions();
	} // redo
	
	public class CellFormat {
		Color m_bgColor;
		Color m_fgColor;
		Font m_font;
		Border m_border;
		int m_alignment = SwingConstants.LEFT;
		public CellFormat() {}
		
		public String toString() {
			String sStr = "";
			if (m_bgColor != null) {
				sStr += "bg:" + m_bgColor.getRGB() + " ";
			}
			if (m_fgColor != null) {
				sStr += "fg:" + m_fgColor.getRGB() + " ";
			}
			if (m_font != null) {
				sStr += "font:" + m_font.getFontName() + "," + m_font.getStyle() + "," + m_font.getSize() + " ";
			}
			if (m_border != null) {
				CellBorder border = (CellBorder) m_border;
				sStr += "border:" + border.m_nNorthThickness +"," + border.m_nEastThickness+","+border.m_nSouthThickness+","+border.m_nWestThickness+
					","+border.m_color.getRGB() +" ";
			}
			sStr += "align:" + m_alignment;
			return sStr;
		}
	} // class CellFormat
	
	/** reconstruct a CellFormat from a string that a CellFormat.toString() produces **/
	void parseCellFormat(String sStr, int iRow, int iCol) {
		if (sStr.equals("null")) {
			m_cellFormat[iRow][iCol] = null;
			return;
		}
		CellFormat format = (m_cellFormat[iRow][iCol] != null ? m_cellFormat[iRow][iCol] : new CellFormat());
		m_cellFormat[iRow][iCol] = format;
		String [] sStrs = sStr.split(" ");
		for (String sStr2 : sStrs) {
			String [] sStrs2 = sStr2.split(":");
			if (sStrs2[0].equals("bg")) {
				format.m_bgColor = new Color(Integer.parseInt(sStrs2[1]));
			} else if (sStrs2[0].equals("fg")) {
				format.m_fgColor = new Color(Integer.parseInt(sStrs2[1]));
			} else if (sStrs2[0].equals("font")) {
				String [] sStrs3 = sStrs2[1].split(",");
				format.m_font = new Font(sStrs3[0], Integer.parseInt(sStrs3[1]), Integer.parseInt(sStrs3[2]));
			} else if (sStrs2[0].equals("border")) {
				String [] sStrs3 = sStrs2[1].split(",");
				format.m_border = new CellBorder(Integer.parseInt(sStrs3[0]), Integer.parseInt(sStrs3[1]), Integer.parseInt(sStrs3[2]), Integer.parseInt(sStrs3[3]));
				((CellBorder)format.m_border).m_color = new Color(Integer.parseInt(sStrs3[4]));
			} else if (sStrs2[0].equals("align")) {
				format.m_alignment = Integer.parseInt(sStrs2[1]);
			}
		}
	} // parseCellFormat
	
	/** reconstruct a Plugin from a string that toString(Plugin) produces **/
	void parsePlugin(String sStr, int iRow, int iCol) {
		try {
			String sClass = sStr.substring(0, sStr.indexOf('('));
			for (String sClass2 : m_sPlugInNames) {
				if (sClass2.endsWith(sClass)) {
					sClass = sClass2;
				}
			}
			Plugin plugin = null;
			if (m_objects[iRow][iCol]==null || !m_objects[iRow][iCol].getClass().getName().equals(sClass)) {
				plugin = (Plugin) Class.forName(sClass).newInstance();
			} else {
				plugin = (Plugin) m_objects[iRow][iCol];
			}
			String sArgs = sStr.substring(sStr.indexOf('(') + 1, sStr.lastIndexOf(')'));
			List<Object> objects = new ArrayList<Object>();
			int i = 0;
			while (i < sArgs.length()) {
				if (sArgs.charAt(i) == '[') {
					String sArrayStr = sArgs.substring(i+1, sArgs.indexOf(']', i));
					String [] sStrs = sArrayStr.split(",");
					List<Object> objects2 = new ArrayList<Object>();
					for (String sStr2 : sStrs) {
						objects2.add(parseObject(sStr2));
					}
					objects.add(objects2);
					i = sArgs.indexOf(']', i) + 2; // +1 for ']' +1 for ','
				} else {
					int iComma = sArgs.indexOf(',', i);
					int iEnd = (iComma >= 0 ? iComma : sArgs.length());
					String sInputStr = sArgs.substring(i, iEnd);
					objects.add(parseObject(sInputStr));
					i = iEnd + 1;
				}
			}
			plugin.init(objects.toArray());
			m_pluginLocation.put(plugin, iRow + iCol * MAX_ROW);
			m_plugins.add(plugin);
			m_objects[iRow][iCol] = plugin;
		} catch (Exception e) {
			e.printStackTrace();
		}
	} // parsePlugin
	
	Object parseObject(String sStr) {
		if (sStr.equals("null")) {
			return null;
		}
		if (sStr.startsWith("$")) {
	    	Pattern cellPattern = Pattern.compile("([a-zA-Z]+)([0-9]+)");
			Matcher cellMatcher = cellPattern.matcher(sStr.substring(1));
			if (cellMatcher.matches()) {
				String sCol = cellMatcher.group(1).toUpperCase();
				String sRow = cellMatcher.group(2);
				int iCol = sCol.charAt(0) - 'A'; 
				for (int i = 1;i < sCol.length(); i++) {
					iCol = (iCol+1) * 26 + sCol.charAt(i) - 'A'; 
				}
				int iRow = Integer.parseInt(sRow) - 1;
				return m_objects[iRow][iCol];
			}
		}
		return sStr;
	} // parseObject

	

	/** file menu actions **/
	Action a_new = new MyAction("New", "New spreadsheet file", "new", "ctrl N") {
		public void actionPerformed(ActionEvent ae) {
			newSpreadsheet();
		}			
	};
	Action a_open = new MyAction("Open", "Open spreadsheet file or Import beast xml specification", "open", "ctrl O") {
		public void actionPerformed(ActionEvent ae) {
			openSpreadsheet();
		}			
	};
	Action a_save = new MyAction("Save", "Save spreadsheet file", "save", "ctrl S") {
		public void actionPerformed(ActionEvent ae) {
			saveSpreadsheet();
		}			
	};
	Action a_saveas = new MyAction("Save As", "Save spreadsheet file under new name", "saveas", "") {
		public void actionPerformed(ActionEvent ae) {
			saveSpreadsheetAs();
		}			
	};
	Action a_export = new MyAction("Export", "Export beast xml specification", "export", "") {
		public void actionPerformed(ActionEvent ae) {
			exportBeast();
		}			
	};
	Action a_exit = new MyAction("Exit", "Exit program", "exit", "ctrl Q") {
		public void actionPerformed(ActionEvent ae) {
			System.exit(0);
		}			
	};

	/** edit menu actions **/
	Action a_cut = new MyAction("Cut", "Cut", "cut", "ctrl X") {
		public void actionPerformed(ActionEvent ae) {
			copy();
			delete();
		}			
	};
	Action a_copy = new MyAction("Copy", "Copy", "copy", "ctrl C") {
		public void actionPerformed(ActionEvent ae) {
			copy();
		}			
	};
	Action a_del = new MyAction("Delete", "Delete", "del", "del") {
		public void actionPerformed(ActionEvent ae) {
			delete();
		}			
	};
	Action a_paste = new MyAction("Paste", "Paste", "paste", "ctrl V") {
		public void actionPerformed(ActionEvent ae) {
			paste();
		}			
	};
	Action a_undo = new MyAction("Undo", "Undo", "undo", "ctrl Z") {
		public void actionPerformed(ActionEvent ae) {
			undo();
		}			
	};
	Action a_redo = new MyAction("Redo", "Redo", "redo", "ctrl Y") {
		public void actionPerformed(ActionEvent ae) {
			redo();
		}			
	};
	/** window actions **/
    Action a_viewtoolbar = new MyAction("View toolbar", "View toolbar", "toolbar", "") {
        public void actionPerformed(ActionEvent ae) {
        	m_toolBar.setVisible(!m_toolBar.isVisible());
        }
    };
	/** help actions **/
    Action a_about = new MyAction("About", "Help about", "about", "") {
        public void actionPerformed(ActionEvent ae) {
            JOptionPane.showMessageDialog(null, "Beast II Spreadsheet\nRemco Bouckaert\nrrb@xm.co.nz\n2010\nGPL licence", "About Message", JOptionPane.PLAIN_MESSAGE);
        }
    };
	
	
	
	/** file action implementations **/
	void newSpreadsheet() {
		m_plugins = new ArrayList<Plugin>();
		m_formulas = new ArrayList<FormulaCell>();
		m_pluginLocation = new HashMap<Plugin, Integer>();
		m_formulaLocation = new HashMap<FormulaCell, Integer>();
		m_actions = new ArrayList<SpreadSheet.UndoAction>();
		m_iTopUndoAction = 0;

		for (int i = 0; i < MAX_ROW; i++) {
    		for (int j = 0; j < MAX_COL; j++) {
    			m_objects[i][j] = null;
    			m_cellFormat[i][j] = null;
    		}
		}
		m_table.repaint();
	}

	void openSpreadsheet() {
        JFileChooser fc = new JFileChooser(m_sDir);
		fc.addChoosableFileFilter(new FileFilter() {
			public boolean accept(File f) {
				return f.isDirectory() || f.getName().toLowerCase().endsWith(getExtention());
			}
			public String getExtention(){return ".bsp";}
			public String getDescription() {return "Beast Spreadsheet files";}
		});
		fc.addChoosableFileFilter(new FileFilter() {
			public boolean accept(File f) {
				return f.isDirectory() || f.getName().toLowerCase().endsWith(getExtention());
			}
			public String getExtention(){return ".xml";}
			public String getDescription() {return "Beast II xml files";}
		});
		fc.setDialogTitle("Load Beast Spreadheet file");
		int rval = fc.showOpenDialog(null);
		if (rval == JFileChooser.APPROVE_OPTION) {
			m_sSpreadsheetFileName = fc.getSelectedFile().toString();
			if (m_sSpreadsheetFileName.lastIndexOf('/') > 0) {
				m_sDir = m_sSpreadsheetFileName.substring(0, m_sSpreadsheetFileName.lastIndexOf('/'));
			}
			newSpreadsheet();
			if (m_sSpreadsheetFileName.toLowerCase().endsWith(".xml")) {
				try {
					readXML(m_sSpreadsheetFileName);
				} catch (Exception e) {
					e.printStackTrace();
				}
				m_sSpreadsheetFileName = m_sSpreadsheetFileName.substring(0, m_sSpreadsheetFileName.length()-4) + ".bsp";
			} else {
				try {
					BufferedReader fin = new BufferedReader(new FileReader(m_sSpreadsheetFileName));
					String sText = "";
					while (fin.ready()) {
						sText += fin.readLine();
					}
					fin.close();
					processSpreadSheetAsText(sText, 0, 0);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	void saveSpreadsheet() {
		if (m_sSpreadsheetFileName == null) {
			saveSpreadsheetAs();
		}
    	File file = new File(m_sSpreadsheetFileName);
    	if (file.exists()) {
    		System.err.println("Overwriting file " + m_sSpreadsheetFileName + ".");
    	}
    	try {
    		PrintStream out = new PrintStream(m_sSpreadsheetFileName);
    		m_table.selectAll();
    		// first the contents
    		out.print(getSpreadsheetAsText());
    		// then the cell widths & heights if any
    		int iRowEnd = m_table.getSelectionModel().getMaxSelectionIndex(); 
    		for (int iRow = m_table.getSelectedRow(); iRow < iRowEnd; iRow++) {
    			int nHeight = m_rowHeaderTable.getRowHeight(iRow);
    			if (nHeight != m_rowHeaderTable.getRowHeight()) {
    				out.print(iRow + " " + nHeight + " {} h:\n");
    			}
    		}
    		int iColEnd = m_table.getColumnModel().getSelectionModel().getMaxSelectionIndex();
    		for (int iCol = m_table.getSelectedColumn(); iCol < iColEnd; iCol++) {
    			int nWidth = m_table.getColumn(headers[iCol]).getWidth();
    			if (nWidth != DEFAULT_COLUMN_WIDTH) {
    				out.print(nWidth + " " + iCol + " {} w:\n");
    			}
    		}
    		out.close();
    	} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	void saveSpreadsheetAs() {
        JFileChooser fc = new JFileChooser(m_sDir);
		fc.addChoosableFileFilter(new FileFilter() {
			public boolean accept(File f) {
				return f.isDirectory() || f.getName().toLowerCase().endsWith(getExtention());
			}
			public String getExtention(){return ".bsp";}
			public String getDescription() {return "Beast Spreadsheet files";}
		});
		fc.setDialogTitle("Save Beast Spreadheet file");
		int rval = fc.showSaveDialog(null);
		if (rval == JFileChooser.APPROVE_OPTION) {
			m_sSpreadsheetFileName = fc.getSelectedFile().toString();
			if (m_sSpreadsheetFileName.lastIndexOf('/') > 0) {
				m_sDir = m_sSpreadsheetFileName.substring(0, m_sSpreadsheetFileName.lastIndexOf('/'));
			}
			try {
				saveSpreadsheet();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	void exportBeast() {
		int iRow = m_table.getSelectedRow(); 
		int iCol = m_table.getSelectedColumn();
		if (iRow < 0 || iCol < 0 || m_objects[iRow][iCol] == null || !(m_objects[iRow][iCol] instanceof beast.core.Runnable)) {
			JOptionPane.showMessageDialog(this, "Select a runnable plugin to export as Beast II xml file");
			return;
		}
        JFileChooser fc = new JFileChooser(m_sDir);
		fc.addChoosableFileFilter(new FileFilter() {
			public boolean accept(File f) {
				return f.isDirectory() || f.getName().toLowerCase().endsWith(getExtention());
			}
			public String getExtention(){return ".xml";}
			public String getDescription() {return "Beast II xml files";}
		});
		fc.setDialogTitle("Save Beast II xml file");
		if (m_sSpreadsheetFileName != null && m_sSpreadsheetFileName.endsWith(".bsp")) {
			fc.setSelectedFile(new File(m_sSpreadsheetFileName.substring(0, m_sSpreadsheetFileName.length()-4) + ".xml"));
		}
		int rval = fc.showSaveDialog(null);
		if (rval == JFileChooser.APPROVE_OPTION) {
			String sFileName = fc.getSelectedFile().toString();
			if (sFileName .lastIndexOf('/') > 0) {
				m_sDir = sFileName .substring(0, sFileName .lastIndexOf('/'));
			}
			try {
				Plugin plugin = (Plugin) m_objects[iRow][iCol];
				PrintStream out = new PrintStream(sFileName);
				out.print(new XMLProducer().toXML(plugin));
				out.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}
	
	
	/** edit action implementations **/
	
	
	void copy() {
		// Get the min and max ranges of selected cells 
		String sClipboardText = getSpreadsheetAsText();
	    StringSelection stringSelection = new StringSelection( sClipboardText );
	    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
	    clipboard.setContents( stringSelection, this );
		updateActions();
	} // copy
	
	
	void delete() {
		beginEditAction();
		// Get the min and max ranges of selected cells 
		int iRowStart = m_table.getSelectedRow(); 
		int iRowEnd = m_table.getSelectionModel().getMaxSelectionIndex(); 
		int iColStart = m_table.getSelectedColumn(); 
		int iColEnd = m_table.getColumnModel().getSelectionModel().getMaxSelectionIndex(); 
		// Check each cell in the range
		for (int iRow = iRowStart; iRow <= iRowEnd; iRow++) { 
			for (int iCol = iColStart; iCol <= iColEnd; iCol++) { 
				if (m_table.isCellSelected(iRow, iCol)) {
					editObject(iRow,iCol);
					if (m_objects[iRow][iCol] instanceof Plugin) {
						m_pluginLocation.remove(m_objects[iRow][iCol]);
						m_plugins.remove(m_objects[iRow][iCol]);
					} else if (m_objects[iRow][iCol] instanceof FormulaCell) {
						m_formulaLocation.remove(m_objects[iRow][iCol]);
						m_formulas.remove(m_objects[iRow][iCol]);
					}
					m_objects[iRow][iCol] = null;
				}
			}
		}
		m_table.repaint();
		endEditAction();
	} // delete
	
	void paste() {
		String sClipboard = "";
	    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
	    Transferable contents = clipboard.getContents(null);
	    boolean hasTransferableText = (contents != null) &&  contents.isDataFlavorSupported(DataFlavor.stringFlavor);
	    if ( hasTransferableText ) {
	      try {
	        sClipboard = (String)contents.getTransferData(DataFlavor.stringFlavor);
	      }
	      catch (UnsupportedFlavorException ex){
	        //highly unlikely since we are using a standard DataFlavor
	        ex.printStackTrace();
	      }
	      catch (IOException ex) {
	        ex.printStackTrace();
	      }
	    }
	    
		int iRowStart = m_table.getSelectedRow(); 
		int iColStart = m_table.getSelectedColumn(); 
		if (iRowStart < 0 || iColStart < 0) {
			return;
		}
		beginEditAction();
		processSpreadSheetAsText(sClipboard, iRowStart, iColStart);
		m_table.repaint();
        endEditAction();
	} // paste
	
	String getSpreadsheetAsText() {
		int iRowStart = m_table.getSelectedRow(); 
		int iRowEnd = m_table.getSelectionModel().getMaxSelectionIndex(); 
		int iColStart = m_table.getSelectedColumn(); 
		int iColEnd = m_table.getColumnModel().getSelectionModel().getMaxSelectionIndex();
		// Check each cell in the range
		String sClipboardText = iRowStart +" " + iColStart +" {} o:\n";
		for (int iRow = iRowStart; iRow <= iRowEnd; iRow++) { 
			for (int iCol = iColStart; iCol <= iColEnd; iCol++) { 
				if (m_table.isCellSelected(iRow, iCol) && m_objects[iRow][iCol] != null) {
					sClipboardText += getCellAsText(iRow, iCol);
				}
			}
		}
		return sClipboardText;
	}

	
	String getCellAsText(int iRow, int iCol) {
		Object o = m_objects[iRow][iCol];
		String sText = iRow + " " + iCol + " ";
		sText += "{" + (m_cellFormat[iRow][iCol]!=null ? m_cellFormat[iRow][iCol].toString() : "")+"}";
		if (o == null) {
			sText += " n:";
		} else if (o instanceof FormulaCell) {
			sText += " f:=" + ((FormulaCell) o).m_sFormula;
		} else if (o instanceof Plugin) {
			sText += " p:" + toString((Plugin) o);
		} else {
			sText += " s:" + o.toString();
		}
		sText += "\n";
		return sText;
	}	
	
	/** try to paste contents of sText into the spreadsheet
	 * @param sText: string of cell-specs, line by line, as produced by getSpreadsheetAsText()
	 * @param iRow0, iCol0: relative cell position
	 */
	void processSpreadSheetAsText(String sText, int iRow0, int iCol0) {
		String [] sStrs = sText.split("\n");
    	Pattern cellPattern = Pattern.compile("([0-9]+) ([0-9]+) \\{([^}]*)\\} ([a-z]):(.*)");
    	for (int i = 0; i < sStrs.length; i++) {
    		String sStr = sStrs[i];
    		Matcher cellMatcher = cellPattern.matcher(sStr);
    		if (cellMatcher.matches()) {
    			int iRow = Integer.parseInt(cellMatcher.group(1));
    			int iCol = Integer.parseInt(cellMatcher.group(2));
    			String sCellFormat = cellMatcher.group(3);
    			char aType = cellMatcher.group(4).charAt(0);
    			String sSpec = cellMatcher.group(5);
    			if (sCellFormat.length() > 0) {
					editFormat(iRow + iRow0, iCol + iCol0);
    				parseCellFormat(sCellFormat, iRow + iRow0, iCol + iCol0);
    			}
    			if (aType == 'o') {
    				// offset, should be on first line of sStrs only
    				iRow0 -= iRow;
    				iCol0 -= iCol;
    			} else if (aType == 'n') {
    				// null object
					editFormat(iRow + iRow0, iCol + iCol0);
    				m_objects[iRow + iRow0][iCol + iCol0] = null; 
    			} else if (aType == 'y') {
    				// cell format
					editFormat(iRow + iRow0, iCol + iCol0);
    				parseCellFormat(sSpec, iRow + iRow0, iCol + iCol0);
    			} else if (aType == 'p') {
    				// plugin
					editObject(iRow + iRow0, iCol + iCol0);
    				parsePlugin(sSpec, iRow + iRow0, iCol + iCol0);
    			} else if (aType == 'f') {
    				// cell formula
					editObject(iRow + iRow0, iCol + iCol0);
					FormulaCell formula = new FormulaCell(sSpec);
    				m_objects[iRow + iRow0][iCol + iCol0] = formula;
    				m_formulas.add(formula);
    				m_formulaLocation.put(formula, iRow + iRow0 + (iCol + iCol0) * MAX_ROW);
    			} else if (aType == 'w') {
    				// cell width
    				int nWidth = iRow;
					editWidth(iCol);
    				m_table.getColumnModel().getColumn(iCol).setPreferredWidth(nWidth); 
    			} else if (aType == 'h') {
    				// cell height
    				int nHeight = iCol;
					editHeight(iCol);
    				m_table.setRowHeight(iRow, nHeight);
    				m_rowHeaderTable.setRowHeight(iRow, nHeight);
    			} else {
    				// assume it is a string
					editObject(iRow + iRow0, iCol + iCol0);
    				m_objects[iRow + iRow0][iCol + iCol0] = sSpec;
    			}
    		} else {
    			// not a Beast spreadsheet paste, assume it is a string
				editObject(iRow0, iCol0);
    			m_objects[iRow0][iCol0] = sText;
    			return;
    		}
    	}
	} // processSpreadSheetAsText
	
	/** enable/disable actions depending on the state of the system **/
	void updateActions() {
		int iRow = m_table.getSelectedRow(); 
		int iCol = m_table.getSelectedColumn();
		boolean bSelected = iRow >= 0 && iCol >= 0 && m_objects[iRow][iCol] != null;
		a_copy.setEnabled(bSelected);
		a_cut.setEnabled(bSelected);
		
	    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
	    Transferable contents = clipboard.getContents(null);
	    boolean hasTransferableText = (contents != null) &&  contents.isDataFlavorSupported(DataFlavor.stringFlavor);
	    a_paste.setEnabled(hasTransferableText);
	    
	    a_undo.setEnabled(m_iTopUndoAction > 0);
	    a_redo.setEnabled(m_iTopUndoAction < m_actions.size());
	    m_table.repaint();
	} // updateActions
	
	JToolBar createToolBar() {
		JToolBar toolBar = new JToolBar();
		toolBar.add(a_new);
		toolBar.add(a_open);
		toolBar.add(a_save);
		toolBar.addSeparator();
		toolBar.add(a_cut);
		toolBar.add(a_copy);
		toolBar.add(a_del);
		toolBar.add(a_paste);
		toolBar.addSeparator();
		toolBar.add(a_undo);
		toolBar.add(a_redo);
		toolBar.addSeparator();
		toolBar.add(new MyAction("Background", "Background color", "color", "") {
			public void actionPerformed(ActionEvent ae) {
				setColor(true);
	        }
		});
		toolBar.add(new MyAction("Color", "Text color", "tcolor", "") {
			

			public void actionPerformed(ActionEvent ae) {
				setColor(false);
	        }
		});
		toolBar.addSeparator();
		toolBar.add(new MyAction("Bold", "Toggle boldness of text", "bold", "") {
			

			public void actionPerformed(ActionEvent ae) {
	        	toggleFontProperty(null,Font.BOLD,0);
	        }
		});
		toolBar.add(new MyAction("Italic", "Toggle italicness of text", "italic", "") {
			

			public void actionPerformed(ActionEvent ae) {
	        	toggleFontProperty(null,Font.ITALIC,0);
	        }
		});
		toolBar.add(new MyAction("Bigger", "Increase size of font", "bigger", "") {
			

			public void actionPerformed(ActionEvent ae) {
	        	toggleFontProperty(null, -1, 1);
	        }
		});
		toolBar.add(new MyAction("Smaller", "Decrease size of font", "smaller", "") {
			

			public void actionPerformed(ActionEvent ae) {
	        	toggleFontProperty(null, -1, 3);
	        }
		});
		
		GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
		JComboBox combo = new JComboBox(env.getAvailableFontFamilyNames());
	    combo.addActionListener(new ActionListener() {
	           public void actionPerformed(ActionEvent e) {
	        	   toggleFontProperty((String)((JComboBox)e.getSource(  )).getSelectedItem(), -1, -1);
	           }
	    });
	    toolBar.add(combo);
		combo.setMaximumSize(new Dimension(350, 20));
		
		toolBar.addSeparator();
		toolBar.add(new MyAction("Left", "Align left", "left", "") {
			

			public void actionPerformed(ActionEvent ae) {
	        	setAlignemnt(SwingConstants.LEFT);
	        }
		});
		toolBar.add(new MyAction("Right", "Align right", "right", "") {
			

			public void actionPerformed(ActionEvent ae) {
	        	setAlignemnt(SwingConstants.RIGHT);
	        }
		});
		toolBar.add(new MyAction("Center", "Align center", "center", "") {
			

			public void actionPerformed(ActionEvent ae) {
	        	setAlignemnt(SwingConstants.CENTER);
	        }
		});
		toolBar.addSeparator();
		
		
		
		JComboBox combo2 = new JComboBox(m_borderIcons);
		combo2.setRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
					boolean cellHasFocus) {
				    JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			        label.setIcon(m_borderIcons[Math.max(index, 0)]);
			        return label;				
			}
		});
		combo2.setMaximumSize(new Dimension(40, 20));
	    combo2.addActionListener(new ActionListener() {
	           public void actionPerformed(ActionEvent e) {
	        	   int i = ((JComboBox)e.getSource()).getSelectedIndex();
	        	   System.err.println("Selected " + i);
	        	   setBorderProperty(i);
	           }
	    });
	    toolBar.add(combo2);
		
		return toolBar;
	} // createToolBar

	void setBorderProperty(int iBorder) {
		beginEditAction();
		// Get the min and max ranges of selected cells 
		int iRowStart = m_table.getSelectedRow(); 
		int iRowEnd = m_table.getSelectionModel().getMaxSelectionIndex(); 
		int iColStart = m_table.getSelectedColumn(); 
		int iColEnd = m_table.getColumnModel().getSelectionModel().getMaxSelectionIndex(); 
		// Check each cell in the range 
		for (int iRow = iRowStart; iRow <= iRowEnd; iRow++) { 
			for (int iCol = iColStart; iCol <= iColEnd; iCol++) { 
				if (m_table.isCellSelected(iRow, iCol)) {  
					if (m_cellFormat[iRow][iCol] == null) {
						m_cellFormat[iRow][iCol] = new CellFormat();
					}
					editFormat(iRow, iCol);
					m_cellFormat[iRow][iCol].m_border = m_borders[iBorder];
				}
			}
		}
		endEditAction();
		m_table.repaint();
	}
	/** choose colour for foreground or background of selected cells **/
	void setColor(boolean bForeground) {
		beginEditAction();
		// Get the min and max ranges of selected cells 
		int iRowStart = m_table.getSelectedRow(); 
		int iColStart = m_table.getSelectedColumn(); 
    	if (iRowStart < 0 || iColStart < 0) {
    		return;
    	}
    	Color initialColor = Color.white;
    	if (m_cellFormat[iRowStart][iColStart] != null) {
    		initialColor = (bForeground?m_cellFormat[iRowStart][iColStart].m_fgColor : m_cellFormat[iRowStart][iColStart].m_bgColor);
    	}
        Color color = JColorChooser.showDialog(null, "Select Text color", initialColor);
        if (color != null) {
		
	    	int iRowEnd = m_table.getSelectionModel().getMaxSelectionIndex(); 
			int iColEnd = m_table.getColumnModel().getSelectionModel().getMaxSelectionIndex(); 
			// Check each cell in the range 
			for (int iRow = iRowStart; iRow <= iRowEnd; iRow++) { 
				for (int iCol = iColStart; iCol <= iColEnd; iCol++) { 
					if (m_table.isCellSelected(iRow, iCol)) {
						editFormat(iRow, iCol);
						if (m_cellFormat[iRow][iCol] == null) {
							m_cellFormat[iRow][iCol] = new CellFormat();
						}
						if (bForeground) {
							m_cellFormat[iRow][iCol].m_bgColor = color;
						} else {
							m_cellFormat[iRow][iCol].m_fgColor = color;
						}
					}
				}
			}
        }
		m_table.repaint();
		endEditAction();
	}
	
	/** set alignment of selected cells **/
	void setAlignemnt(int nAlignment) {
		beginEditAction();
		// Get the min and max ranges of selected cells 
		int iRowStart = m_table.getSelectedRow(); 
		int iRowEnd = m_table.getSelectionModel().getMaxSelectionIndex(); 
		int iColStart = m_table.getSelectedColumn(); 
		int iColEnd = m_table.getColumnModel().getSelectionModel().getMaxSelectionIndex(); 
		// Check each cell in the range 
		for (int iRow = iRowStart; iRow <= iRowEnd; iRow++) { 
			for (int iCol = iColStart; iCol <= iColEnd; iCol++) { 
				if (m_table.isCellSelected(iRow, iCol)) {
					editFormat(iRow, iCol);
					if (m_cellFormat[iRow][iCol] == null) {
						m_cellFormat[iRow][iCol] = new CellFormat();
					}
					m_cellFormat[iRow][iCol].m_alignment = nAlignment;
				}
			}
		}
		m_table.repaint();
		endEditAction();
	} // setAlignment

	/** set font properties of selected cells **/
	void toggleFontProperty(String sFontName, int nFontStyle, int nFontSize) {
		beginEditAction();
		// Get the min and max ranges of selected cells 
		int iRowStart = m_table.getSelectedRow(); 
		int iRowEnd = m_table.getSelectionModel().getMaxSelectionIndex(); 
		int iColStart = m_table.getSelectedColumn(); 
		int iColEnd = m_table.getColumnModel().getSelectionModel().getMaxSelectionIndex(); 
		// Check each cell in the range 
		for (int iRow = iRowStart; iRow <= iRowEnd; iRow++) { 
			for (int iCol = iColStart; iCol <= iColEnd; iCol++) { 
				if (m_table.isCellSelected(iRow, iCol)) {
					editFormat(iRow, iCol);
			       	if (m_cellFormat[iRow][iCol] == null) {
			       		m_cellFormat[iRow][iCol] = new CellFormat();
			       	}
			   		if (m_cellFormat[iRow][iCol].m_font == null) {
			   	       	Font font = m_table.getFont(); 
			   			m_cellFormat[iRow][iCol].m_font = new Font(font.getName(), font.getStyle(), font.getSize());
			   		}
			       	Font font = m_cellFormat[iRow][iCol].m_font;
			       	String sFont = (sFontName == null ? font.getFamily() : sFontName);
			       	int nStyle = (nFontStyle > 0 ? (font.getStyle() ^ nFontStyle) : font.getStyle());
			       	int nSize = (nFontSize > 0 ? Math.max(2, font.getSize() + 2 - nFontSize): font.getSize());
			   		m_cellFormat[iRow][iCol].m_font = new Font(sFont, nStyle, nSize);
				}
			}
		}
		endEditAction();
	    m_table.repaint();
	} // toggleFontProperty

	void readXML(String sFile) throws Exception {
		XMLParser parser = new XMLParser();
		Random rand = new Random(127);
		

		Plugin plugin0 = null;
		plugin0 = parser.parseFile(sFile);
		// collect all objects and store in m_plugins
		m_plugins = new ArrayList<Plugin>();
		collectPlugins(plugin0);
		m_plugins.add(plugin0);

		Color color = new Color(rand.nextInt(256), 128+rand.nextInt(128), rand.nextInt(128));
		int k = 0;
		int i = 0;
		while (k < m_plugins.size()) {
			Plugin plugin = m_plugins.get(k++);
			if (i > 1) {
				Object prev = m_objects[i-1][1];
				if (prev instanceof Sequence && !(plugin instanceof Sequence) ||
				    !(prev instanceof Operator) && plugin instanceof Operator ||
					plugin instanceof Alignment ||
					plugin instanceof Runnable ||
					plugin instanceof Logger) {
					color = new Color(128+rand.nextInt(128), 200+rand.nextInt(56), rand.nextInt(128));
					i++;
				}
			}
			String sID = plugin.getID();
			if (sID == null || sID.equals("")) {
				sID = plugin.getClass().getName().substring(plugin.getClass().getName().lastIndexOf('.') + 1);
			}
			m_objects[i][0] = sID;
			m_objects[i][1] = plugin;
			CellFormat format = new CellFormat();
			format.m_bgColor = color; 
			m_cellFormat[i][1] = format;
			
			plugin.setID("B"+(i+1));
			m_pluginLocation.put(plugin, i + MAX_ROW);
			if (plugin instanceof StateNode || plugin instanceof Distribution) {
				FormulaCell formula = new FormulaCell("=$B"+(i+1)); 
				m_objects[i][2] = formula;
				m_formulas.add(formula);
				m_formulaLocation.put(formula, i + MAX_ROW * 2);
			}
			i++;
		}
		m_table.getColumnModel().getColumn(0).setPreferredWidth(100);
		m_table.getColumnModel().getColumn(1).setPreferredWidth(400);
		updateActions();
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
			endEditAction();
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			beginEditAction();
			int iRow = m_rowHeaderTable.rowAtPoint(e.getPoint());
			editHeight(iRow);
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
				try {
					return ((Distribution)o).calculateLogP();
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (o instanceof String) {
				return Double.parseDouble((String) o);
			}
			return Double.NaN;
		}
			
		Object value() {
			String sFormula = "with (Math) {";
			for (int i = 0; i < m_nTokens.size(); i++) {
				if (m_nTokens.get(i) >= 0) {
					sFormula += "(" + getCellRef(m_nTokens.get(i)) + ")";
				} else {
					sFormula += m_sTokens.get(i);
				}
			}
			sFormula += "}";
			System.err.println("parsing " + sFormula);
			try {
				Object o = m_engine.eval(sFormula);
				m_sValue = o.toString();
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
	    	Pattern pattern = Pattern.compile("([^\\$]*)\\$([a-zA-Z0-9]*)(.*)");
	    	Pattern cellPattern = Pattern.compile("([a-zA-Z]+)([0-9]+)");
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
						String sCol = cellMatcher.group(1).toUpperCase();
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

	class MyTableCellRenderer extends DefaultTableCellRenderer {

		public Component getTableCellRendererComponent (JTable table, Object value, boolean bSelected, boolean bFocused, int iRow, int iCol) {

			setEnabled(table == null || table.isEnabled()); // see question above

	        if (m_cellFormat[iRow][iCol] != null) {
	            setForeground(m_cellFormat[iRow][iCol].m_fgColor);
	            setHorizontalAlignment(m_cellFormat[iRow][iCol].m_alignment);
			} else {
	            setForeground(null);
	            setHorizontalAlignment(SwingConstants.LEFT);
			}

	        if (value instanceof Plugin) {
	        	setText(SpreadSheet.toString((Plugin) value));
	        } else {
	        	super.getTableCellRendererComponent(table, value, bSelected, bFocused, iRow, iCol);
	        }

	        if (m_cellFormat[iRow][iCol] != null) {
	        	if (bSelected) {
	        		if (m_cellFormat[iRow][iCol].m_bgColor != null) {
	        		Color c1 = m_cellFormat[iRow][iCol].m_bgColor;
	        		Color c2 = Color.LIGHT_GRAY;
	        			setBackground(new Color((c1.getRed() + c2.getRed())/2, (c1.getGreen() + c2.getGreen())/2, (c1.getBlue() + c2.getBlue())/2));
	        		} else {
		        		setBackground(Color.LIGHT_GRAY);
	        		}
	        	} else {
	        		setBackground(m_cellFormat[iRow][iCol].m_bgColor);
	        	}
	            setFont(m_cellFormat[iRow][iCol].m_font);
	            setBorder(m_cellFormat[iRow][iCol].m_border);
			} else {
	        	if (bSelected) {
	        		setBackground(Color.LIGHT_GRAY);
	        	} else {
	        		setBackground(null);
	        	}
	            setFont(null);
	            setBorder(null);
			}
	        return this;
	    }
	} // class ColoredTableCellRenderer
	

	public class CellBorder extends AbstractBorder implements SwingConstants { 
		protected int m_nNorthThickness;
		protected int m_nSouthThickness;
		protected int m_nEastThickness;
		protected int m_nWestThickness;  
		protected Color m_color;
	  
	    public CellBorder(int nNorth, int nEast, int nSouth, int nWest) {
		    m_nNorthThickness = nNorth;
		    m_nEastThickness = nEast;
		    m_nSouthThickness = nSouth;
		    m_nWestThickness = nWest;
		    m_color = Color.black;
	    }

	    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
	    	Color oldColor = g.getColor();
	    
	    	g.setColor(m_color);
	    	for (int i = 0; i < m_nNorthThickness; i++)  {
	    		g.drawLine(x, y+i, x+width-1, y+i);
	    	}
	    	for (int i = 0; i < m_nSouthThickness; i++)  {
	    		g.drawLine(x, y+height-i-1, x+width-1, y+height-i-1);
	    	}
	    	for (int i = 0; i < m_nWestThickness; i++)  {
	    		g.drawLine(x+i, y, x+i, y+height-1);
	    	}
	    	for (int i = 0; i < m_nEastThickness; i++)  {
	    		g.drawLine(x+width-i-1, y, x+width-i-1, y+height-1);
	    	}
	 
	    	g.setColor(oldColor);
	    }
	} // class CellBorder

	void selectCell(int iRow, int iCol) {
		m_table.clearSelection();
		m_table.addRowSelectionInterval(iRow, iRow);
		m_table.addColumnSelectionInterval(iCol, iCol);
	}

	
	
	protected JMenuBar getMenuBar() {
		JMenuBar m_menuBar = new JMenuBar();
		JMenu fileMenu = new JMenu("File");
		fileMenu.setMnemonic('F');
		// ----------------------------------------------------------------------
		// File menu */
		m_menuBar.add(fileMenu);
		fileMenu.add(a_open);
		fileMenu.add(a_save);
		fileMenu.add(a_saveas);
//		fileMenu.add(a_loadimage);
		fileMenu.addSeparator();
//		fileMenu.add(a_import);
		fileMenu.add(a_export);
//		fileMenu.add(a_print);
		fileMenu.addSeparator();
		fileMenu.add(a_exit);

		// ----------------------------------------------------------------------
		// Edit menu */
		JMenu editMenu = new JMenu("Edit");
		editMenu.setMnemonic('E');
		m_menuBar.add(editMenu);
		editMenu.add(a_undo);
		editMenu.add(a_redo);
		fileMenu.addSeparator();
//		editMenu.add(a_selectAll);
//		editMenu.add(a_unselectAll);
		editMenu.add(a_cut);
		editMenu.add(a_copy);
		editMenu.add(a_del);
		editMenu.add(a_paste);
		

		// ----------------------------------------------------------------------
		// Window menu */
		JMenu windowMenu = new JMenu("Window");
		windowMenu.setMnemonic('W');
		m_menuBar.add(windowMenu);
		windowMenu.add(a_viewtoolbar);
//		windowMenu.add(a_viewtoolbar);

		// ----------------------------------------------------------------------
		// Help menu */
		JMenu helpMenu = new JMenu("Help");
		helpMenu.setMnemonic('H');
		m_menuBar.add(helpMenu);
//		helpMenu.add(a_help);
		helpMenu.add(a_about);
		return m_menuBar;
	} // makeMenuBar
	
	
	
	/**
	 * Rudimentary test of this panel, takes a Beast II xml file as argument and
	 * opens it
	 **/
	public static void main(String args[]) {
		try { 

			Logger.FILE_MODE = Logger.FILE_OVERWRITE;
			SpreadSheet spreadSheet = new SpreadSheet();
			spreadSheet.setSize(2048, 2048);
			if (args.length > 0) {
				spreadSheet.readXML(args[0]);
			}

			JFrame frame = new JFrame("Beast II Calculator");
			JMenuBar menuBar = spreadSheet.getMenuBar();
			frame.setJMenuBar(menuBar);
			frame.getContentPane().add(spreadSheet, BorderLayout.CENTER);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setSize(800, 600);
			frame.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	} // main

	@Override
	public void lostOwnership(Clipboard clipboard, Transferable contents) {
		// do nothing
	}

} // class SpreadSheet
