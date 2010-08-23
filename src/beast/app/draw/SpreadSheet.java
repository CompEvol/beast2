package beast.app.draw;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import beast.core.Plugin;
import beast.util.XMLParser;

/** panel that shows a beast model as a spreadsheet **/
public class SpreadSheet extends JPanel {
	private static final long serialVersionUID = 1L;
	
	List<Plugin>      m_objects;
	JTable            m_table;
	Object [][]       m_rows;
	
	int MAX_ROW = 255;
	int MAX_COL = 32;
	
	void init() {
		m_rows = new Object[MAX_ROW][MAX_COL];
		String [] headers = new String[MAX_COL];
		String abc="ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		for (int i = 0; i < MAX_COL; i++) {
			headers[i] = (i>=26?abc.charAt(i/26-1):' ')+"" + abc.charAt(i%26)+"";
		}
		for (int i = 0; i < m_objects.size(); i++) {
			Plugin plugin = m_objects.get(i); 
			m_rows[i][0] = plugin.getID();
			if (m_rows[i][0] == null || m_rows[i][0].equals("")) {
				m_rows[i][0] = plugin.getClass().getName().substring(plugin.getClass().getName().lastIndexOf('.') + 1);
			}
			m_rows[i][1] = plugin;//.toString();
		}
		
		m_table = new JTable(m_rows, headers);
		m_table.addMouseListener(new MouseListener() {
			@Override public void mouseReleased(MouseEvent e) {}
			@Override public void mousePressed(MouseEvent e) {}
			@Override public void mouseExited(MouseEvent e) {}
			@Override public void mouseEntered(MouseEvent e) {}
			
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
			@Override public void keyReleased(KeyEvent e) {}
			@Override public void keyPressed(KeyEvent e) {}
		});
		JScrollPane scrollPane = new JScrollPane(m_table);

		TableColumnModel columnModel = m_table.getColumnModel();
		TableColumn column0 = columnModel.getColumn(0);
		TableColumn column1 = columnModel.getColumn(1);
		column0.setPreferredWidth(3000);
		column1.setPreferredWidth(10000);
		
		setRowHeader(m_table);
		
		
        this.setLayout(new BorderLayout());
        this.add(scrollPane, BorderLayout.CENTER);
	} // init


	void edit() {
		int iCol = m_table.getSelectedColumn();
		int iRow = m_table.getSelectedRow();
		Object o = m_rows[iRow][iCol];
		if (o instanceof Plugin) {
			Plugin plugin = (Plugin) o;
			PluginDialog dlg = new PluginDialog(plugin, plugin.getClass());
            dlg.setVisible(true);
            if (dlg.getOK()) {
            	plugin = dlg.panel.m_plugin;
            	m_rows[iRow][iCol] = plugin;
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
        m_rows[iSelectedRow][iSelectedColumn] = o;
        m_table.repaint();
	}
	
	/**
	 * * Creates row header for table with row number (starting with 1)
	 * displayed
	 */
	public void setRowHeader(JTable table) {
		Container p = table.getParent();
		if (p instanceof JViewport) {
			Container gp = p.getParent();
			if (gp instanceof JScrollPane) {
				JScrollPane scrollPane = (JScrollPane) gp;
				scrollPane.setRowHeaderView(new TableRowHeader(table));
			}
		}
	}	
	
	void readXML(String sFile) throws Exception {
		XMLParser parser = new XMLParser();
		Plugin plugin = null;
		plugin = parser.parseFile(sFile);
		// collect all objects and store in m_objects
		m_objects = new ArrayList<Plugin>();
		m_objects.add(plugin);
		collectPlugins(plugin);
	} // readXML

	void collectPlugins(Plugin plugin) throws Exception {
		for (Plugin plugin2 : plugin.listActivePlugins()) {
			if (!m_objects.contains(plugin2)) {
				m_objects.add(plugin2);
				collectPlugins(plugin2);
			}
		}
	} // collectPlugins
	
	public static void main(String args[]) {
		try {
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
