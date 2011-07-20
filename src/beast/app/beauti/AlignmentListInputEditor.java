package beast.app.beauti;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import javax.swing.Box;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import beast.app.draw.ExtensionFileFilter;
import beast.app.draw.ListInputEditor;
import beast.app.draw.PluginPanel;
import beast.app.draw.SmallButton;
import beast.core.Input;
import beast.core.Plugin;
import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.FilteredAlignment;
import beast.evolution.branchratemodel.BranchRateModel;
import beast.evolution.likelihood.TreeLikelihood;
import beast.evolution.sitemodel.SiteModel;
import beast.evolution.tree.Tree;
import beast.util.NexusParser;
import beast.util.XMLParser;

public class AlignmentListInputEditor extends ListInputEditor {
	private static final long serialVersionUID = 1L;

	final static int NAME_COLUMN = 0;
	final static int FILE_COLUMN = 1;
	final static int TAXA_COLUMN = 2;
	final static int SITES_COLUMN = 3;
	final static int TYPE_COLUMN = 4;
	final static int SITEMODEL_COLUMN = 5;
	final static int CLOCKMODEL_COLUMN = 6;
	final static int TREE_COLUMN = 7;
	
	/** alignments that form a partition. These can be FilteredAlignments **/
	List<Alignment> m_alignments;
	int m_nPartitions;
	TreeLikelihood[] m_likelihoods;
	Object[][] m_tableData;
	JTable m_table;

	String[] m_sPartitionNames;

	@Override
	public Class<?> type() {
		return List.class;
	}

	@Override
	public Class<?> baseType() {
		return Alignment.class;
	}

