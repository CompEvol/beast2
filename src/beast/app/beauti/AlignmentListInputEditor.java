package beast.app.beauti;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.Box;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.CellEditorListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import beast.app.draw.ListInputEditor;
import beast.app.draw.SmallButton;
import beast.core.Input;
import beast.core.MCMC;
import beast.core.Plugin;
import beast.core.Input.Validate;
import beast.core.util.CompoundDistribution;
import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.FilteredAlignment;
import beast.evolution.branchratemodel.BranchRateModel;
import beast.evolution.likelihood.TreeLikelihood;
import beast.evolution.sitemodel.SiteModel;
import beast.evolution.tree.Tree;

// TODO: add useAmbiguities flag
// TODO: add warning if useAmbiguities=false and nr of patterns=1 (happens when all data is ambiguous)

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
	JTextField nameEditor;
	List<JButton> linkButtons;
	List<JButton> unlinkButtons;
	JButton splitButton; 
	JButton delButton;

	JScrollPane scrollPane;

	// public AlignmentListInputEditor() {}
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
	public void init(Input<?> input, Plugin plugin, int itemNr, ExpandOption bExpandOption, boolean bAddButtons) {
		this.itemNr = itemNr;
		if (input.get() instanceof List) {
			alignments = (List<Alignment>) input.get();
		} else {
			// we just have a single Alignment
			alignments = new ArrayList<Alignment>();
			alignments.add((Alignment) input.get());
		}
		linkButtons = new ArrayList<JButton>();
		unlinkButtons = new ArrayList<JButton>();
		nPartitions = alignments.size();
		// super.init(input, plugin, bExpandOption, false);
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		
		Box box = Box.createVerticalBox();
		box.add(Box.createVerticalStrut(5));
		box.add(createButtonBox());
		box.add(Box.createVerticalStrut(5));
		box.add(createListBox());
		panel.add(box, BorderLayout.NORTH);

		Box buttonBox = Box.createHorizontalBox();

		m_addButton = new SmallButton("+", true, SmallButton.ButtonType.square);
		m_addButton.setName("+");
		m_addButton.setToolTipText("Add item to the list");
		m_addButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				addItem();
			}
		});
		buttonBox.add(Box.createHorizontalStrut(5));
		buttonBox.add(m_addButton);
		buttonBox.add(Box.createHorizontalStrut(5));

		delButton = new SmallButton("-", true, SmallButton.ButtonType.square);
		delButton.setName("-");
		delButton.setToolTipText("Delete selected items from the list");
		delButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (doc.bHasLinkedAtLeastOnce) {
					JOptionPane.showMessageDialog(null, "Cannot delete partition while parameters are linked");
					return;
				}
				delItem();
			}
		});
		buttonBox.add(delButton);
		buttonBox.add(Box.createHorizontalStrut(5));

		splitButton = new JButton("Split");
		splitButton.setName("Split");
		splitButton.setToolTipText("Split alignment into partitions, for example, codon positions");
		splitButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				splitItem();
			}
		});
		buttonBox.add(splitButton);

		buttonBox.add(Box.createHorizontalGlue());
		panel.add(buttonBox, BorderLayout.SOUTH);
		add(panel);
		
		updateStatus();
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
		linkSModelButton.setName("Link " + sLabel);
		linkSModelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JButton button = (JButton) e.getSource();
				link(columnLabelToNr(button.getText()));
				table.repaint();
			}

		});
		box.add(linkSModelButton);
		linkSModelButton.setEnabled(!getDoc().bHasLinkedAtLeastOnce);
		JButton unlinkSModelButton = new JButton("Unlink " + sLabel);
		unlinkSModelButton.setName("Unlink " + sLabel);
		unlinkSModelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JButton button = (JButton) e.getSource();
				unlink(columnLabelToNr(button.getText()));
				table.repaint();
			}

		});
		box.add(unlinkSModelButton);
		unlinkSModelButton.setEnabled(!getDoc().bHasLinkedAtLeastOnce);
		box.add(Box.createHorizontalGlue());
		
		linkButtons.add(linkSModelButton);
		unlinkButtons.add(unlinkSModelButton);
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
			tableData[iRow][nColumn] = getDoc().sPartitionNames.get(iRow).partition;
			try {
				updateModel(nColumn, iRow);
			} catch (Exception ex) {
				System.err.println(ex.getMessage());
				ex.printStackTrace();
			}
		}
	}

	int[] getTableRowSelection() {
		int[] nSelected = table.getSelectedRows();
//		if (nSelected.length == 0) {
//			// select all
//			nSelected = new int[nPartitions];
//			for (int i = 0; i < nPartitions; i++) {
//				nSelected[i] = i;
//			}
//		}
		return nSelected;
	}

	/** set partition of type nColumn to partition model nr iRow **/
	void updateModel(int nColumn, int iRow) throws Exception {
		System.err.println("updateModel: " + iRow + " " + nColumn + " " + table.getSelectedRow() + " "
				+ table.getSelectedColumn());
		for (int i = 0; i < nPartitions; i++) {
			System.err.println(i + " " + tableData[i][0] + " " + tableData[i][SITEMODEL_COLUMN] + " "
					+ tableData[i][CLOCKMODEL_COLUMN] + " " + tableData[i][TREE_COLUMN]);
		}

		getDoc();
		String sPartition = (String) tableData[iRow][nColumn];

		// check if partition needs renaming
		String oldName = null;
		boolean isRenaming = false;
		try {
			switch (nColumn) {
			case SITEMODEL_COLUMN:
				if (!doc.pluginmap.containsKey("SiteModel.s:" + sPartition)) {
					String sID = likelihoods[iRow].m_pSiteModel.get().getID();
					oldName = BeautiDoc.parsePartition(sID);
					doc.renamePartition(BeautiDoc.SITEMODEL_PARTITION, oldName, sPartition);
					isRenaming = true;
				}
				break;
			case CLOCKMODEL_COLUMN: {
				String sID = likelihoods[iRow].m_pBranchRateModel.get().getID();
				String sClockModelName = sID.substring(0, sID.indexOf('.')) + ".c:" + sPartition;
				if (!doc.pluginmap.containsKey(sClockModelName)) {
					oldName = BeautiDoc.parsePartition(sID);
					doc.renamePartition(BeautiDoc.CLOCKMODEL_PARTITION, oldName, sPartition);
					isRenaming = true;
				}
			}
				break;
			case TREE_COLUMN:
				if (!doc.pluginmap.containsKey("Tree.t:" + sPartition)) {
					String sID = likelihoods[iRow].m_tree.get().getID();
					oldName = BeautiDoc.parsePartition(sID);
					doc.renamePartition(BeautiDoc.TREEMODEL_PARTITION, oldName, sPartition);
					isRenaming = true;
				}
				break;
			}
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, "Cannot rename item: " + e.getMessage());
			tableData[iRow][nColumn] = oldName;
			return;
		}
		if (isRenaming) {
			doc.determinePartitions();
			initTableData();
			setUpComboBoxes();
			table.repaint();
			return;
		}
		
		int partitionID = BeautiDoc.ALIGNMENT_PARTITION;
		switch (nColumn) {
		case SITEMODEL_COLUMN:
			partitionID = BeautiDoc.SITEMODEL_PARTITION;
			break;
		case CLOCKMODEL_COLUMN:
			partitionID = BeautiDoc.CLOCKMODEL_PARTITION;
			break;
		case TREE_COLUMN:
			partitionID = BeautiDoc.TREEMODEL_PARTITION;
			break;
		}
		int nPartition = doc.getPartitionNr(sPartition, partitionID);
		TreeLikelihood treeLikelihood = null;
		if (nPartition >= 0) {
			// we ar linking
			treeLikelihood = likelihoods[nPartition];
		}
		// (TreeLikelihood) doc.pluginmap.get("treeLikelihood." +
		// tableData[iRow][NAME_COLUMN]);

		boolean needsRePartition = false;
		
		PartitionContext oldContext = new PartitionContext(this.likelihoods[iRow]);

		switch (nColumn) {
		case SITEMODEL_COLUMN: {
			SiteModel.Base siteModel = null;
			if (treeLikelihood != null) { // getDoc().getPartitionNr(sPartition,
											// BeautiDoc.SITEMODEL_PARTITION) !=
											// iRow) {
				siteModel = treeLikelihood.m_pSiteModel.get();
			} else {
				siteModel = (SiteModel) doc.pluginmap.get("SiteModel.s:" + sPartition);
				if (siteModel != likelihoods[iRow].m_pSiteModel.get()) {
					PartitionContext context = getPartitionContext(iRow);
					siteModel = (SiteModel.Base) BeautiDoc.deepCopyPlugin(likelihoods[iRow].m_pSiteModel.get(),
							likelihoods[iRow], (MCMC) doc.mcmc.get(), context, doc, null);
				}
			}
			SiteModel.Base target = this.likelihoods[iRow].m_pSiteModel.get();
			if (!target.m_pSubstModel.canSetValue(siteModel.m_pSubstModel.get(), target)) {
				throw new Exception("Cannot link site model: substitution models are incompatible");
			}
			needsRePartition = (this.likelihoods[iRow].m_pSiteModel.get() != siteModel);
			this.likelihoods[iRow].m_pSiteModel.setValue(siteModel, this.likelihoods[iRow]);

			sPartition = likelihoods[iRow].m_pSiteModel.get().getID();
			sPartition = BeautiDoc.parsePartition(sPartition);
			getDoc().setCurrentPartition(BeautiDoc.SITEMODEL_PARTITION, iRow, sPartition);
		}
			break;
		case CLOCKMODEL_COLUMN: {
			BranchRateModel clockModel = null;
			if (treeLikelihood != null) { // getDoc().getPartitionNr(sPartition,
											// BeautiDoc.CLOCKMODEL_PARTITION)
											// != iRow) {
				clockModel = treeLikelihood.m_pBranchRateModel.get();
			} else {
				clockModel = getDoc().getClockModel(sPartition);
				if (clockModel != likelihoods[iRow].m_pBranchRateModel.get()) {
					PartitionContext context = getPartitionContext(iRow);
					clockModel = (BranchRateModel) BeautiDoc.deepCopyPlugin(likelihoods[iRow].m_pBranchRateModel.get(),
							likelihoods[iRow], (MCMC) doc.mcmc.get(), context, doc, null);
				}
			}
			// make sure that *if* the clock model has a tree as input, it is
			// the same as
			// for the likelihood
			Tree tree = null;
			for (Input<?> input : ((Plugin) clockModel).listInputs()) {
				if (input.getName().equals("tree")) {
					tree = (Tree) input.get();
				}

			}
			if (tree != null && tree != this.likelihoods[iRow].m_tree.get()) {
				throw new Exception("Cannot link clock model with different trees");
			}

			needsRePartition = (this.likelihoods[iRow].m_pBranchRateModel.get() != clockModel);
			this.likelihoods[iRow].m_pBranchRateModel.setValue(clockModel, this.likelihoods[iRow]);
			sPartition = likelihoods[iRow].m_pBranchRateModel.get().getID();
			sPartition = BeautiDoc.parsePartition(sPartition);
			getDoc().setCurrentPartition(BeautiDoc.CLOCKMODEL_PARTITION, iRow, sPartition);
		}
			break;
		case TREE_COLUMN: {
			Tree tree = null;
			if (treeLikelihood != null) { // getDoc().getPartitionNr(sPartition,
											// BeautiDoc.TREEMODEL_PARTITION) !=
											// iRow) {
				tree = treeLikelihood.m_tree.get();
			} else {
				tree = (Tree) doc.pluginmap.get("Tree.t:" + sPartition);
				if (tree != likelihoods[iRow].m_tree.get()) {
					PartitionContext context = getPartitionContext(iRow);
					tree = (Tree) BeautiDoc.deepCopyPlugin(likelihoods[iRow].m_tree.get(), likelihoods[iRow],
							(MCMC) doc.mcmc.get(), context, doc, null);
				}
			}
			// sanity check: make sure taxon sets are compatible
			String[] taxa = tree.getTaxaNames();
			List<String> taxa2 = this.likelihoods[iRow].m_data.get().getTaxaNames();
			if (taxa.length != taxa2.size()) {
				throw new Exception("Cannot link trees: incompatible taxon sets");
			}
			for (String taxon : taxa) {
				boolean found = false;
				for (String taxon2 : taxa2) {
					if (taxon.equals(taxon2)) {
						found = true;
						break;
					}
				}
				if (!found) {
					throw new Exception("Cannot link trees: taxon" + taxon + "is not in alignment");
				}
			}

			needsRePartition = (this.likelihoods[iRow].m_tree.get() != tree);
System.err.println("needsRePartition = " + needsRePartition);			
			if (needsRePartition) {
				Tree oldTree = this.likelihoods[iRow].m_tree.get();
				List<Tree> tModels = new ArrayList<Tree>();
				for (TreeLikelihood likelihood : likelihoods) {
					if (likelihood.m_tree.get() == oldTree) {
						tModels.add(likelihood.m_tree.get());
					}
				}
				if (tModels.size() == 1) {
					// remove old tree from model
					oldTree.m_bIsEstimated.setValue(false, this.likelihoods[iRow].m_tree.get());
					for (Plugin plugin : oldTree.outputs.toArray(new Plugin[0])) {
						for (Input<?> input : plugin.listInputs()) {
							try {
							if (input.get() == oldTree && input.getRule() != Input.Validate.REQUIRED) {
								input.setValue(null, plugin);
							} else if (input.get() instanceof List) {
								List<?> list = (List<?>) input.get();
								if (list.contains(oldTree) && input.getRule() != Validate.REQUIRED) {
									list.remove(oldTree);
								}
							}
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
			likelihoods[iRow].m_tree.setValue(tree, likelihoods[iRow]);
			// TreeDistribution d = getDoc().getTreePrior(sPartition);
			// CompoundDistribution prior = (CompoundDistribution)
			// doc.pluginmap.get("prior");
			// if (!getDoc().posteriorPredecessors.contains(d)) {
			// prior.pDistributions.setValue(d, prior);
			// }
			sPartition = likelihoods[iRow].m_tree.get().getID();
			sPartition = BeautiDoc.parsePartition(sPartition);
			getDoc().setCurrentPartition(BeautiDoc.TREEMODEL_PARTITION, iRow, sPartition);
		}
		}
		tableData[iRow][nColumn] = sPartition;
		if (needsRePartition) {
			doc.setUpActivePlugins();
			List<BeautiSubTemplate> templates = new ArrayList<BeautiSubTemplate>();
			templates.add(doc.beautiConfig.partitionTemplate.get());
			templates.addAll(doc.beautiConfig.subTemplates);
			doc.applyBeautiRules(templates, false, oldContext);
			doc.determinePartitions();
		}
		if (treeLikelihood == null) {
			initTableData();
			setUpComboBoxes();
		}
		
		updateStatus();
	}

	private PartitionContext getPartitionContext(int iRow) {
		PartitionContext context = new PartitionContext(
				tableData[iRow][NAME_COLUMN].toString(),
				tableData[iRow][SITEMODEL_COLUMN].toString(),
				tableData[iRow][CLOCKMODEL_COLUMN].toString(),
				tableData[iRow][TREE_COLUMN].toString());
		return context;
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
		String sPartition = BeautiDoc.parsePartition(sID);
		return sPartition;
	}

	protected Component createListBox() {
		String[] columnData = new String[] { "Name", "File", "Taxa", "Sites", "Data Type", "Site Model", "Clock Model",
				"Tree" };
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
				updateStatus();
				return comp;
			}
		};
		table.setRowHeight(25);
		table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		table.setColumnSelectionAllowed(false);
		table.setRowSelectionAllowed(true);
		table.setName("alignmenttable");

		setUpComboBoxes();

		TableColumn col = table.getColumnModel().getColumn(NAME_COLUMN);
		nameEditor = new JTextField();
		nameEditor.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void removeUpdate(DocumentEvent e) {
				processPartitionName();
			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				processPartitionName();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				processPartitionName();
			}
		});

		col.setCellEditor(new DefaultCellEditor(nameEditor));

		// // set up editor that makes sure only doubles are accepted as entry
		// // and only the Date column is editable.
		table.setDefaultEditor(Object.class, new TableCellEditor() {
			JTextField m_textField = new JTextField();
			int m_iRow, m_iCol;

			@Override
			public boolean stopCellEditing() {
				System.err.println("stopCellEditing()");
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
				System.err.println("isCellEditable()");
				return table.getSelectedColumn() == 0;
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
						Alignment alignment = alignments.get(iAlignmemt);
						int best = 0;
						BeautiAlignmentProvider provider = null;
						for (BeautiAlignmentProvider provider2 : doc.beautiConfig.alignmentProvider) {
							int match = provider2.matches(alignment);
							if (match > best) {
								best = match;
								provider = provider2;
							}
						}
						provider.editAlignment(alignment, doc);
					} catch (Exception e1) {
						e1.printStackTrace();
					}
					updateStatus();
				}
			}
		});

		scrollPane = new JScrollPane(table);
		
		scrollPane.addComponentListener(new ComponentListener() {
			
			@Override
			public void componentShown(ComponentEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void componentResized(ComponentEvent e) {
				if (doc.getFrame() != null) {
					Dimension preferredSize = doc.getFrame().getSize();
					preferredSize.height = Math.max(preferredSize.height - 150, 0);
					preferredSize.width = Math.max(preferredSize.width - 20, 0);
					scrollPane.setPreferredSize(preferredSize);
				}				
			}
			
			@Override
			public void componentMoved(ComponentEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void componentHidden(ComponentEvent e) {
				// TODO Auto-generated method stub
				
			}
		});
		return scrollPane;
	} // createListBox
	
	void setUpComboBoxes() {
		// set up comboboxes
		Set<String>[] partitionNames = new HashSet[3];
		for (int i = 0; i < 3; i++) {
			partitionNames[i] = new HashSet<String>();
		}
		for (int i = 0; i < nPartitions; i++) {
			partitionNames[0].add(likelihoods[i].m_pSiteModel.get().getID());
			partitionNames[1].add(likelihoods[i].m_pBranchRateModel.get().getID());
			partitionNames[2].add(likelihoods[i].m_tree.get().getID());
		}
		String[][] sPartitionNames = new String[3][];
		for (int i = 0; i < 3; i++) {
			sPartitionNames[i] = partitionNames[i].toArray(new String[0]);
		}
		for (int j = 0; j < 3; j++) {
			for (int i = 0; i < sPartitionNames[j].length; i++) {
				sPartitionNames[j][i] = BeautiDoc.parsePartition(sPartitionNames[j][i]);
			}
		}
		TableColumn col = table.getColumnModel().getColumn(SITEMODEL_COLUMN);
		JComboBox siteModelComboBox = new JComboBox(sPartitionNames[0]);
		siteModelComboBox.setEditable(true);
		siteModelComboBox.addActionListener(new ComboActionListener(SITEMODEL_COLUMN));

		col.setCellEditor(new DefaultCellEditor(siteModelComboBox));
		// If the cell should appear like a combobox in its
		// non-editing state, also set the combobox renderer
		col.setCellRenderer(new MyComboBoxRenderer(sPartitionNames[0]));
		col = table.getColumnModel().getColumn(CLOCKMODEL_COLUMN);

		JComboBox clockModelComboBox = new JComboBox(sPartitionNames[1]);
		clockModelComboBox.setEditable(true);
		clockModelComboBox.addActionListener(new ComboActionListener(CLOCKMODEL_COLUMN));

		col.setCellEditor(new DefaultCellEditor(clockModelComboBox));
		col.setCellRenderer(new MyComboBoxRenderer(sPartitionNames[1]));
		col = table.getColumnModel().getColumn(TREE_COLUMN);

		JComboBox treeComboBox = new JComboBox(sPartitionNames[2]);
		treeComboBox.setEditable(true);
		treeComboBox.addActionListener(new ComboActionListener(TREE_COLUMN));
		col.setCellEditor(new DefaultCellEditor(treeComboBox));
		col.setCellRenderer(new MyComboBoxRenderer(sPartitionNames[2]));
		col = table.getColumnModel().getColumn(TAXA_COLUMN);
		col.setPreferredWidth(30);
		col = table.getColumnModel().getColumn(SITES_COLUMN);
		col.setPreferredWidth(30);
	}

	void processPartitionName() {
		System.err.println("processPartitionName");
		System.err.println(table.getSelectedColumn() + " " + table.getSelectedRow());
		String oldName = tableData[table.getSelectedRow()][table.getSelectedColumn()].toString();
		String newName = nameEditor.getText();
		if (!oldName.equals(newName)) {
			try {
				int partitionID = -2;
				switch (table.getSelectedColumn()) {
				case NAME_COLUMN:
					partitionID = BeautiDoc.ALIGNMENT_PARTITION;
					break;
				case SITEMODEL_COLUMN:
					partitionID = BeautiDoc.SITEMODEL_PARTITION;
					break;
				case CLOCKMODEL_COLUMN:
					partitionID = BeautiDoc.CLOCKMODEL_PARTITION;
					break;
				case TREE_COLUMN:
					partitionID = BeautiDoc.TREEMODEL_PARTITION;
					break;
				default:
					throw new Exception("Cannot rename item in column");
				}
				getDoc().renamePartition(partitionID, oldName, newName);
				table.setValueAt(newName, table.getSelectedRow(), table.getSelectedColumn());
				setUpComboBoxes();
			} catch (Exception e) {
				JOptionPane.showMessageDialog(null, "Renaming failed: " + e.getMessage());
			}
		}
		// debugging code:
		for (int i = 0; i < nPartitions; i++) {
			System.err.println(i + " " + tableData[i][0]);
		}
	}

	class ComboActionListener implements ActionListener {
		int m_nColumn;

		public ComboActionListener(int nColumn) {
			m_nColumn = nColumn;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
//			 SwingUtilities.invokeLater(new Runnable() {
//			 @Override
//			 public void run() {
			System.err.println("actionPerformed ");
			System.err.println(table.getSelectedRow() + " " + table.getSelectedColumn());
			if (table.getSelectedRow() >= 0 && table.getSelectedColumn() >= 0) {
				System.err.println(" " + table.getValueAt(table.getSelectedRow(), table.getSelectedColumn()));
			}
			for (int i = 0; i < nPartitions; i++) {
				try {
					updateModel(m_nColumn, i);
				} catch (Exception ex) {
					System.err.println(ex.getMessage());
				}
			}
//		    }});
		}
	}

	public class MyComboBoxRenderer extends JComboBox implements TableCellRenderer {
		private static final long serialVersionUID = 1L;

		public MyComboBoxRenderer(String[] items) {
			super(items);
			setOpaque(true);
		}

		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
				boolean hasFocus, int row, int column) {
			if (isSelected) {
				// setForeground(table.getSelectionForeground());
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

		// Component c = this;
		if (plugins != null) {
			refreshPanel();
		}
	} // addItem

	void delItem() {
		int[] nSelected = getTableRowSelection();
		if (nSelected.length == 0) {
			JOptionPane.showMessageDialog(this, "Select partitions to delete, before hitting the delete button");
		}
		// do the actual deleting
		for (int i = nSelected.length - 1; i >= 0; i--) {
			int iRow = nSelected[i];
			
			// before deleting, unlink site model, clock model and tree
			
			// check whether any of the models are linked
			BranchRateModel.Base clockModel = likelihoods[iRow].m_pBranchRateModel.get();
			SiteModel.Base siteModel = likelihoods[iRow].m_pSiteModel.get();
			Tree tree = likelihoods[iRow].m_tree.get();
			List<TreeLikelihood> cModels = new ArrayList<TreeLikelihood>();
			List<TreeLikelihood> sModels = new ArrayList<TreeLikelihood>();
			List<TreeLikelihood> tModels = new ArrayList<TreeLikelihood>();
			for (TreeLikelihood likelihood : likelihoods) {
				if (likelihood != likelihoods[iRow]) {
				if (likelihood.m_pBranchRateModel.get() == clockModel) {
					cModels.add(likelihood);
				}
				if (likelihood.m_pSiteModel.get() == siteModel) {
					sModels.add(likelihood);
				}
				if (likelihood.m_tree.get() == tree) {
					tModels.add(likelihood);
				}
				}
			}
			
			try {
				if (cModels.size() > 0) {
					// clock model is linked, so we need to unlink
					if (doc.getPartitionNr(clockModel) != iRow) {
						tableData[iRow][CLOCKMODEL_COLUMN] = getDoc().sPartitionNames.get(iRow).partition;
					} else {
						int iFreePartition = doc.getPartitionNr(cModels.get(0));
						tableData[iRow][CLOCKMODEL_COLUMN] = getDoc().sPartitionNames.get(iFreePartition).partition;
					}
					updateModel(CLOCKMODEL_COLUMN, iRow);
				}
				
				if (sModels.size() > 0) {
					// site model is linked, so we need to unlink
					if (doc.getPartitionNr(siteModel) != iRow) {
						tableData[iRow][SITEMODEL_COLUMN] = getDoc().sPartitionNames.get(iRow).partition;
					} else {
						int iFreePartition = doc.getPartitionNr(sModels.get(0));
						tableData[iRow][SITEMODEL_COLUMN] = getDoc().sPartitionNames.get(iFreePartition).partition;
					}
					updateModel(SITEMODEL_COLUMN, iRow);
				}
				
				if (tModels.size() > 0) {
					// tree is linked, so we need to unlink
					if (doc.getPartitionNr(tree) != iRow) {
						tableData[iRow][TREE_COLUMN] = getDoc().sPartitionNames.get(iRow).partition;
					} else {
						int iFreePartition = doc.getPartitionNr(tModels.get(0));
						tableData[iRow][TREE_COLUMN] = getDoc().sPartitionNames.get(iFreePartition).partition;
					}
					updateModel(TREE_COLUMN, iRow);
				}
				getDoc().delAlignmentWithSubnet(alignments.get(iRow));
				alignments.remove(iRow);
			} catch (Exception e) {
				JOptionPane.showMessageDialog(null, "Deletion failed: " + e.getMessage());
				e.printStackTrace();
			}
		}
		refreshPanel();
	} // delItem

	void splitItem() {
		int[] nSelected = getTableRowSelection();
		if (nSelected.length == 0) {
			JOptionPane.showMessageDialog(this, "Select partitions to split, before hitting the split button");
			return;
		}
		String[] options = { "{1,2} + 3", "{1,2} + 3 frame 2", "{1,2} + 3 frame 3", "1 + 2 + 3", "1 + 2 + 3 frame 2", "1 + 2 + 3 frame 3"};

		String option = (String)JOptionPane.showInputDialog(null, "Split selected alignments into partitions", "Option",
		                    JOptionPane.WARNING_MESSAGE, null, options, "1 + 2 + 3");
		if (option == null) {
			return;
		}
		
		String[] filters = null;
		String[] ids = null;
		if (option.equals(options[0])) {
			filters = new String[] { "1::3,2::3", "3::3" };
			ids = new String[] { "_1,2", "_3" };
		} else if (option.equals(options[1])) {
			filters = new String[] { "1::3,3::3", "2::3" };
			ids = new String[] { "_1,2", "_3" };
		} else if (option.equals(options[2])) {
			filters = new String[] { "2::3,3::3", "1::3" };
			ids = new String[] { "_1,2", "_3" };
		} else if (option.equals(options[3])) {
			filters = new String[] { "1::3", "2::3", "3::3" };
			ids = new String[] { "_1", "_2", "_3" };
		} else if (option.equals(options[4])) {
			filters = new String[] { "2::3", "3::3", "1::3" };
			ids = new String[] { "_1", "_2", "_3" };
		} else if (option.equals(options[5])) {
			filters = new String[] { "3::3", "1::3", "2::3" };
			ids = new String[] { "_1", "_2", "_3" };
		} else {
			return;
		}

		for (int i = nSelected.length - 1; i >= 0; i--) {
			int iRow = nSelected[i];
			Alignment alignment = alignments.remove(iRow);
			getDoc().delAlignmentWithSubnet(alignment);
			try {
				for (int j = 0; j < filters.length; j++) {
					FilteredAlignment f = new FilteredAlignment();
					f.initByName("data", alignment, "filter", filters[j], "dataType", alignment.m_sDataType.get());
					f.setID(alignment.getID() + ids[j]);
					getDoc().addAlignmentWithSubnet(f);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		refreshPanel();
	} // splitItem

	@Override
	public List<Plugin> pluginSelector(Input<?> input, Plugin plugin, List<String> sTabuList) {
		List<BeautiAlignmentProvider> providers = doc.beautiConfig.alignmentProvider;
		BeautiAlignmentProvider selectedProvider = null;
		if (providers.size() == 1) {
			selectedProvider = providers.get(0);
		} else {
			selectedProvider = (BeautiAlignmentProvider) JOptionPane.showInputDialog(this, "Select what to add", 
					"Add partition",  
					JOptionPane.QUESTION_MESSAGE, null, providers.toArray(), 
					providers.get(0));
			if (selectedProvider == null) {
				return null;
			}
		}
		List<Plugin> selectedPlugins = selectedProvider.getAlignments(doc);
		return selectedPlugins;
		
	} // pluginSelector
	
	
	/** enable/disable buttons, etc **/
	void updateStatus() {
		boolean status = (alignments.size() > 1);
		if (alignments.size() >= 2 && getTableRowSelection().length == 0) {
			status = false;
		}
		for (JButton button : linkButtons) {
			button.setEnabled(status);
		}
		for (JButton button : unlinkButtons) {
			button.setEnabled(status);
		}
		status = (getTableRowSelection().length > 0);
		splitButton.setEnabled(status);
		delButton.setEnabled(status);
	}
	
} // class AlignmentListInputEditor
