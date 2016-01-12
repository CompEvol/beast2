package beast.app.beauti;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.CellEditorListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import beast.app.draw.ListInputEditor;
import beast.app.draw.SmallButton;
import beast.app.util.FileDrop;
import beast.core.Input;
import beast.core.MCMC;
import beast.core.BEASTInterface;
import beast.core.Input.Validate;
import beast.core.State;
import beast.core.StateNode;
import beast.core.util.CompoundDistribution;
import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.FilteredAlignment;
import beast.evolution.alignment.Taxon;
import beast.evolution.branchratemodel.BranchRateModel;
import beast.evolution.likelihood.GenericTreeLikelihood;
import beast.evolution.sitemodel.SiteModel;
import beast.evolution.sitemodel.SiteModelInterface;
import beast.evolution.tree.TreeInterface;

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
	int nPartitions;
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

	@SuppressWarnings("unchecked")
	public void init(Input<?> input, BEASTInterface plugin, int itemNr, ExpandOption bExpandOption, boolean bAddButtons) {
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
            public void filesDropped(java.io.File[] files) {
                addFiles(files);
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
            if (doc.bHasLinkedAtLeastOnce) {
                JOptionPane.showMessageDialog(null, "Cannot delete partition while parameters are linked");
                return;
            }
            delItem();
        });
        buttonBox.add(delButton);
        buttonBox.add(Box.createHorizontalStrut(STRUT_SIZE));

        splitButton = new JButton("Split");
        splitButton.setName("Split");
        splitButton.setToolTipText("Split alignment into partitions, for example, codon positions");
        splitButton.addActionListener(e -> splitItem());
        buttonBox.add(splitButton);

        buttonBox.add(Box.createHorizontalGlue());

        return buttonBox;
    }

    private void addFiles(File[] fileArray) {
        List<BEASTInterface> plugins = null;

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
                return;
            }
        }

        plugins = selectedProvider.getAlignments(doc, fileArray);

        // create taxon sets, if any
        if (plugins != null) {
	        for (BEASTInterface o : plugins) {
	        	if (o instanceof Alignment) {
	        		try {
						BeautiDoc.createTaxonSet((Alignment) o, doc);
					} catch (Exception e) {
						e.printStackTrace();
					}
	        	}
	        }
        }

        // Component c = this;
        if (plugins != null) {
            refreshPanel();
        }
    }



	/**
     * This method just adds the two buttons (with add()) and does not add any glue or struts before or after.
     * @param box
     * @param sLabel
     */
	private void addLinkUnlinkPair(Box box, String sLabel) {

        //JLabel label = new JLabel(sLabel+":");
        //box.add(label);
        JButton linkSModelButton = new JButton("Link " + sLabel);
		linkSModelButton.setName("Link " + sLabel);
		linkSModelButton.addActionListener(e -> {
            JButton button = (JButton) e.getSource();
            link(columnLabelToNr(button.getText()));
            table.repaint();
        });
		box.add(linkSModelButton);
		linkSModelButton.setEnabled(!getDoc().bHasLinkedAtLeastOnce);
		JButton unlinkSModelButton = new JButton("Unlink " + sLabel);
		unlinkSModelButton.setName("Unlink " + sLabel);
		unlinkSModelButton.addActionListener(e -> {
            JButton button = (JButton) e.getSource();
            unlink(columnLabelToNr(button.getText()));
            table.repaint();
        });
		box.add(unlinkSModelButton);
		unlinkSModelButton.setEnabled(!getDoc().bHasLinkedAtLeastOnce);

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
        return table.getSelectedRows();
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
					String sID = ((BEASTInterface)likelihoods[iRow].siteModelInput.get()).getID();
					oldName = BeautiDoc.parsePartition(sID);
					doc.renamePartition(BeautiDoc.SITEMODEL_PARTITION, oldName, sPartition);
					isRenaming = true;
				}
				break;
			case CLOCKMODEL_COLUMN: {
				String sID = likelihoods[iRow].branchRateModelInput.get().getID();
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
					String sID = likelihoods[iRow].treeInput.get().getID();
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
		GenericTreeLikelihood treeLikelihood = null;
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
			SiteModelInterface siteModel = null;
			if (treeLikelihood != null) { // getDoc().getPartitionNr(sPartition,
											// BeautiDoc.SITEMODEL_PARTITION) !=
											// iRow) {
				siteModel = treeLikelihood.siteModelInput.get();
			} else {
				siteModel = (SiteModel) doc.pluginmap.get("SiteModel.s:" + sPartition);
				if (siteModel != likelihoods[iRow].siteModelInput.get()) {
					PartitionContext context = getPartitionContext(iRow);
					siteModel = (SiteModel.Base) BeautiDoc.deepCopyPlugin((BEASTInterface) likelihoods[iRow].siteModelInput.get(),
							likelihoods[iRow], (MCMC) doc.mcmc.get(), oldContext, context, doc, null);
				}
			}
			SiteModelInterface target = this.likelihoods[iRow].siteModelInput.get();
			if (target instanceof SiteModel.Base && siteModel instanceof SiteModel.Base) {
				if (!((SiteModel.Base)target).substModelInput.canSetValue(((SiteModel.Base)siteModel).substModelInput.get(), (SiteModel.Base) target)) {
					throw new Exception("Cannot link site model: substitution models are incompatible");
				}
			} else {
				throw new Exception("Don't know how to link this site model");
			}
			needsRePartition = (this.likelihoods[iRow].siteModelInput.get() != siteModel);
			this.likelihoods[iRow].siteModelInput.setValue(siteModel, this.likelihoods[iRow]);

			sPartition = ((BEASTInterface)likelihoods[iRow].siteModelInput.get()).getID();
			sPartition = BeautiDoc.parsePartition(sPartition);
			getDoc().setCurrentPartition(BeautiDoc.SITEMODEL_PARTITION, iRow, sPartition);
		}
			break;
		case CLOCKMODEL_COLUMN: {
			BranchRateModel clockModel = null;
			if (treeLikelihood != null) { // getDoc().getPartitionNr(sPartition,
											// BeautiDoc.CLOCKMODEL_PARTITION)
											// != iRow) {
				clockModel = treeLikelihood.branchRateModelInput.get();
			} else {
				clockModel = getDoc().getClockModel(sPartition);
				if (clockModel != likelihoods[iRow].branchRateModelInput.get()) {
					PartitionContext context = getPartitionContext(iRow);
					clockModel = (BranchRateModel) BeautiDoc.deepCopyPlugin(likelihoods[iRow].branchRateModelInput.get(),
							likelihoods[iRow], (MCMC) doc.mcmc.get(), oldContext, context, doc, null);

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
			if (tree != null && tree != this.likelihoods[iRow].treeInput.get()) {
				throw new Exception("Cannot link clock model with different trees");
			}

			needsRePartition = (this.likelihoods[iRow].branchRateModelInput.get() != clockModel);
			this.likelihoods[iRow].branchRateModelInput.setValue(clockModel, this.likelihoods[iRow]);
			sPartition = likelihoods[iRow].branchRateModelInput.get().getID();
			sPartition = BeautiDoc.parsePartition(sPartition);
			getDoc().setCurrentPartition(BeautiDoc.CLOCKMODEL_PARTITION, iRow, sPartition);
		}
			break;
		case TREE_COLUMN: {
			TreeInterface tree = null;
			if (treeLikelihood != null) { // getDoc().getPartitionNr(sPartition,
											// BeautiDoc.TREEMODEL_PARTITION) !=
											// iRow) {
				tree = treeLikelihood.treeInput.get();
			} else {
				tree = (TreeInterface) doc.pluginmap.get("Tree.t:" + sPartition);
				if (tree != likelihoods[iRow].treeInput.get()) {
					PartitionContext context = getPartitionContext(iRow);
					tree = (TreeInterface) BeautiDoc.deepCopyPlugin((BEASTInterface) likelihoods[iRow].treeInput.get(), likelihoods[iRow],
							(MCMC) doc.mcmc.get(), oldContext, context, doc, null);

					
					State state = ((MCMC) doc.mcmc.get()).startStateInput.get();
					List<StateNode> stateNodes = new ArrayList<>();
					stateNodes.addAll(state.stateNodeInput.get());
					for (StateNode s : stateNodes) {
						if (s.getID().endsWith(".t:" + oldContext.tree) && !(s instanceof TreeInterface)) {
							StateNode copy = (StateNode) BeautiDoc.deepCopyPlugin(s, likelihoods[iRow], (MCMC) doc.mcmc.get(), oldContext, context, doc, null);
						}
					}
				}
			}
			// sanity check: make sure taxon sets are compatible
			Taxon.assertSameTaxa(tree.getID(), tree.getTaxonset().getTaxaNames(),
					likelihoods[iRow].dataInput.get().getID(), likelihoods[iRow].dataInput.get().getTaxaNames());

			needsRePartition = (this.likelihoods[iRow].treeInput.get() != tree);
System.err.println("needsRePartition = " + needsRePartition);			
			if (needsRePartition) {
				TreeInterface oldTree = this.likelihoods[iRow].treeInput.get();
				List<TreeInterface> tModels = new ArrayList<TreeInterface>();
				for (GenericTreeLikelihood likelihood : likelihoods) {
					if (likelihood.treeInput.get() == oldTree) {
						tModels.add(likelihood.treeInput.get());
					}
				}
				if (tModels.size() == 1) {
					// remove old tree from model
					((BEASTInterface)oldTree).setInputValue("estimate", false);
                	// use toArray to prevent ConcurrentModificationException
					for (Object plugin : BEASTInterface.getOutputs(oldTree).toArray()) { //.toArray(new BEASTInterface[0])) {
						for (Input<?> input : ((BEASTInterface)plugin).listInputs()) {
							try {
							if (input.get() == oldTree) {
								if (input.getRule() != Input.Validate.REQUIRED) {
									input.setValue(tree/*null*/, (BEASTInterface) plugin);
								//} else {
									//input.setValue(tree, (BEASTInterface) plugin);
								}
							} else if (input.get() instanceof List) {
								List list = (List) input.get();
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
			likelihoods[iRow].treeInput.setValue(tree, likelihoods[iRow]);
			// TreeDistribution d = getDoc().getTreePrior(sPartition);
			// CompoundDistribution prior = (CompoundDistribution)
			// doc.pluginmap.get("prior");
			// if (!getDoc().posteriorPredecessors.contains(d)) {
			// prior.pDistributions.setValue(d, prior);
			// }
			sPartition = likelihoods[iRow].treeInput.get().getID();
			sPartition = BeautiDoc.parsePartition(sPartition);
			getDoc().setCurrentPartition(BeautiDoc.TREEMODEL_PARTITION, iRow, sPartition);
		}
		}
		tableData[iRow][nColumn] = sPartition;
		if (needsRePartition) {
			List<BeautiSubTemplate> templates = new ArrayList<BeautiSubTemplate>();
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
		this.likelihoods = new GenericTreeLikelihood[nPartitions];
		if (tableData == null) {
			tableData = new Object[nPartitions][NR_OF_COLUMNS];
		}
		CompoundDistribution likelihoods = (CompoundDistribution) doc.pluginmap.get("likelihood");

		for (int i = 0; i < nPartitions; i++) {
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
		BEASTInterface plugin = (BEASTInterface) input.get();
		String sID = plugin.getID();
		String sPartition = BeautiDoc.parsePartition(sID);
		return sPartition;
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
			    JComponent jcomp = (JComponent)comp;
			    if (comp == jcomp) {
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

        int rowsToDisplay = 3;
        Dimension d = table.getPreferredSize();
        scrollPane.setPreferredSize(
                new Dimension(d.width,table.getRowHeight()*rowsToDisplay+table.getTableHeader().getHeight()));

		return scrollPane;
	} // createListBox
	
	void setUpComboBoxes() {
		// set up comboboxes
		Set<String>[] partitionNames = new HashSet[3];
		for (int i = 0; i < 3; i++) {
			partitionNames[i] = new HashSet<String>();
		}
		for (int i = 0; i < nPartitions; i++) {
			partitionNames[0].add(((BEASTInterface) likelihoods[i].siteModelInput.get()).getID());
			partitionNames[1].add(likelihoods[i].branchRateModelInput.get().getID());
			partitionNames[2].add(likelihoods[i].treeInput.get().getID());
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
		
		col = table.getColumnModel().getColumn(USE_AMBIGUITIES_COLUMN);
		JCheckBox checkBox = new JCheckBox();
		checkBox.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				JCheckBox checkBox = (JCheckBox) e.getSource();
				if (table.getSelectedRow() >= 0 && table.getSelectedColumn() >= 0) {
					System.err.println(" " + table.getValueAt(table.getSelectedRow(), table.getSelectedColumn()));
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
		
			}
		});
		col.setCellEditor(new DefaultCellEditor(checkBox));
		col.setCellRenderer(new MyCheckBoxRenderer());
		col.setPreferredWidth(20);
		col.setMaxWidth(20);
	}

	void processPartitionName() {
		System.err.println("processPartitionName");
		System.err.println(table.getSelectedColumn() + " " + table.getSelectedRow());
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

	public class MyCheckBoxRenderer extends JCheckBox implements TableCellRenderer {
		private static final long serialVersionUID = 1L;

		public MyCheckBoxRenderer() {
			super();
			setOpaque(true);
		}

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
	protected void addSingleItem(BEASTInterface plugin) {
		initTableData();
		repaint();
	}

	@Override
	protected void addItem() {
		List<BEASTInterface> plugins = doc.beautiConfig.selectAlignments(doc, this);

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
			BranchRateModel.Base clockModel = likelihoods[iRow].branchRateModelInput.get();
			SiteModelInterface siteModel = likelihoods[iRow].siteModelInput.get();
			TreeInterface tree = likelihoods[iRow].treeInput.get();
			List<GenericTreeLikelihood> cModels = new ArrayList<GenericTreeLikelihood>();
			List<GenericTreeLikelihood> sModels = new ArrayList<GenericTreeLikelihood>();
			List<GenericTreeLikelihood> tModels = new ArrayList<GenericTreeLikelihood>();
			for (GenericTreeLikelihood likelihood : likelihoods) {
				if (likelihood != likelihoods[iRow]) {
				if (likelihood.branchRateModelInput.get() == clockModel) {
					cModels.add(likelihood);
				}
				if (likelihood.siteModelInput.get() == siteModel) {
					sModels.add(likelihood);
				}
				if (likelihood.treeInput.get() == tree) {
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
					if (doc.getPartitionNr((BEASTInterface) siteModel) != iRow) {
						tableData[iRow][SITEMODEL_COLUMN] = getDoc().sPartitionNames.get(iRow).partition;
					} else {
						int iFreePartition = doc.getPartitionNr(sModels.get(0));
						tableData[iRow][SITEMODEL_COLUMN] = getDoc().sPartitionNames.get(iFreePartition).partition;
					}
					updateModel(SITEMODEL_COLUMN, iRow);
				}
				
				if (tModels.size() > 0) {
					// tree is linked, so we need to unlink
					if (doc.getPartitionNr((BEASTInterface) tree) != iRow) {
						tableData[iRow][TREE_COLUMN] = getDoc().sPartitionNames.get(iRow).partition;
					} else {
						int iFreePartition = doc.getPartitionNr(tModels.get(0));
						tableData[iRow][TREE_COLUMN] = getDoc().sPartitionNames.get(iFreePartition).partition;
					}
					updateModel(TREE_COLUMN, iRow);
				}
				getDoc().delAlignmentWithSubnet(alignments.get(iRow));
				alignments.remove(iRow);
			    // remove deleted likelihood from likelihoods array
				GenericTreeLikelihood[] tmp = new GenericTreeLikelihood[likelihoods.length - 1];
				int k = 0;
				for (int j = 0; j < likelihoods.length; j++) {
					if (j != iRow) {
						tmp[k] = likelihoods[j];
						k++;
					}
				}
				likelihoods = tmp;
				nPartitions--;
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
					f.initByName("data", alignment, "filter", filters[j], "dataType", alignment.dataTypeInput.get());
					f.setID(alignment.getID() + ids[j]);
					getDoc().addAlignmentWithSubnet(f, getDoc().beautiConfig.partitionTemplate.get());
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
	}
	
} // class AlignmentListInputEditor