	@Override
	public void init(Input<?> input, Plugin plugin, EXPAND bExpand, boolean bAddButtons) {
		m_alignments = (List<Alignment>) input.get();
		m_nPartitions = m_alignments.size();
		// super.init(input, plugin, bExpand, false);
		Box box = createVerticalBox();
		box.add(Box.createVerticalStrut(5));
		box.add(createButtonBox());
		box.add(Box.createVerticalStrut(5));
		box.add(createListBox());
		box.add(Box.createVerticalGlue());

		Box buttonBox = box.createHorizontalBox();

		m_addButton = new SmallButton("+", true);
		m_addButton.setToolTipText("Add item to the list");
		m_addButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				addItem();
			}
		});
		buttonBox.add(m_addButton);
		buttonBox.add(Box.createHorizontalGlue());
		box.add(buttonBox);
		add(box);

	}

	private Component createButtonBox() {
		Box box = Box.createHorizontalBox();
		box.add(Box.createHorizontalGlue());
		addLinkUnlinkPair(box, "Site Models");
		addLinkUnlinkPair(box, "Clock Models");
		addLinkUnlinkPair(box, "Trees");
		box.add(Box.createHorizontalGlue());
		return box;
	}

	private void addLinkUnlinkPair(Box box, String sLabel) {
		JButton linkSModelButton = new JButton("Link " + sLabel);
		linkSModelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JButton button = (JButton) e.getSource();
				link(columnLabelToNr(button.getText()));
				m_table.repaint();
			}

		});
		box.add(linkSModelButton);
		JButton unlinkSModelButton = new JButton("Unlink " + sLabel);
		unlinkSModelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JButton button = (JButton) e.getSource();
				unlink(columnLabelToNr(button.getText()));
				m_table.repaint();
			}

		});
		box.add(unlinkSModelButton);
		box.add(Box.createHorizontalGlue());
	}
	private int columnLabelToNr(String sColumn) {
		int nColumn;
		if (sColumn.contains("Tree")) {
			nColumn = TREE_COLUMN;
		} else if (sColumn.contains("Clock")) {
			nColumn = CLOCKMODEL_COLUMN;
		} else {
			nColumn = SITEMODEL_COLUMN;
		}
		return nColumn;
	}

	
	private void link(int nColumn) {
		int [] nSelected = getTableRowSelection();
		for (int i = 1; i < nSelected.length; i++) {
			int iRow = nSelected[i];
			m_tableData[iRow][nColumn] = m_tableData[nSelected[0]][nColumn];
			try {
				updateModel(nColumn, i);
			} catch (Exception ex) {
				System.err.println(ex.getMessage());
			}
		}
	}
	
	private void unlink(int nColumn) {
		int [] nSelected = getTableRowSelection();
		for (int i = 1; i < nSelected.length; i++) {
			int iRow = nSelected[i];
			m_tableData[iRow][nColumn] = m_sPartitionNames[iRow];
			try {
				updateModel(nColumn, iRow);
			} catch (Exception ex) {
				System.err.println(ex.getMessage());
			}
		}
	}

	int [] getTableRowSelection() {
		int [] nSelected = m_table.getSelectedRows();
		if (nSelected.length == 0) {
			// select all
			nSelected = new int[m_nPartitions];
			for (int i = 0; i < m_nPartitions; i++) {
				nSelected[i] = i;
			}
		}
		return nSelected;
	}
	
	void updateModel(int nColumn, int iRow) throws Exception {
		switch (nColumn) {
			case SITEMODEL_COLUMN: 
			{
				String sPartition = (String) m_tableData[iRow][SITEMODEL_COLUMN];
				SiteModel siteModel = (SiteModel) PluginPanel.g_plugins.get("SiteModel." + sPartition);
				m_likelihoods[iRow].m_pSiteModel.setValue(siteModel, m_likelihoods[iRow]);
			}
				break;
			case CLOCKMODEL_COLUMN:
			{
				String sPartition = (String) m_tableData[iRow][CLOCKMODEL_COLUMN];
				BranchRateModel clockModel = (BranchRateModel) PluginPanel.g_plugins.get("ClockModel." + sPartition);
				m_likelihoods[iRow].m_pBranchRateModel.setValue(clockModel, m_likelihoods[iRow]);
			}
				break;
			case TREE_COLUMN:
			{
				String sPartition = (String) m_tableData[iRow][TREE_COLUMN];
				Tree tree = (Tree) PluginPanel.g_plugins.get("Tree." + sPartition);
				m_likelihoods[iRow].m_tree.setValue(tree, m_likelihoods[iRow]);
			}
		}
	}
	
	@Override
	protected void addInputLabel() {
	}

	void initTableData() {
		m_likelihoods = new TreeLikelihood[m_nPartitions];
		if (m_tableData == null) {
			m_tableData = new Object[m_nPartitions][8];
		}
		for (int i = 0; i < m_nPartitions; i++) {
			Alignment data = m_alignments.get(i);
			// partition name
			m_tableData[i][NAME_COLUMN] = data;

			// alignment name
			if (data instanceof FilteredAlignment) {
				m_tableData[i][FILE_COLUMN] = ((FilteredAlignment) data).m_alignmentInput.get();
			} else {
				m_tableData[i][FILE_COLUMN] = data;
			}
			// # taxa
			m_tableData[i][TAXA_COLUMN] = data.getNrTaxa();
			// # sites
			m_tableData[i][SITES_COLUMN] = data.getSiteCount();
			// Data type
			m_tableData[i][TYPE_COLUMN] = data.getDataType();
			// site model
			TreeLikelihood likelihood = null;
			for (Plugin plugin : data.outputs) {
				if (plugin instanceof TreeLikelihood) {
					likelihood = (TreeLikelihood) plugin;
					break;
				}
			}
			assert (likelihood != null);
			m_likelihoods[i] = likelihood;
			m_tableData[i][SITEMODEL_COLUMN] = getPartition(likelihood.m_pSiteModel);
			// clock model
			m_tableData[i][CLOCKMODEL_COLUMN] = getPartition(likelihood.m_pBranchRateModel);
			// tree
			m_tableData[i][TREE_COLUMN] = getPartition(likelihood.m_tree);
		}
	}

	private String getPartition(Input input) {
		Plugin plugin = (Plugin) input.get();
		String sID = plugin.getID();
		sID = sID.substring(sID.indexOf('.') + 1);
		return sID;
	}

	private Component createListBox() {
		String[] columnData = new String[] { "Name", "File", "Taxa", "Sites", "Data Type", "Site Model", "Clock Model",
				"Tree" };
		initTableData();
		// set up table.
		// special features: background shading of rows
		// custom editor allowing only Date column to be edited.
		m_table = new JTable(m_tableData, columnData) {
			private static final long serialVersionUID = 1L;

			// method that induces table row shading
			@Override
			public Component prepareRenderer(TableCellRenderer renderer, int Index_row, int Index_col) {
				Component comp = super.prepareRenderer(renderer, Index_row, Index_col);
				// even index, selected or not selected
				if (isCellSelected(Index_row, Index_col)) {
					comp.setBackground(Color.gray);
				} else if (Index_row % 2 == 0 && !isCellSelected(Index_row, Index_col)) {
					comp.setBackground(new Color(237, 243, 255));
				} else {
					comp.setBackground(Color.white);
				}
				return comp;
			}
		};
		m_table.setRowHeight(25);
		m_table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		m_table.setColumnSelectionAllowed(false);
		m_table.setRowSelectionAllowed(true);

		
		// set up comboboxes
		m_sPartitionNames = new String[m_nPartitions];
		for (int i = 0; i < m_nPartitions; i++) {
			m_sPartitionNames[i] = m_alignments.get(i).getID();
		}
		TableColumn col = m_table.getColumnModel().getColumn(SITEMODEL_COLUMN);
		JComboBox siteModelComboBox = new JComboBox(m_sPartitionNames);
		siteModelComboBox.addActionListener(new ComboActionListener(SITEMODEL_COLUMN));

		col.setCellEditor(new DefaultCellEditor(siteModelComboBox));
		// If the cell should appear like a combobox in its
		// non-editing state, also set the combobox renderer
		col.setCellRenderer(new MyComboBoxRenderer(m_sPartitionNames));
		col = m_table.getColumnModel().getColumn(CLOCKMODEL_COLUMN);
		
		JComboBox clockModelComboBox = new JComboBox(m_sPartitionNames);
		clockModelComboBox.addActionListener(new ComboActionListener(CLOCKMODEL_COLUMN));
		
		
		col.setCellEditor(new DefaultCellEditor(clockModelComboBox));
		col.setCellRenderer(new MyComboBoxRenderer(m_sPartitionNames));
		col = m_table.getColumnModel().getColumn(TREE_COLUMN);

		JComboBox treeComboBox = new JComboBox(m_sPartitionNames);
		clockModelComboBox.addActionListener(new ComboActionListener(TREE_COLUMN));
		col.setCellEditor(new DefaultCellEditor(treeComboBox));
		col.setCellRenderer(new MyComboBoxRenderer(m_sPartitionNames));
		col = m_table.getColumnModel().getColumn(TAXA_COLUMN);
		col.setPreferredWidth(30);
		col = m_table.getColumnModel().getColumn(SITES_COLUMN);
		col.setPreferredWidth(30);

		// // set up editor that makes sure only doubles are accepted as entry
		// // and only the Date column is editable.
		m_table.setDefaultEditor(Object.class, new TableCellEditor() {
			JTextField m_textField = new JTextField();
			int m_iRow, m_iCol;

			@Override
			public boolean stopCellEditing() {
				m_table.removeEditor();
				String sText = m_textField.getText();
				try {
					Double.parseDouble(sText);
				} catch (Exception e) {
					return false;
				}
				m_tableData[m_iRow][m_iCol] = sText;
				return true;
			}

			@Override
			public boolean isCellEditable(EventObject anEvent) {
				return m_table.getSelectedColumn() == 1;
			}

			@Override
			public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int iRow,
					int iCol) {
//				if (!isSelected) {
//					return null;
//				}
//				m_iRow = iRow;
//				m_iCol = iCol;
//				m_textField.setText((String) value);
//				return m_textField;
				return null;
			}

			@Override
			public boolean shouldSelectCell(EventObject anEvent) {
				return false;
			}

			@Override
			public void removeCellEditorListener(CellEditorListener l) {
			}

			@Override
			public Object getCellEditorValue() {
				return null;
			}

			@Override
			public void cancelCellEditing() {
			}

			@Override
			public void addCellEditorListener(CellEditorListener l) {
			}

		});
		JScrollPane scrollPane = new JScrollPane(m_table);
		return scrollPane;
	} // createListBox


	class ComboActionListener implements ActionListener{
		int m_nColumn;
		public ComboActionListener(int nColumn) {
			m_nColumn = nColumn;
		};
		@Override
		public void actionPerformed(ActionEvent e) {
			for (int i = 0; i < m_nPartitions; i++) {
				try {
					updateModel(m_nColumn, i);
//					switch (m_nColumn) {
//						case SITEMODEL_COLUMN: 
//						{
//							String sPartition = (String) m_tableData[i][SITEMODEL_COLUMN];
//							SiteModel siteModel = (SiteModel) PluginPanel.g_plugins.get("SiteModel." + sPartition);
//							m_likelihoods[i].m_pSiteModel.setValue(siteModel, m_likelihoods[i]);
//						}
//							break;
//						case CLOCKMODEL_COLUMN:
//						{
//							String sPartition = (String) m_tableData[i][CLOCKMODEL_COLUMN];
//							BranchRateModel clockModel = (BranchRateModel) PluginPanel.g_plugins.get("ClockModel." + sPartition);
//							m_likelihoods[i].m_pBranchRateModel.setValue(clockModel, m_likelihoods[i]);
//						}
//							break;
//						case TREE_COLUMN:
//						{
//							String sPartition = (String) m_tableData[i][TREE_COLUMN];
//							Tree tree = (Tree) PluginPanel.g_plugins.get("Tree." + sPartition);
//							m_likelihoods[i].m_tree.setValue(tree, m_likelihoods[i]);
//						}
//							break;
//					}
				} catch (Exception ex) {
					System.err.println(ex.getMessage());
				}
			}
		}
	}

	public class MyComboBoxRenderer extends JComboBox implements TableCellRenderer {
		public MyComboBoxRenderer(String[] items) {
			super(items);
		}

		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
				boolean hasFocus, int row, int column) {
			if (isSelected) {
				setForeground(table.getSelectionForeground());
				super.setBackground(table.getSelectionBackground());
			} else {
				setForeground(table.getForeground());
				setBackground(table.getBackground());
			}

			// Select the current value
			setSelectedItem(value);
			return this;
		}
	}

	@Override
	protected void addSingleItem(Plugin plugin) {
		initTableData();
		repaint();
	}

	@Override
	protected void addItem() {
		List<Plugin> plugins = pluginSelector(m_input, m_plugin, null);

		Component c = this;
		BeautiDoc doc = null;
		while (((Component) c).getParent() != null) {
			c = ((Component) c).getParent();
			if (c instanceof BeautiPanel) {
				doc = ((BeautiPanel) c).m_doc;
			}
		}

		if (plugins != null) {
			for (Plugin plugin : plugins) {
				doc.addAlignmentWithSubnet((Alignment) plugin);
			}
			refreshPanel();
		}
	} // addItem

	@Override
	public List<Plugin> pluginSelector(Input<?> input, Plugin plugin, List<String> sTabuList) {
		List<Plugin> selectedPlugins = new ArrayList<Plugin>();
		JFileChooser fileChooser = new JFileChooser(Beauti.m_sDir);

		fileChooser.addChoosableFileFilter(new ExtensionFileFilter(".xml", "Beast xml file (*.xml)"));
		String[] exts = { ".nex", ".nxs" };
		fileChooser.addChoosableFileFilter(new ExtensionFileFilter(exts, "Nexus file (*.nex)"));

		fileChooser.setDialogTitle("Load Sequence");
		fileChooser.setMultiSelectionEnabled(true);
		int rval = fileChooser.showOpenDialog(null);

		if (rval == JFileChooser.APPROVE_OPTION) {

			File[] files = fileChooser.getSelectedFiles();
			for (int i = 0; i < files.length; i++) {
				String sFileName = files[i].getAbsolutePath();
				if (sFileName.lastIndexOf('/') > 0) {
					Beauti.m_sDir = sFileName.substring(0, sFileName.lastIndexOf('/'));
				}
				if (sFileName.toLowerCase().endsWith(".nex") || sFileName.toLowerCase().endsWith(".nxs")) {
					NexusParser parser = new NexusParser();
					try {
						parser.parseFile(sFileName);
						if (parser.m_filteredAlignments.size() > 0) {
							for (Alignment data : parser.m_filteredAlignments) {
								selectedPlugins.add(data);
							}
						} else {
							selectedPlugins.add(parser.m_alignment);
						}
					} catch (Exception ex) {
						ex.printStackTrace();
						JOptionPane.showMessageDialog(null, "Loading of " + sFileName + " failed: " + ex.getMessage());
						return null;
					}
				}
				if (sFileName.toLowerCase().endsWith(".xml")) {
					Plugin alignment = getXMLData(sFileName);
					selectedPlugins.add(alignment);
				}
			}
			return selectedPlugins;
		}
		return null;
	} // pluginSelector

	static public Plugin getXMLData(String sFileName) {
		XMLParser parser = new XMLParser();
		try {
			String sXML = "";
			BufferedReader fin = new BufferedReader(new FileReader(sFileName));
			while (fin.ready()) {
				sXML += fin.readLine();
			}
			fin.close();
			Plugin runnable = parser.parseFragment(sXML, false);
			return getAlignment(runnable);
		} catch (Exception ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(null, "Loading of " + sFileName + " failed: " + ex.getMessage());
			return null;
		}
	}

	static Plugin getAlignment(Plugin plugin) throws IllegalArgumentException, IllegalAccessException {
		if (plugin instanceof Alignment) {
			return plugin;
		}
		for (Plugin plugin2 : plugin.listActivePlugins()) {
			plugin2 = getAlignment(plugin2);
			if (plugin2 != null) {
				return plugin2;
			}
		}
		return null;
	}

} // class AlignmentListInputEditor
