package beast.app.inputeditor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.CellEditorListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import beast.app.util.FileDrop;
import beast.app.util.PartitionContextUtil;
import beast.base.BEASTInterface;
import beast.base.Input;
import beast.base.Log;
import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.FilteredAlignment;
import beast.evolution.alignment.Taxon;
import beast.evolution.branchratemodel.BranchRateModel;
import beast.evolution.likelihood.GenericTreeLikelihood;
import beast.evolution.sitemodel.SiteModel;
import beast.evolution.sitemodel.SiteModelInterface;
import beast.evolution.tree.TreeInterface;
import beast.inference.MCMC;
import beast.inference.State;
import beast.inference.StateNode;
import beast.inference.util.CompoundDistribution;
import beast.parser.PartitionContext;

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
	final static int USE_AMBIGUITIES_COLUMN = 8;
	
	final static int NR_OF_COLUMNS = 9;

    final static int STRUT_SIZE = 5;

	/**
	 * alignments that form a partition. These can be FilteredAlignments *
	 */
	List<Alignment> alignments;
	int partitionCount;
	GenericTreeLikelihood[] likelihoods;
	Object[][] tableData;
	JTable table;
	JTextField nameEditor;
	List<JButton> linkButtons;
	List<JButton> unlinkButtons;
	JButton splitButton;

    /**
     * The button for deleting an alignment in the alignment list.
     */
    JButton delButton;
    protected SmallButton replaceButton;

	private JScrollPane scrollPane;

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

	@Override
	@SuppressWarnings("unchecked")
	public void init(Input<?> input, BEASTInterface beastObject, int itemNr, ExpandOption isExpandOption, boolean addButtons) {
		this.itemNr = itemNr;
		if (input.get() instanceof List) {
			alignments = (List<Alignment>) input.get();
		} else {
			// we just have a single Alignment
			alignments = new ArrayList<>();
			alignments.add((Alignment) input.get());
		}
		linkButtons = new ArrayList<>();
		unlinkButtons = new ArrayList<>();
		partitionCount = alignments.size();

        // override BoxLayout in superclass
        setLayout(new BorderLayout());

        add(createLinkButtons(), BorderLayout.NORTH);
        add(createListBox(), BorderLayout.CENTER);

        //Box box = Box.createVerticalBox();
		//box.add(Box.createVerticalStrut(STRUT_SIZE));
		//box.add(createLinkButtons());
		//box.add(Box.createVerticalStrut(STRUT_SIZE));
		//box.add(createListBox());
        //box.add(Box.createVerticalStrut(STRUT_SIZE));
        //box.add(Box.createVerticalGlue());
		//add(box, BorderLayout.CENTER);

        Color focusColor = UIManager.getColor("Focus.color");
        Border focusBorder = BorderFactory.createMatteBorder(2, 2, 2, 2, focusColor);
        new FileDrop(null, scrollPane, focusBorder, new FileDrop.Listener() {
            @Override
			public void filesDropped(java.io.File[] files) {
            	SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						addItem(files);
					}
				});
            }   // end filesDropped
        }); // end FileDrop.Listener

        // this should place the add/remove/split buttons at the bottom of the window.
        add(createAddRemoveSplitButtons(), BorderLayout.SOUTH);

        updateStatus();
	}

    /**
     * Creates the link/unlink button component
     * @return a box containing three link/unlink button pairs.
     */
	private JComponent createLinkButtons() {

        Box box = Box.createHorizontalBox();
		addLinkUnlinkPair(box, "Site Models");
        box.add(Box.createHorizontalStrut(STRUT_SIZE));
        addLinkUnlinkPair(box, "Clock Models");
        box.add(Box.createHorizontalStrut(STRUT_SIZE));
        addLinkUnlinkPair(box, "Trees");
		box.add(Box.createHorizontalGlue());
		return box;
	}

    private JComponent createAddRemoveSplitButtons() {
        Box buttonBox = Box.createHorizontalBox();

        addButton = new SmallButton("+", true, SmallButton.ButtonType.square);
        addButton.setName("+");
        addButton.setToolTipText("Add item to the list");
        addButton.addActionListener(e -> addItem());
        buttonBox.add(Box.createHorizontalStrut(STRUT_SIZE));
        buttonBox.add(addButton);
        buttonBox.add(Box.createHorizontalStrut(STRUT_SIZE));

        delButton = new SmallButton("-", true, SmallButton.ButtonType.square);
        delButton.setName("-");
        delButton.setToolTipText("Delete selected items from the list");
        delButton.addActionListener(e -> {
            if (doc.hasLinkedAtLeastOnce) {
                JOptionPane.showMessageDialog(null, "Cannot delete partition while parameters are linked");
                return;
            }
            delItem();
        });
        buttonBox.add(delButton);
        buttonBox.add(Box.createHorizontalStrut(STRUT_SIZE));

        replaceButton = new SmallButton("r", true, SmallButton.ButtonType.square);
        replaceButton.setName("r");
        replaceButton.setToolTipText("Replace alignment by one loaded from file");
        replaceButton.addActionListener(e -> replaceItem());
        buttonBox.add(Box.createHorizontalStrut(STRUT_SIZE));
        buttonBox.add(replaceButton);
        buttonBox.add(Box.createHorizontalStrut(STRUT_SIZE));

        
        splitButton = new JButton("Split");
        splitButton.setName("Split");
        splitButton.setToolTipText("Split alignment into partitions, for example, codon positions");
        splitButton.addActionListener(e -> splitItem());
        buttonBox.add(splitButton);

        buttonBox.add(Box.createHorizontalGlue());

        return buttonBox;
    }

	/**
     * This method just adds the two buttons (with add()) and does not add any glue or struts before or after.
     * @param box
     * @param label
     */
	private void addLinkUnlinkPair(Box box, String label) {

        //JLabel label = new JLabel(label+":");
        //box.add(label);
        JButton linkSModelButton = new JButton("Link " + label);
		linkSModelButton.setName("Link " + label);
		linkSModelButton.addActionListener(e -> {
            JButton button = (JButton) e.getSource();
            link(columnLabelToNr(button.getText()));
            table.repaint();
        });
		box.add(linkSModelButton);
		linkSModelButton.setEnabled(!getDoc().hasLinkedAtLeastOnce);
		JButton unlinkSModelButton = new JButton("Unlink " + label);
		unlinkSModelButton.setName("Unlink " + label);
		unlinkSModelButton.addActionListener(e -> {
            JButton button = (JButton) e.getSource();
            unlink(columnLabelToNr(button.getText()));
            table.repaint();
        });
		box.add(unlinkSModelButton);
		unlinkSModelButton.setEnabled(!getDoc().hasLinkedAtLeastOnce);

		linkButtons.add(linkSModelButton);
		unlinkButtons.add(unlinkSModelButton);
	}

	private int columnLabelToNr(String column) {
		int columnNr;
		if (column.contains("Tree")) {
			columnNr = TREE_COLUMN;
		} else if (column.contains("Clock")) {
			columnNr = CLOCKMODEL_COLUMN;
		} else {
			columnNr = SITEMODEL_COLUMN;
		}
		return columnNr;
	}

	private void link(int columnNr) {
		int[] selected = getTableRowSelection();
		// do the actual linking
		for (int i = 1; i < selected.length; i++) {
			int rowNr = selected[i];
			link(columnNr, rowNr, selected[0]);
		}
	}
	
	/** links partition in row "rowToLink" with partition in "rowToLinkWith" so that
	 * after linking there is only one partition for context "columnNr", namely that
	 * of "rowToLinkWith"
	 */
	private void link(int columnNr, int rowToLink, int rowToLinkWith) {
		Object old = tableData[rowToLink][columnNr];
		tableData[rowToLink][columnNr] = tableData[rowToLinkWith][columnNr];
		try {
			updateModel(columnNr, rowToLink);
		} catch (Exception ex) {
			Log.warning.println(ex.getMessage());
			// unlink if we could not link
			tableData[rowToLink][columnNr] = old;
			try {
				updateModel(columnNr, rowToLink);
			} catch (Exception ex2) {
				// ignore
			}
		}
		MRCAPriorInputEditor.customConnector(doc);
	}

	
	private void unlink(int columnNr) {
		int[] selected = getTableRowSelection();
		for (int i = 1; i < selected.length; i++) {
			int rowNr = selected[i];
			tableData[rowNr][columnNr] = getDoc().partitionNames.get(rowNr).partition;
			try {
				updateModel(columnNr, rowNr);
			} catch (Exception ex) {
				Log.err.println(ex.getMessage());
				ex.printStackTrace();
			}
		}
	}


    int[] getTableRowSelection() {
        return table.getSelectedRows();
	}

	/** set partition of type columnNr to partition model nr rowNr **/
	void updateModel(int columnNr, int rowNr) {
		Log.warning.println("updateModel: " + rowNr + " " + columnNr + " " + table.getSelectedRow() + " "
				+ table.getSelectedColumn());
		for (int i = 0; i < partitionCount; i++) {
			Log.warning.println(i + " " + tableData[i][0] + " " + tableData[i][SITEMODEL_COLUMN] + " "
					+ tableData[i][CLOCKMODEL_COLUMN] + " " + tableData[i][TREE_COLUMN]);
		}

		getDoc();
		String partition = (String) tableData[rowNr][columnNr];

		// check if partition needs renaming
		String oldName = null;
		boolean isRenaming = false;
		try {
			switch (columnNr) {
			case SITEMODEL_COLUMN:
				if (!doc.pluginmap.containsKey("SiteModel.s:" + partition)) {
					String id = ((BEASTInterface)likelihoods[rowNr].siteModelInput.get()).getID();
					oldName = BeautiDoc.parsePartition(id);
					doc.renamePartition(BeautiDoc.SITEMODEL_PARTITION, oldName, partition);
					isRenaming = true;
				}
				break;
			case CLOCKMODEL_COLUMN: {
				String id = likelihoods[rowNr].branchRateModelInput.get().getID();
				String clockModelName = id.substring(0, id.indexOf('.')) + ".c:" + partition;
				if (!doc.pluginmap.containsKey(clockModelName)) {
					oldName = BeautiDoc.parsePartition(id);
					doc.renamePartition(BeautiDoc.CLOCKMODEL_PARTITION, oldName, partition);
					isRenaming = true;
				}
			}
				break;
			case TREE_COLUMN:
				if (!doc.pluginmap.containsKey("Tree.t:" + partition)) {
					String id = likelihoods[rowNr].treeInput.get().getID();
					oldName = BeautiDoc.parsePartition(id);
					doc.renamePartition(BeautiDoc.TREEMODEL_PARTITION, oldName, partition);
					isRenaming = true;
				}
				break;
			}
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, "Cannot rename item: " + e.getMessage());
			tableData[rowNr][columnNr] = oldName;
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
		switch (columnNr) {
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
		int partitionNr = doc.getPartitionNr(partition, partitionID);
		GenericTreeLikelihood treeLikelihood = null;
		if (partitionNr >= 0) {
			// we ar linking
			treeLikelihood = likelihoods[partitionNr];
		}
		// (TreeLikelihood) doc.pluginmap.get("treeLikelihood." +
		// tableData[rowNr][NAME_COLUMN]);

		boolean needsRePartition = false;
		
		PartitionContext oldContext = PartitionContextUtil.newPartitionContext(this.likelihoods[rowNr]);

		switch (columnNr) {
		case SITEMODEL_COLUMN: {
			SiteModelInterface siteModel = null;
			if (treeLikelihood != null) { // getDoc().getPartitionNr(partition,
											// BeautiDoc.SITEMODEL_PARTITION) !=
											// rowNr) {
				siteModel = treeLikelihood.siteModelInput.get();
			} else {
				siteModel = (SiteModel) doc.pluginmap.get("SiteModel.s:" + partition);
				if (siteModel != likelihoods[rowNr].siteModelInput.get()) {
					PartitionContext context = getPartitionContext(rowNr);
					try {
					siteModel = (SiteModel.Base) BeautiDoc.deepCopyPlugin((BEASTInterface) likelihoods[rowNr].siteModelInput.get(),
							likelihoods[rowNr], (MCMC) doc.mcmc.get(), oldContext, context, doc, null);
					} catch (RuntimeException e) {
						JOptionPane.showMessageDialog(this, "Could not clone site model: " + e.getMessage());
						return;
					}
				}
			}
			SiteModelInterface target = this.likelihoods[rowNr].siteModelInput.get();
			if (target instanceof SiteModel.Base && siteModel instanceof SiteModel.Base) {
				if (!((SiteModel.Base)target).substModelInput.canSetValue(((SiteModel.Base)siteModel).substModelInput.get(), (SiteModel.Base) target)) {
					throw new IllegalArgumentException("Cannot link site model: substitution models (" + 
							((SiteModel.Base)target).substModelInput.get().getClass().toString() + " and " +
							((SiteModel.Base)siteModel).substModelInput.get().getClass().toString() +
							") are incompatible");
				}
			} else {
				throw new IllegalArgumentException("Don't know how to link this site model");
			}
			needsRePartition = (this.likelihoods[rowNr].siteModelInput.get() != siteModel);
			this.likelihoods[rowNr].siteModelInput.setValue(siteModel, this.likelihoods[rowNr]);

			partition = ((BEASTInterface)likelihoods[rowNr].siteModelInput.get()).getID();
			partition = BeautiDoc.parsePartition(partition);
			getDoc().setCurrentPartition(BeautiDoc.SITEMODEL_PARTITION, rowNr, partition);
		}
			break;
		case CLOCKMODEL_COLUMN: {
			BranchRateModel clockModel = null;
			if (treeLikelihood != null) { // getDoc().getPartitionNr(partition,
											// BeautiDoc.CLOCKMODEL_PARTITION)
											// != rowNr) {
				clockModel = treeLikelihood.branchRateModelInput.get();
			} else {
				clockModel = getDoc().getClockModel(partition);
				if (clockModel != likelihoods[rowNr].branchRateModelInput.get()) {
					PartitionContext context = getPartitionContext(rowNr);
					try {
						clockModel = (BranchRateModel) BeautiDoc.deepCopyPlugin(likelihoods[rowNr].branchRateModelInput.get(),
							likelihoods[rowNr], (MCMC) doc.mcmc.get(), oldContext, context, doc, null);
					} catch (RuntimeException e) {
						JOptionPane.showMessageDialog(this, "Could not clone clock model: " + e.getMessage());
						return;
					}
				}
			}
			// make sure that *if* the clock model has a tree as input, it is
			// the same as
			// for the likelihood
			TreeInterface tree = null;
			for (Input<?> input : ((BEASTInterface) clockModel).listInputs()) {
				if (input.getName().equals("tree")) {
					tree = (TreeInterface) input.get();
				}

			}
			if (tree != null && tree != this.likelihoods[rowNr].treeInput.get()) {
				JOptionPane.showMessageDialog(this, "Cannot link clock model with different trees");
				throw new IllegalArgumentException("Cannot link clock model with different trees");
			}

			needsRePartition = (this.likelihoods[rowNr].branchRateModelInput.get() != clockModel);
			this.likelihoods[rowNr].branchRateModelInput.setValue(clockModel, this.likelihoods[rowNr]);
			partition = likelihoods[rowNr].branchRateModelInput.get().getID();
			partition = BeautiDoc.parsePartition(partition);
			getDoc().setCurrentPartition(BeautiDoc.CLOCKMODEL_PARTITION, rowNr, partition);
		}
			break;
		case TREE_COLUMN: {
			TreeInterface tree = null;
			if (treeLikelihood != null) { // getDoc().getPartitionNr(partition,
											// BeautiDoc.TREEMODEL_PARTITION) !=
											// rowNr) {
				tree = treeLikelihood.treeInput.get();
			} else {
				tree = (TreeInterface) doc.pluginmap.get("Tree.t:" + partition);
				if (tree != likelihoods[rowNr].treeInput.get()) {
					PartitionContext context = getPartitionContext(rowNr);
					try {
						tree = (TreeInterface) BeautiDoc.deepCopyPlugin((BEASTInterface) likelihoods[rowNr].treeInput.get(), likelihoods[rowNr],
							(MCMC) doc.mcmc.get(), oldContext, context, doc, null);
					} catch (RuntimeException e) {
						JOptionPane.showMessageDialog(this, "Could not clone tree model: " + e.getMessage());
						return;
					}
					
					State state = ((MCMC) doc.mcmc.get()).startStateInput.get();
					List<StateNode> stateNodes = new ArrayList<>();
					stateNodes.addAll(state.stateNodeInput.get());
					for (StateNode s : stateNodes) {
						if (s.getID().endsWith(".t:" + oldContext.tree) && !(s instanceof TreeInterface)) {
							try {
								@SuppressWarnings("unused")
								StateNode copy = (StateNode) BeautiDoc.deepCopyPlugin(s, likelihoods[rowNr], (MCMC) doc.mcmc.get(), oldContext, context, doc, null);
							} catch (RuntimeException e) {
								JOptionPane.showMessageDialog(this, "Could not clone tree model: " + e.getMessage());
								return;
							}

						}
					}
				}
			}
			// sanity check: make sure taxon sets are compatible
			Taxon.assertSameTaxa(tree.getID(), tree.getTaxonset().getTaxaNames(),
					likelihoods[rowNr].dataInput.get().getID(), likelihoods[rowNr].dataInput.get().getTaxaNames());

			needsRePartition = (this.likelihoods[rowNr].treeInput.get() != tree);
			Log.warning.println("needsRePartition = " + needsRePartition);			
			if (needsRePartition) {
				TreeInterface oldTree = this.likelihoods[rowNr].treeInput.get();
				List<TreeInterface> tModels = new ArrayList<>();
				for (GenericTreeLikelihood likelihood : likelihoods) {
					if (likelihood.treeInput.get() == oldTree) {
						tModels.add(likelihood.treeInput.get());
					}
				}
				if (tModels.size() == 1) {
					// remove old tree from model
					((BEASTInterface)oldTree).setInputValue("estimate", false);
                	// use toArray to prevent ConcurrentModificationException
					for (Object beastObject : BEASTInterface.getOutputs(oldTree).toArray()) { //.toArray(new BEASTInterface[0])) {
						for (Input<?> input : ((BEASTInterface)beastObject).listInputs()) {
							try {
							if (input.get() == oldTree) {
								if (input.getRule() != Input.Validate.REQUIRED) {
									input.setValue(tree/*null*/, (BEASTInterface) beastObject);
								//} else {
									//input.setValue(tree, (BEASTInterface) beastObject);
								}
							} else if (input.get() instanceof List) {
								@SuppressWarnings("unchecked")
								List<TreeInterface> list = (List<TreeInterface>) input.get();
								if (list.contains(oldTree)) { // && input.getRule() != Validate.REQUIRED) {
									list.remove(oldTree);
									if (!list.contains(tree)) {
										list.add(tree);
									}
								}
							}
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
			likelihoods[rowNr].treeInput.setValue(tree, likelihoods[rowNr]);
			// TreeDistribution d = getDoc().getTreePrior(partition);
			// CompoundDistribution prior = (CompoundDistribution)
			// doc.pluginmap.get("prior");
			// if (!getDoc().posteriorPredecessors.contains(d)) {
			// prior.pDistributions.setValue(d, prior);
			// }
			partition = likelihoods[rowNr].treeInput.get().getID();
			partition = BeautiDoc.parsePartition(partition);
			getDoc().setCurrentPartition(BeautiDoc.TREEMODEL_PARTITION, rowNr, partition);
		}
		}
		tableData[rowNr][columnNr] = partition;
		if (needsRePartition) {
			List<BeautiSubTemplate> templates = new ArrayList<>();
			templates.add(doc.beautiConfig.partitionTemplate.get());
			templates.addAll(doc.beautiConfig.subTemplates);
			// keep applying rules till model does not change
			doc.setUpActivePlugins();
			int n;
			do {
				n = doc.posteriorPredecessors.size();
				doc.applyBeautiRules(templates, false, oldContext);
				doc.setUpActivePlugins();
			} while (n != doc.posteriorPredecessors.size());
			doc.determinePartitions();
		}
		if (treeLikelihood == null) {
			initTableData();
			setUpComboBoxes();
		}
		
		updateStatus();
	}

	private PartitionContext getPartitionContext(int rowNr) {
		PartitionContext context = new PartitionContext(
				tableData[rowNr][NAME_COLUMN].toString(),
				tableData[rowNr][SITEMODEL_COLUMN].toString(),
				tableData[rowNr][CLOCKMODEL_COLUMN].toString(),
				tableData[rowNr][TREE_COLUMN].toString());
		return context;
	}

	@Override
	protected void addInputLabel() {
	}

	void initTableData() {
		this.likelihoods = new GenericTreeLikelihood[partitionCount];
		if (tableData == null) {
			tableData = new Object[partitionCount][NR_OF_COLUMNS];
		}
		CompoundDistribution likelihoods = (CompoundDistribution) doc.pluginmap.get("likelihood");

		for (int i = 0; i < partitionCount; i++) {
			Alignment data = alignments.get(i);
			// partition name
			tableData[i][NAME_COLUMN] = data;

			// alignment name
			if (data instanceof FilteredAlignment) {
				tableData[i][FILE_COLUMN] = ((FilteredAlignment) data).alignmentInput.get();
			} else {
				tableData[i][FILE_COLUMN] = data;
			}
			// # taxa
			tableData[i][TAXA_COLUMN] = data.getTaxonCount();
			// # sites
			tableData[i][SITES_COLUMN] = data.getSiteCount();
			// Data type
			tableData[i][TYPE_COLUMN] = data.getDataType();
			// site model
			GenericTreeLikelihood likelihood = (GenericTreeLikelihood) likelihoods.pDistributions.get().get(i);
			assert (likelihood != null);
			this.likelihoods[i] = likelihood;
			tableData[i][SITEMODEL_COLUMN] = getPartition(likelihood.siteModelInput);
			// clock model
			tableData[i][CLOCKMODEL_COLUMN] = getPartition(likelihood.branchRateModelInput);
			// tree
			tableData[i][TREE_COLUMN] = getPartition(likelihood.treeInput);
			// useAmbiguities
			tableData[i][USE_AMBIGUITIES_COLUMN] = null;
			try {
				if (hasUseAmbiguitiesInput(i)) {
					tableData[i][USE_AMBIGUITIES_COLUMN] = likelihood.getInputValue("useAmbiguities");
				}
			} catch (Exception e) {
				// ignore
			}
		}
	}

	private boolean hasUseAmbiguitiesInput(int i) {
		try {
			for (Input<?> input : likelihoods[i].listInputs()) {
				if (input.getName().equals("useAmbiguities")) {
					return true;
				}
			}
		} catch (Exception e) {
			// ignore
		}
		return false;
	}

	private String getPartition(Input<?> input) {
		BEASTInterface beastObject = (BEASTInterface) input.get();
		String id = beastObject.getID();
		String partition = BeautiDoc.parsePartition(id);
		return partition;
	}

	protected Component createListBox() {
		String[] columnData = new String[] { "Name", "File", "Taxa", "Sites", "Data Type", "Site Model", "Clock Model",
				"Tree", "Ambiguities" };
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
			    JComponent jcomp = (JComponent) comp;
		    	switch (Index_col) {
		    	case NAME_COLUMN:			    		
	    		case CLOCKMODEL_COLUMN: 
	    		case TREE_COLUMN: 
	    		case SITEMODEL_COLUMN: 
			        jcomp.setToolTipText("Set " + table.getColumnName(Index_col).toLowerCase() + " for this partition");
					break;
	    		case FILE_COLUMN:
	    		case TAXA_COLUMN:
	    		case SITES_COLUMN:
	    		case TYPE_COLUMN:
			        jcomp.setToolTipText("Report " + table.getColumnName(Index_col).toLowerCase() + " for this partition");
					break;
	    		case USE_AMBIGUITIES_COLUMN: 
					jcomp.setToolTipText("<html>Flag whether to use ambiguities.<br>" +
							"If not set, the treelikelihood will treat ambiguities in the<br>" +
							"data as unknowns<br>" +
							"If set, the treelikelihood will use ambiguities as equally<br>" +
							"likely values for the tips.<br>" +
							"This will make the computation twice as slow.</html>");
					break;
				default:
			        jcomp.setToolTipText(null);
		    	}
				updateStatus();
				return comp;
			}
		};
		int size = table.getFont().getSize();
		table.setRowHeight(25 * size/13);
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
				//Log.warning.println("stopCellEditing()");
				table.removeEditor();
				String text = m_textField.getText();
				try {
					Double.parseDouble(text);
				} catch (Exception e) {
					return false;
				}
				tableData[m_iRow][m_iCol] = text;
				return true;
			}

			@Override
			public boolean isCellEditable(EventObject anEvent) {
				//Log.warning.println("isCellEditable()");
				return table.getSelectedColumn() == 0;
			}

			@Override
			public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int rowNr,
					int colNr) {
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
						int alignmemt = table.rowAtPoint(e.getPoint());
						Alignment alignment = alignments.get(alignmemt);
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
				} else if (e.getButton() == e.BUTTON3) {
					int alignmemt = table.rowAtPoint(e.getPoint());
					Alignment alignment = alignments.get(alignmemt);
					int result = JOptionPane.showConfirmDialog(null, "Do you want to replace alignment " + alignment.getID());
					if (result == JOptionPane.YES_OPTION) {
						replaceItem(alignment);
					}
				}
			}
		});

		scrollPane = new JScrollPane(table);

        int rowsToDisplay = 3;
        Dimension d = table.getPreferredSize();
        scrollPane.setPreferredSize(
                new Dimension(d.width,table.getRowHeight()*rowsToDisplay+table.getTableHeader().getHeight()));

		return scrollPane;
	} // createListBox
	
	void setUpComboBoxes() {
		// set up comboboxes
		@SuppressWarnings("unchecked")
		Set<String>[] partitionNames = new HashSet[3];
		for (int i = 0; i < 3; i++) {
			partitionNames[i] = new HashSet<>();
		}
		for (int i = 0; i < partitionCount; i++) {
			partitionNames[0].add(((BEASTInterface) likelihoods[i].siteModelInput.get()).getID());
			partitionNames[1].add(likelihoods[i].branchRateModelInput.get().getID());
			partitionNames[2].add(likelihoods[i].treeInput.get().getID());
		}
		String[][] partitionNameStrings = new String[3][];
		for (int i = 0; i < 3; i++) {
			partitionNameStrings[i] = partitionNames[i].toArray(new String[0]);
		}
		for (int j = 0; j < 3; j++) {
			for (int i = 0; i < partitionNameStrings[j].length; i++) {
				partitionNameStrings[j][i] = BeautiDoc.parsePartition(partitionNameStrings[j][i]);
			}
		}
		TableColumn col = table.getColumnModel().getColumn(SITEMODEL_COLUMN);
		JComboBox<String> siteModelComboBox = new JComboBox<>(partitionNameStrings[0]);
		siteModelComboBox.setEditable(true);
		siteModelComboBox.addActionListener(new ComboActionListener(SITEMODEL_COLUMN));

		col.setCellEditor(new DefaultCellEditor(siteModelComboBox));
		// If the cell should appear like a combobox in its
		// non-editing state, also set the combobox renderer
		col.setCellRenderer(new MyComboBoxRenderer(partitionNameStrings[0]));
		col = table.getColumnModel().getColumn(CLOCKMODEL_COLUMN);

		JComboBox<String> clockModelComboBox = new JComboBox<>(partitionNameStrings[1]);
		clockModelComboBox.setEditable(true);
		clockModelComboBox.addActionListener(new ComboActionListener(CLOCKMODEL_COLUMN));

		col.setCellEditor(new DefaultCellEditor(clockModelComboBox));
		col.setCellRenderer(new MyComboBoxRenderer(partitionNameStrings[1]));
		col = table.getColumnModel().getColumn(TREE_COLUMN);

		JComboBox<String> treeComboBox = new JComboBox<>(partitionNameStrings[2]);
		treeComboBox.setEditable(true);
		treeComboBox.addActionListener(new ComboActionListener(TREE_COLUMN));
		col.setCellEditor(new DefaultCellEditor(treeComboBox));
		col.setCellRenderer(new MyComboBoxRenderer(partitionNameStrings[2]));
		col = table.getColumnModel().getColumn(TAXA_COLUMN);
		col.setPreferredWidth(30);
		col = table.getColumnModel().getColumn(SITES_COLUMN);
		col.setPreferredWidth(30);
		
		col = table.getColumnModel().getColumn(USE_AMBIGUITIES_COLUMN);
		JCheckBox checkBox = new JCheckBox();
		checkBox.addActionListener(e -> {
				if (table.getSelectedRow() >= 0 && table.getSelectedColumn() >= 0) {
					Log.warning.println(" " + table.getValueAt(table.getSelectedRow(), table.getSelectedColumn()));
				}
				try {
					int row = table.getSelectedRow();
					if (hasUseAmbiguitiesInput(row)) {
						likelihoods[row].setInputValue("useAmbiguities", checkBox.isSelected());
						tableData[row][USE_AMBIGUITIES_COLUMN] = checkBox.isSelected();
					} else {
						if (checkBox.isSelected()) {
							checkBox.setSelected(false);
						}
					}
				} catch (Exception ex) {
					// TODO: handle exception
				}
		
			});
		col.setCellEditor(new DefaultCellEditor(checkBox));
		col.setCellRenderer(new MyCheckBoxRenderer());
		col.setPreferredWidth(20);
		col.setMaxWidth(20);
	}

	void processPartitionName() {
		Log.warning.println("processPartitionName");
		Log.warning.println(table.getSelectedColumn() + " " + table.getSelectedRow());
		String oldName = tableData[table.getSelectedRow()][table.getSelectedColumn()].toString();
		String newName = nameEditor.getText();
		if (!oldName.equals(newName) && newName.indexOf(".") >= 0) {
			// prevent full stops to be used in IDs
			newName = newName.replaceAll("\\.", "");
			table.setValueAt(newName, table.getSelectedRow(), table.getSelectedColumn());
			table.repaint();
		}
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
					throw new IllegalArgumentException("Cannot rename item in column");
				}
				getDoc().renamePartition(partitionID, oldName, newName);
				table.setValueAt(newName, table.getSelectedRow(), table.getSelectedColumn());
				setUpComboBoxes();
			} catch (Exception e) {
				JOptionPane.showMessageDialog(null, "Renaming failed: " + e.getMessage());
			}
		}
		// debugging code:
		//for (int i = 0; i < partitionCount; i++) {
		//	Log.warning.println(i + " " + tableData[i][0]);
		//}
	}

	class ComboActionListener implements ActionListener {
		int m_nColumn;

		public ComboActionListener(int columnNr) {
			m_nColumn = columnNr;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
//			 SwingUtilities.invokeLater(new Runnable() {
//			 @Override
//			 public void run() {
			Log.warning.println("actionPerformed ");
			Log.warning.println(table.getSelectedRow() + " " + table.getSelectedColumn());
			if (table.getSelectedRow() >= 0 && table.getSelectedColumn() >= 0) {
				Log.warning.println(" " + table.getValueAt(table.getSelectedRow(), table.getSelectedColumn()));
			}
			for (int i = 0; i < partitionCount; i++) {
				try {
					updateModel(m_nColumn, i);
				} catch (Exception ex) {
					Log.warning.println(ex.getMessage());
				}
			}
//		    }});
		}
	}

	public class MyComboBoxRenderer extends JComboBox<String> implements TableCellRenderer {
		private static final long serialVersionUID = 1L;

		public MyComboBoxRenderer(String[] items) {
			super(items);
			setOpaque(true);
		}

		@Override
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

	public class MyCheckBoxRenderer extends JCheckBox implements TableCellRenderer {
		private static final long serialVersionUID = 1L;

		public MyCheckBoxRenderer() {
			super();
			setOpaque(true);
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
				boolean hasFocus, int row, int column) {
			if (hasUseAmbiguitiesInput(row)) {
				if (isSelected) {
					// setForeground(table.getSelectionForeground());
					super.setBackground(table.getSelectionBackground());
				} else {
					setForeground(table.getForeground());
					setBackground(table.getBackground());
				}
				setEnabled(true);
				setSelected((Boolean) value);
			} else {
				setEnabled(false);
			}
			return this;
		}
	}

	@Override
	protected void addSingleItem(BEASTInterface beastObject) {
		initTableData();
		repaint();
	}

	@Override
	protected void addItem() {
		addItem(null);
	} // addItem

	private void addItem(File[] fileArray) {
		List<BEASTInterface> beastObjects = doc.beautiConfig.selectAlignments(doc, this, fileArray);

		// Component c = this;
		if (beastObjects != null) {
			refreshPanel();
		}
	}

	void delItem() {
		int[] selected = getTableRowSelection();
		if (selected.length == 0) {
			JOptionPane.showMessageDialog(this, "Select partitions to delete, before hitting the delete button");
		}
		// do the actual deleting
		for (int i = selected.length - 1; i >= 0; i--) {
			int rowNr = selected[i];
			
			// before deleting, unlink site model, clock model and tree
			
			// check whether any of the models are linked
			BranchRateModel.Base clockModel = likelihoods[rowNr].branchRateModelInput.get();
			SiteModelInterface siteModel = likelihoods[rowNr].siteModelInput.get();
			TreeInterface tree = likelihoods[rowNr].treeInput.get();
			List<GenericTreeLikelihood> cModels = new ArrayList<>();
			List<GenericTreeLikelihood> models = new ArrayList<>();
			List<GenericTreeLikelihood> tModels = new ArrayList<>();
			for (GenericTreeLikelihood likelihood : likelihoods) {
				if (likelihood != likelihoods[rowNr]) {
				if (likelihood.branchRateModelInput.get() == clockModel) {
					cModels.add(likelihood);
				}
				if (likelihood.siteModelInput.get() == siteModel) {
					models.add(likelihood);
				}
				if (likelihood.treeInput.get() == tree) {
					tModels.add(likelihood);
				}
				}
			}
			
			try {
				if (cModels.size() > 0) {
					// clock model is linked, so we need to unlink
					if (doc.getPartitionNr(clockModel) != rowNr) {
						tableData[rowNr][CLOCKMODEL_COLUMN] = getDoc().partitionNames.get(rowNr).partition;
					} else {
						int freePartition = doc.getPartitionNr(cModels.get(0));
						tableData[rowNr][CLOCKMODEL_COLUMN] = getDoc().partitionNames.get(freePartition).partition;
					}
					updateModel(CLOCKMODEL_COLUMN, rowNr);
				}
				
				if (models.size() > 0) {
					// site model is linked, so we need to unlink
					if (doc.getPartitionNr((BEASTInterface) siteModel) != rowNr) {
						tableData[rowNr][SITEMODEL_COLUMN] = getDoc().partitionNames.get(rowNr).partition;
					} else {
						int freePartition = doc.getPartitionNr(models.get(0));
						tableData[rowNr][SITEMODEL_COLUMN] = getDoc().partitionNames.get(freePartition).partition;
					}
					updateModel(SITEMODEL_COLUMN, rowNr);
				}
				
				if (tModels.size() > 0) {
					// tree is linked, so we need to unlink
					if (doc.getPartitionNr((BEASTInterface) tree) != rowNr) {
						tableData[rowNr][TREE_COLUMN] = getDoc().partitionNames.get(rowNr).partition;
					} else {
						int freePartition = doc.getPartitionNr(tModels.get(0));
						tableData[rowNr][TREE_COLUMN] = getDoc().partitionNames.get(freePartition).partition;
					}
					updateModel(TREE_COLUMN, rowNr);
				}
				getDoc().delAlignmentWithSubnet(alignments.get(rowNr));
				alignments.remove(rowNr);
			    // remove deleted likelihood from likelihoods array
				GenericTreeLikelihood[] tmp = new GenericTreeLikelihood[likelihoods.length - 1];
				int k = 0;
				for (int j = 0; j < likelihoods.length; j++) {
					if (j != rowNr) {
						tmp[k] = likelihoods[j];
						k++;
					}
				}
				likelihoods = tmp;
				partitionCount--;
			} catch (Exception e) {
				JOptionPane.showMessageDialog(null, "Deletion failed: " + e.getMessage());
				e.printStackTrace();
			}
		}
		MRCAPriorInputEditor.customConnector(doc);
		refreshPanel();
	} // delItem

	
	void replaceItem() {
		int [] selected = getTableRowSelection();
		if (selected.length != 1) {
			// don't know how to replace multiple alignments at the same time
			// should never get here (button is disabled)
			return;
		}
		Alignment alignment = alignments.get(selected[0]);
		replaceItem(alignment);
	}
	
	private void replaceItem(Alignment alignment) {
		BeautiAlignmentProvider provider = new BeautiAlignmentProvider();
		List<BEASTInterface> list = provider.getAlignments(doc);
		List<Alignment> alignments = new ArrayList<>();
		for (BEASTInterface o : list) {
			if (o instanceof Alignment) {
				alignments.add((Alignment) o);
			}
		}
		Alignment replacement = null;
		if (alignments.size() > 1) {
			JComboBox<Alignment> jcb = new JComboBox<Alignment>(alignments.toArray(new Alignment[]{}));
			JOptionPane.showMessageDialog( null, jcb, "Select a replacement alignment", JOptionPane.QUESTION_MESSAGE);
			replacement = (Alignment) jcb.getSelectedItem();
		} else if (alignments.size() == 1) {
			replacement = alignments.get(0);
		}
		if (replacement != null) {
			if (!replacement.getDataType().getClass().getName().equals(alignment.getDataType().getClass().getName())) {
				JOptionPane.showMessageDialog(null, "Data types do not match, so alignment cannot be replaced: " + 
						replacement.getID() + " " + replacement.getDataType().getClass().getName() + " != " + 
						alignment.getID() + " " + alignment.getDataType().getClass().getName());
				return;
			}
			// replace alignment
			Set<BEASTInterface> outputs = new LinkedHashSet<>();
			outputs.addAll(alignment.getOutputs());
			for (BEASTInterface o : outputs) {
				for (Input<?> input : o.listInputs()) {
					if (input.get() == alignment) {
						input.setValue(replacement, o);
						replacement.getOutputs().add(o);
					} else if (input.get() instanceof List) {
						@SuppressWarnings("rawtypes")
						List inputlist = (List) input.get();
						int i = inputlist.indexOf(alignment);
						if (i >= 0) {
							inputlist.set(i, replacement);
							replacement.getOutputs().add(o);
						}
					}
				}
			}
			int i = doc.alignments.indexOf(alignment);
			doc.alignments.set(i, replacement);
			refreshPanel();
		}
	} // replaceItem
	
	void splitItem() {
		int[] selected = getTableRowSelection();
		if (selected.length == 0) {
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

		for (int i = selected.length - 1; i >= 0; i--) {
			int rowNr = selected[i];
			Alignment alignment = alignments.remove(rowNr);
			getDoc().delAlignmentWithSubnet(alignment);
			try {
				List<Alignment> newAlignments = new ArrayList<>();
				for (int j = 0; j < filters.length; j++) {
					FilteredAlignment f = new FilteredAlignment();
					f.initByName("data", alignment, "filter", filters[j], "dataType", alignment.dataTypeInput.get());
					f.setID(alignment.getID() + ids[j]);
					getDoc().addAlignmentWithSubnet(f, getDoc().beautiConfig.partitionTemplate.get());
					newAlignments.add(f);
				}
				alignments.addAll(newAlignments);
				partitionCount = alignments.size();
				tableData = null; 
				initTableData();			
				if (newAlignments.size() == 2) {
					link(TREE_COLUMN, alignments.size() - 1, alignments.size() - 2);
				} else {
					link(TREE_COLUMN, alignments.size() - 2, alignments.size() - 3);
					tableData = null; 
					initTableData();			
					link(TREE_COLUMN, alignments.size() - 1, alignments.size() - 2);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		refreshPanel();
	} // splitItem

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
		replaceButton.setEnabled(getTableRowSelection().length == 1);
	}
	
} // class AlignmentListInputEditor

