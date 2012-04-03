package beast.app.beauti;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import beast.app.draw.ExtensionFileFilter;
import beast.app.draw.ListInputEditor;
import beast.app.draw.SmallButton;
import beast.core.Input;
import beast.core.Plugin;
import beast.core.util.CompoundDistribution;
import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.FilteredAlignment;
import beast.evolution.alignment.Sequence;
import beast.evolution.branchratemodel.BranchRateModel;
import beast.evolution.datatype.DataType;
import beast.evolution.likelihood.TreeLikelihood;
import beast.evolution.sitemodel.SiteModel;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;
import beast.evolution.tree.TreeDistribution;
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

    /**
     * alignments that form a partition. These can be FilteredAlignments *
     */
    List<Alignment> alignments;
    int nPartitions;
    TreeLikelihood[] likelihoods;
    Object[][] tableData;
    JTable table;

	//public AlignmentListInputEditor() {}
	public AlignmentListInputEditor(BeautiDoc doc) {
		super(doc);
	}

    @Override
    public Class<?> type() {
        return List.class;
    }

    @Override
    public Class<?> baseType() {
        return Alignment.class;
    }

    @Override
    public Class<?>[] types() {
        Class<?>[] types = new Class[2];
        types[0] = List.class;
        types[1] = Alignment.class;
        return types;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void init(Input<?> input, Plugin plugin, ExpandOption bExpandOption, boolean bAddButtons) {
        if (input.get() instanceof List) {
            alignments = (List<Alignment>) input.get();
        } else {
            // we just have a single Alignment
            alignments = new ArrayList<Alignment>();
            alignments.add((Alignment) input.get());
        }
        nPartitions = alignments.size();
        // super.init(input, plugin, bExpandOption, false);
        Box box = Box.createVerticalBox();
        box.add(Box.createVerticalStrut(5));
        box.add(createButtonBox());
        box.add(Box.createVerticalStrut(5));
        box.add(createListBox());
        box.add(Box.createVerticalGlue());

        Box buttonBox = Box.createHorizontalBox();

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

    protected Component createButtonBox() {
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
                table.repaint();
            }

        });
        box.add(linkSModelButton);
        JButton unlinkSModelButton = new JButton("Unlink " + sLabel);
        unlinkSModelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JButton button = (JButton) e.getSource();
                unlink(columnLabelToNr(button.getText()));
                table.repaint();
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
        int[] nSelected = getTableRowSelection();
        // do the actual linking
        for (int i = 1; i < nSelected.length; i++) {
            int iRow = nSelected[i];
            Object old = tableData[iRow][nColumn]; 
            tableData[iRow][nColumn] = tableData[nSelected[0]][nColumn];
            try {
                updateModel(nColumn, iRow);
            } catch (Exception ex) {
                System.err.println(ex.getMessage());
                // unlink if we could not link
                tableData[iRow][nColumn] = old;
                try {
                	updateModel(nColumn, iRow);
                } catch (Exception ex2) {
                	// ignore
                }
            }
        }
    }

    private void unlink(int nColumn) {
        int[] nSelected = getTableRowSelection();
        for (int i = 1; i < nSelected.length; i++) {
            int iRow = nSelected[i];
            tableData[iRow][nColumn] = getDoc().sPartitionNames.get(iRow);
            try {
                updateModel(nColumn, iRow);
            } catch (Exception ex) {
                System.err.println(ex.getMessage());
            }
        }
    }

    int[] getTableRowSelection() {
        int[] nSelected = table.getSelectedRows();
        if (nSelected.length == 0) {
            // select all
            nSelected = new int[nPartitions];
            for (int i = 0; i < nPartitions; i++) {
                nSelected[i] = i;
            }
        }
        return nSelected;
    }

    void updateModel(int nColumn, int iRow) throws Exception {
        getDoc();
        String sPartition = (String) tableData[iRow][nColumn];
        TreeLikelihood treeLikelihood = (TreeLikelihood) doc.pluginmap.get("treeLikelihood." + sPartition);
        switch (nColumn) {
            case SITEMODEL_COLUMN: {
                if (getDoc().getPartitionNr(sPartition) != iRow) {
                    this.likelihoods[iRow].m_pSiteModel.setValue(treeLikelihood.m_pSiteModel.get(), this.likelihoods[iRow]);
                } else {
                    SiteModel siteModel = (SiteModel) doc.pluginmap.get("SiteModel." + sPartition);
                    this.likelihoods[iRow].m_pSiteModel.setValue(siteModel, this.likelihoods[iRow]);
                }
                sPartition = treeLikelihood.m_pSiteModel.get().getID();
                sPartition = sPartition.substring(sPartition.indexOf('.') + 1);
                getDoc().setCurrentPartition(0, iRow, sPartition);
            }
            break;
            case CLOCKMODEL_COLUMN: {
                // String sPartition = (String)
                // m_tableData[iRow][CLOCKMODEL_COLUMN];
                // BranchRateModel clockModel = m_doc.getClockModel(sPartition);
                // (BranchRateModel) PluginPanel.g_plugins.get("ClockModel." +
                // sPartition);
                if (getDoc().getPartitionNr(sPartition) != iRow) {
                    this.likelihoods[iRow].m_pBranchRateModel.setValue(treeLikelihood.m_pBranchRateModel.get(),
                            this.likelihoods[iRow]);
                } else {
                    BranchRateModel clockModel = getDoc().getClockModel(sPartition);
                    this.likelihoods[iRow].m_pBranchRateModel.setValue(clockModel, this.likelihoods[iRow]);
                }
                sPartition = treeLikelihood.m_pBranchRateModel.get().getID();
                sPartition = sPartition.substring(sPartition.indexOf('.') + 1);
                getDoc().setCurrentPartition(1, iRow, sPartition);
            }
            break;
            case TREE_COLUMN: {
            	Tree tree = null;
                if (getDoc().getPartitionNr(sPartition) != iRow) {
                	tree = treeLikelihood.m_tree.get();
                } else {
                    tree = (Tree) doc.pluginmap.get("Tree." + sPartition);
                }
                // sanity check: make sure taxon sets are compatible
                String [] taxa = tree.getTaxaNames();
                List<String> taxa2 = this.likelihoods[iRow].m_data.get().getTaxaNames();
                if (taxa.length != taxa2.size()) {
                	throw new Exception("Cannot link trees: incompatible taxon sets");
                }
                for (String taxon : taxa) {
                	boolean found = false;
                	for (String taxon2: taxa2) {
                		if (taxon.equals(taxon2)) {
                			found = true;
                			break;
                		}
                	}
                	if (!found) {
                		throw new Exception("Cannot link trees: taxon" + taxon + "is not in alignment");
                	}
                }
                
                
                this.likelihoods[iRow].m_tree.setValue(tree, this.likelihoods[iRow]);
                TreeDistribution d = getDoc().getTreePrior(sPartition);
                CompoundDistribution prior = (CompoundDistribution) doc.pluginmap.get("prior");
                if (!getDoc().posteriorPredecessors.contains(d)) {
                    prior.pDistributions.setValue(d, prior);
                }
                sPartition = treeLikelihood.m_tree.get().getID();
                sPartition = sPartition.substring(sPartition.indexOf('.') + 1);
                getDoc().setCurrentPartition(2, iRow, sPartition);
            }
        }
        tableData[iRow][nColumn] = sPartition;
    }

    @Override
    protected void addInputLabel() {
    }

    void initTableData() {
        this.likelihoods = new TreeLikelihood[nPartitions];
        if (tableData == null) {
            tableData = new Object[nPartitions][8];
        }
        CompoundDistribution likelihoods = (CompoundDistribution) doc.pluginmap.get("likelihood");

        for (int i = 0; i < nPartitions; i++) {
            Alignment data = alignments.get(i);
            // partition name
            tableData[i][NAME_COLUMN] = data;

            // alignment name
            if (data instanceof FilteredAlignment) {
                tableData[i][FILE_COLUMN] = ((FilteredAlignment) data).m_alignmentInput.get();
            } else {
                tableData[i][FILE_COLUMN] = data;
            }
            // # taxa
            tableData[i][TAXA_COLUMN] = data.getNrTaxa();
            // # sites
            tableData[i][SITES_COLUMN] = data.getSiteCount();
            // Data type
            tableData[i][TYPE_COLUMN] = data.getDataType();
            // site model
            TreeLikelihood likelihood = (TreeLikelihood) likelihoods.pDistributions.get().get(i);
            assert (likelihood != null);
            this.likelihoods[i] = likelihood;
            tableData[i][SITEMODEL_COLUMN] = getPartition(likelihood.m_pSiteModel);
            // clock model
            tableData[i][CLOCKMODEL_COLUMN] = getPartition(likelihood.m_pBranchRateModel);
            // tree
            tableData[i][TREE_COLUMN] = getPartition(likelihood.m_tree);
        }
    }

    private String getPartition(Input<?> input) {
        Plugin plugin = (Plugin) input.get();
        String sID = plugin.getID();
        sID = sID.substring(sID.indexOf('.') + 1);
        return sID;
    }

    protected Component createListBox() {
        String[] columnData = new String[]{"Name", "File", "Taxa", "Sites", "Data Type", "Site Model", "Clock Model",
                "Tree"};
        initTableData();
        // set up table.
        // special features: background shading of rows
        // custom editor allowing only Date column to be edited.
        table = new JTable(tableData, columnData) {
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
        table.setRowHeight(25);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setColumnSelectionAllowed(false);
        table.setRowSelectionAllowed(true);

        // set up comboboxes
        String[] sPartitionNames = new String[nPartitions];
        for (int i = 0; i < nPartitions; i++) {
            sPartitionNames[i] = alignments.get(i).getID();
        }
        TableColumn col = table.getColumnModel().getColumn(SITEMODEL_COLUMN);
        JComboBox siteModelComboBox = new JComboBox(sPartitionNames);
        siteModelComboBox.addActionListener(new ComboActionListener(SITEMODEL_COLUMN));

        col.setCellEditor(new DefaultCellEditor(siteModelComboBox));
        // If the cell should appear like a combobox in its
        // non-editing state, also set the combobox renderer
        col.setCellRenderer(new MyComboBoxRenderer(sPartitionNames));
        col = table.getColumnModel().getColumn(CLOCKMODEL_COLUMN);

        JComboBox clockModelComboBox = new JComboBox(sPartitionNames);
        clockModelComboBox.addActionListener(new ComboActionListener(CLOCKMODEL_COLUMN));

        col.setCellEditor(new DefaultCellEditor(clockModelComboBox));
        col.setCellRenderer(new MyComboBoxRenderer(sPartitionNames));
        col = table.getColumnModel().getColumn(TREE_COLUMN);

        JComboBox treeComboBox = new JComboBox(sPartitionNames);
        treeComboBox.addActionListener(new ComboActionListener(TREE_COLUMN));
        col.setCellEditor(new DefaultCellEditor(treeComboBox));
        col.setCellRenderer(new MyComboBoxRenderer(sPartitionNames));
        col = table.getColumnModel().getColumn(TAXA_COLUMN);
        col.setPreferredWidth(30);
        col = table.getColumnModel().getColumn(SITES_COLUMN);
        col.setPreferredWidth(30);

        // // set up editor that makes sure only doubles are accepted as entry
        // // and only the Date column is editable.
        table.setDefaultEditor(Object.class, new TableCellEditor() {
            JTextField m_textField = new JTextField();
            int m_iRow
                    ,
                    m_iCol;

            @Override
            public boolean stopCellEditing() {
                table.removeEditor();
                String sText = m_textField.getText();
                try {
                    Double.parseDouble(sText);
                } catch (Exception e) {
                    return false;
                }
                tableData[m_iRow][m_iCol] = sText;
                return true;
            }

            @Override
            public boolean isCellEditable(EventObject anEvent) {
                return table.getSelectedColumn() == 1;
            }

            @Override
            public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int iRow,
                                                         int iCol) {
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

        // show alignment viewer when double clicking a row
        table.addMouseListener(new MouseListener() {

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
                if (e.getClickCount() > 1) {
                    try {
                        int iAlignmemt = table.rowAtPoint(e.getPoint());
                        AlignmentViewer viewer = new AlignmentViewer(alignments.get(iAlignmemt));
                        viewer.showInDialog();
                    } catch (Exception e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        return scrollPane;
    } // createListBox

    class ComboActionListener implements ActionListener {
        int m_nColumn;

        public ComboActionListener(int nColumn) {
            m_nColumn = nColumn;
        }

        ;

        @Override
        public void actionPerformed(ActionEvent e) {
            for (int i = 0; i < nPartitions; i++) {
                try {
                    updateModel(m_nColumn, i);
                } catch (Exception ex) {
                    System.err.println(ex.getMessage());
                }
            }
        }
    }

    public class MyComboBoxRenderer extends JComboBox implements TableCellRenderer {
        private static final long serialVersionUID = 1L;

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

        //Component c = this;
        if (plugins != null) {
            for (Plugin plugin : plugins) {
                getDoc().addAlignmentWithSubnet((Alignment) plugin);
            }
            refreshPanel();
        }
    } // addItem

    @Override
    public List<Plugin> pluginSelector(Input<?> input, Plugin plugin, List<String> sTabuList) {
        List<Plugin> selectedPlugins = new ArrayList<Plugin>();
        JFileChooser fileChooser = new JFileChooser(Beauti.g_sDir);

        fileChooser.addChoosableFileFilter(new ExtensionFileFilter(".xml", "Beast xml file (*.xml)"));
        String[] exts = {".nex", ".nxs", ".nexus"};
        fileChooser.addChoosableFileFilter(new ExtensionFileFilter(exts, "Nexus file (*.nex)"));

        fileChooser.setDialogTitle("Load Sequence");
        fileChooser.setMultiSelectionEnabled(true);
        int rval = fileChooser.showOpenDialog(null);

        if (rval == JFileChooser.APPROVE_OPTION) {

            File[] files = fileChooser.getSelectedFiles();
            for (File file : files) {
                String fileName = file.getName();
//				if (sFileName.lastIndexOf('/') > 0) {
//					Beauti.g_sDir = sFileName.substring(0, sFileName.lastIndexOf('/'));
//				}
                if (fileName.toLowerCase().endsWith(".nex") || fileName.toLowerCase().endsWith(".nxs") || fileName.toLowerCase().endsWith(".nexus")) {
                    NexusParser parser = new NexusParser();
                    try {
                        parser.parseFile(file);
                        if (parser.m_filteredAlignments.size() > 0) {
                            for (Alignment data : parser.m_filteredAlignments) {
                                selectedPlugins.add(data);
                            }
                        } else {
                            selectedPlugins.add(parser.m_alignment);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(null, "Loading of " + fileName + " failed: " + ex.getMessage());
                        return null;
                    }
                }
                if (file.getName().toLowerCase().endsWith(".xml")) {
                    Plugin alignment = getXMLData(file);
                    selectedPlugins.add(alignment);
                }
            }
            return selectedPlugins;
        }
        return null;
    } // pluginSelector

    static public Plugin getXMLData(File file) {
        String sXML = "";
        try {
        	// parse as BEAST 2 xml fragment
            XMLParser parser = new XMLParser();
            BufferedReader fin = new BufferedReader(new FileReader(file));
            while (fin.ready()) {
                sXML += fin.readLine();
            }
            fin.close();
            Plugin runnable = parser.parseFragment(sXML, false);
            return getAlignment(runnable);
        } catch (Exception ex) {
        	// attempt to parse as BEAST 1 xml
        	try {
        		Plugin alignment = parseBeast1XML(sXML);
        		if (alignment != null) {
        			alignment.setID(file.getName().substring(0, file.getName().length() - 4));
        		}
        		return alignment;
            } catch (Exception ex2) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "Loading of " + file.getName() + " failed: " + ex.getMessage() + "\n" + ex2.getMessage());
            }
            return null;
        }
    }

    private static Plugin parseBeast1XML(String sXML) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        Document doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(sXML)));
        doc.normalize();

        NodeList alignments = doc.getElementsByTagName("alignment");
        Alignment alignment = new Alignment();
        alignment.m_sDataType.setValue("nucleotide", alignment);

        // parse first alignment
        org.w3c.dom.Node node = alignments.item(0);
        
        String sDataType = node.getAttributes().getNamedItem("dataType").getNodeValue();
        int nTotalCount = 4;
        if (sDataType == null) {
            alignment.m_sDataType.setValue("integer", alignment);
        } else if (sDataType.toLowerCase().equals("dna") || sDataType.toLowerCase().equals("nucleotide")) {
            alignment.m_sDataType.setValue("nucleotide", alignment);
            nTotalCount = 4;
        } else if (sDataType.toLowerCase().equals("aminoacid") || sDataType.toLowerCase().equals("protein")) {
            alignment.m_sDataType.setValue("aminoacid", alignment);
            nTotalCount = 20;
        } else {
            alignment.m_sDataType.setValue("integer", alignment);
        }
        
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
        	org.w3c.dom.Node child = children.item(i);
        	if (child.getNodeName().equals("sequence")) {
                Sequence sequence = new Sequence();
                // find the taxon
                String taxon = "";
                NodeList sequenceChildren = child.getChildNodes();
                for (int j = 0; j < sequenceChildren.getLength(); j++) {
                	org.w3c.dom.Node child2 = sequenceChildren.item(j);
                	if (child2.getNodeName().equals("taxon")) {
                		taxon = child2.getAttributes().getNamedItem("idref").getNodeValue();
                	}
                }
                String data = child.getTextContent();
                sequence.initByName("totalcount", nTotalCount, "taxon", taxon, "value", data);
                sequence.setID("seq_" + taxon);
                alignment.m_pSequences.setValue(sequence, alignment);
        		
        	}
        }
    	//alignment.initAndValidate();
        alignment.setID("beast1");
    	return alignment;
	} // parseBeast1XML

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
