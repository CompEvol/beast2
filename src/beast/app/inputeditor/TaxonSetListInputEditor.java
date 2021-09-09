package beast.app.inputeditor;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.datatransfer.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.regex.PatternSyntaxException;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.*;

import beast.app.inputeditor.BeautiDoc;
import beast.app.inputeditor.ListInputEditor;
import beast.base.BEASTInterface;
import beast.base.BEASTObject;
import beast.base.Input;
import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.Taxon;
import beast.evolution.alignment.TaxonSet;



public class TaxonSetListInputEditor extends ListInputEditor implements TreeModelListener {
	public TaxonSetListInputEditor(BeautiDoc doc) {
		super(doc);
	}

	private static final long serialVersionUID = 1L;
	List<TaxonSet> m_taxonset;
	Map<String,Taxon> m_taxonMap;
	DefaultTreeModel m_treemodel;
	JTree m_tree;
	JTextField filterEntry;
	String m_sFilter = ".*";

	@Override
	public Class<?> type() {
		return List.class;
	}

	@Override
	public Class<?> baseType() {
		return TaxonSet.class;
	}

	@Override
	public void init(Input<?> input, BEASTInterface plugin, int itemNr, ExpandOption bExpand, boolean bAddButtons) {
		this.itemNr = itemNr;
		List<TaxonSet> taxonset = (List<TaxonSet>) input.get();
		add(getContent(taxonset));
	}


	private Component getContent(List<TaxonSet> taxonset) {
		m_taxonset = taxonset;
		m_taxonMap = new HashMap<String, Taxon>();
		for (Taxon taxonset2 : m_taxonset) {
			for (Taxon taxon : ((TaxonSet)taxonset2).taxonsetInput.get()) {
				m_taxonMap.put(taxon.getID(), taxon);
			}
		}

		DefaultMutableTreeNode Node = new DefaultMutableTreeNode("Taxon sets");
		m_treemodel = new DefaultTreeModel(Node);
		m_treemodel.addTreeModelListener(this);

		taxonSetToModel();

		m_tree = new JTree(m_treemodel);
		m_tree.setDragEnabled(true);
		m_tree.setEditable(true);
		// tree.setRootVisible(false);
		m_tree.setDropMode(DropMode.ON_OR_INSERT);
		m_tree.setTransferHandler(new TreeTransferHandler());
		m_tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
		expandTree(m_tree);

		m_tree.setCellRenderer(new DefaultTreeCellRenderer() {
		      public Component getTreeCellRendererComponent(JTree tree,
		          Object value, boolean sel, boolean expanded, boolean leaf,
		          int row, boolean hasFocus) {
			        super.getTreeCellRendererComponent(tree, value, sel, expanded,
				            leaf, row, hasFocus);
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
		        if (node != m_treemodel.getRoot() &&
		        		node.getParent() != m_treemodel.getRoot() && !node.toString().matches(m_sFilter)) {
		          setForeground(Color.lightGray);
		        }
		        return this;
		      }
		    });

		m_tree.setCellEditor(new DefaultTreeCellEditor(m_tree, (DefaultTreeCellRenderer) m_tree.getCellRenderer()) {
			public boolean isCellEditable(EventObject event) {
				boolean returnValue = super.isCellEditable(event);
				if (returnValue) {
					// don't edit if it is not a child
					Object node = tree.getLastSelectedPathComponent();
					if ((node != null) && (node instanceof TreeNode)) {
						TreeNode treeNode = (TreeNode) node;
						returnValue = treeNode.getParent() == m_treemodel.getRoot();
						//!treeNode.isLeaf() && treeNode.getParent() != null;
					}
				}
				return returnValue;
			}

		});

		JScrollPane pane = new JScrollPane(m_tree);

		Box box = Box.createVerticalBox();
		box.add(createFilterBox());
		box.add(pane);
		box.add(createButtonBox());
		return box;
	}

	private Component createFilterBox() {
		Box filterBox = Box.createHorizontalBox();
		filterBox.add(new JLabel("filter: "));
		Dimension size = new Dimension(100,20);
		filterEntry = new JTextField();
		filterEntry.setMinimumSize(size);
		filterEntry.setPreferredSize(size);
		filterEntry.setSize(size);
		filterEntry.setToolTipText("Enter regular expression to match taxa");
		filterEntry.setMaximumSize(new Dimension(1024, 20));
		filterBox.add(filterEntry);
		filterEntry.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void removeUpdate(DocumentEvent e) {
				processFilter();
			}
			@Override
			public void insertUpdate(DocumentEvent e) {
				processFilter();
			}
			@Override
			public void changedUpdate(DocumentEvent e) {
				processFilter();
			}
			private void processFilter() {
				String sFilter = ".*" + filterEntry.getText() + ".*";
				try {
					// sanity check: make sure the filter is legit
					sFilter.matches(sFilter);
					m_sFilter = sFilter;
					m_tree.repaint();
				} catch (PatternSyntaxException e) {
					// ignore
				}
			}
		});
		return filterBox;
	}

	/** for adding and deleting taxon sets **/
	private Box createButtonBox() {
		Box buttonBox = Box.createHorizontalBox();

		JButton delButton = new JButton("Delete");
		delButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int[] selRows = m_tree.getSelectionRows();
				if (selRows.length == 0) {
					return;
				}
				TreePath path = m_tree.getPathForRow(selRows[0]);
				DefaultMutableTreeNode firstNode = (DefaultMutableTreeNode) path.getLastPathComponent();
				if (firstNode.getChildCount() > 0) {
					JOptionPane.showMessageDialog(m_tree, "Cannot delete " + firstNode.toString() + ":there are still children left");
					return;
				}
				if (firstNode.getParent() == null) {
					JOptionPane.showMessageDialog(m_tree, "Cannot delete root");
					return;
				}
				if (firstNode.getParent().getParent() != null) {
					JOptionPane.showMessageDialog(m_tree, "Cannot delete taxon");
					return;
				}
				m_treemodel.removeNodeFromParent(firstNode);
				modelToTaxonset();
			}
		});
		buttonBox.add(Box.createHorizontalGlue());
		buttonBox.add(delButton);
		buttonBox.add(Box.createHorizontalGlue());


		JButton addButton = new JButton("New");
		addButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				DefaultMutableTreeNode root = (DefaultMutableTreeNode) m_treemodel.getRoot();
				DefaultMutableTreeNode Kid = new DefaultMutableTreeNode("New taxonset");
				m_treemodel.insertNodeInto(Kid, root, m_taxonset.size());
				modelToTaxonset();
			}
		});
		buttonBox.add(addButton);
		buttonBox.add(Box.createHorizontalGlue());
		return buttonBox;
	}

	/** for convert taxon sets to table model **/
	private void taxonSetToModel() {
		List<TaxonSet> taxonsets = m_taxonset;
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) m_treemodel.getRoot();
		for (int i = root.getChildCount()-1; i >= 0 ; i--) {
			DefaultMutableTreeNode child = (DefaultMutableTreeNode) root.getChildAt(i);
			m_treemodel.removeNodeFromParent(child);
		}

		for (int i = 0; i < taxonsets.size(); i++) {
			DefaultMutableTreeNode Kid = new DefaultMutableTreeNode(taxonsets.get(i).getID());
			m_treemodel.insertNodeInto(Kid, root, i);
			List<Taxon> taxa = ((TaxonSet) taxonsets.get(i)).taxonsetInput.get();
			for (int j = 0; j < taxa.size(); j++) {
				DefaultMutableTreeNode GKid = new DefaultMutableTreeNode(taxa.get(j).getID());
				GKid.setAllowsChildren(false);
				m_treemodel.insertNodeInto(GKid, Kid, j);
			}
		}
	}

	/** for convert table model to taxon sets **/
	private void modelToTaxonset() {
		List<TaxonSet> taxonsets = m_taxonset;
		taxonsets.clear();

		DefaultMutableTreeNode root = (DefaultMutableTreeNode) m_treemodel.getRoot();
		for (int i = 0; i < root.getChildCount(); i++) {
			DefaultMutableTreeNode child = (DefaultMutableTreeNode) root.getChildAt(i);
			TaxonSet taxonset = new TaxonSet();
			taxonset.setID(child.toString());
			for (int j = 0; j < child.getChildCount(); j++) {
				DefaultMutableTreeNode gchild = (DefaultMutableTreeNode) child.getChildAt(j);
				Taxon taxon = m_taxonMap.get(gchild.toString());
				try {
					taxonset.taxonsetInput.setValue(taxon, taxonset);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			taxonsets.add(taxonset);
		}
		//System.err.println(new XMLProducer().toXML(m_taxonset));

	}


	private void expandTree(JTree tree) {
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
		Enumeration<?> e = root.breadthFirstEnumeration();
		while (e.hasMoreElements()) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
			if (node.isLeaf())
				continue;
			int row = tree.getRowForPath(new TreePath(node.getPath()));
			tree.expandRow(row);
		}
	}

//	public static void main(String[] args) {
//		JFrame f = new JFrame();
//		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//
//		TaxonSet taxonset1 = newTaxonSet("animal", newTaxon("first"), newTaxon("second"), newTaxon("third"));
//		TaxonSet taxonset2 = newTaxonSet("plant", newTaxon("firstA"), newTaxon("secondA"), newTaxon("thirdA"));
//		TaxonSet taxonset3 = newTaxonSet("bacteria", newTaxon("firstB"), newTaxon("secondB"), newTaxon("thirdB"));
//		TaxonSet taxonset = newTaxonSet("top", taxonset1, taxonset2, taxonset3);
//
//		f.add(new DataInputEditor().getContent(taxonset));
//		f.setSize(400, 400);
//		f.setLocation(200, 200);
//		f.setVisible(true);
//	}

	private static TaxonSet newTaxonSet(String sID, Taxon newTaxon, Taxon newTaxon2, Taxon newTaxon3) {
		TaxonSet taxonset = new TaxonSet();
		taxonset.setID(sID);
		try {
			taxonset.taxonsetInput.setValue(newTaxon, taxonset);
			taxonset.taxonsetInput.setValue(newTaxon2, taxonset);
			taxonset.taxonsetInput.setValue(newTaxon3, taxonset);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return taxonset;
	}

	private static Taxon newTaxon(String sID) {
		Taxon taxon = new Taxon();
		taxon.setID(sID);
		return taxon;
	}

	public class TreeTransferHandler extends TransferHandler {
		private static final long serialVersionUID = 1L;

		DataFlavor nodesFlavor;
		DataFlavor[] flavors = new DataFlavor[1];
		DefaultMutableTreeNode[] nodesToRemove;

		public TreeTransferHandler() {
			try {
				String mimeType = DataFlavor.javaJVMLocalObjectMimeType + ";class=\""
						+ javax.swing.tree.DefaultMutableTreeNode[].class.getName() + "\"";
				nodesFlavor = new DataFlavor(mimeType);
				flavors[0] = nodesFlavor;
			} catch (ClassNotFoundException e) {
				System.out.println("ClassNotFound: " + e.getMessage());
			}
		}



		public boolean canImport(TransferHandler.TransferSupport support) {
			if (!support.isDrop()) {
				return false;
			}
			support.setShowDropLocation(true);
			if (!support.isDataFlavorSupported(nodesFlavor)) {
				return false;
			}
			// Do not allow a drop on the drag source selections.
			JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
			JTree tree = (JTree) support.getComponent();
			int dropRow = tree.getRowForPath(dl.getPath());
			int[] selRows = tree.getSelectionRows();
			for (int i = 0; i < selRows.length; i++) {
				if (selRows[i] == dropRow) {
					return false;
				}
			}
			// Do not allow MOVE-action drops if a non-leaf node is
			// selected unless all of its children are also selected.
			int action = support.getDropAction();
			if (action == MOVE) {
				return haveCompleteNode(tree);
			}
			// Do not allow a non-leaf node to be copied to a level
			// which is less than its source level.
			TreePath dest = dl.getPath();
			DefaultMutableTreeNode target = (DefaultMutableTreeNode) dest.getLastPathComponent();
			TreePath path = tree.getPathForRow(selRows[0]);
			DefaultMutableTreeNode firstNode = (DefaultMutableTreeNode) path.getLastPathComponent();
			if (firstNode.getChildCount() > 0 && target.getLevel() < firstNode.getLevel()) {
				return false;
			}
			return true;
		}

		private boolean haveCompleteNode(JTree tree) {
			int[] selRows = tree.getSelectionRows();
			TreePath path = tree.getPathForRow(selRows[0]);
			DefaultMutableTreeNode first = (DefaultMutableTreeNode) path.getLastPathComponent();
			int childCount = first.getChildCount();
			// first has children and no children are selected.
			if (childCount > 0 && selRows.length == 1)
				return false;
			// first may have children.
			for (int i = 1; i < selRows.length; i++) {
				path = tree.getPathForRow(selRows[i]);
				DefaultMutableTreeNode next = (DefaultMutableTreeNode) path.getLastPathComponent();
				if (first.isNodeChild(next)) {
					// Found a child of first.
					if (childCount > selRows.length - 1) {
						// Not all children of first are selected.
						return false;
					}
				}
			}
			return true;
		}

		protected Transferable createTransferable(JComponent c) {
			JTree tree = (JTree) c;
			TreePath[] paths = tree.getSelectionPaths();
			if (paths != null) {
				// Make up a node array of copies for transfer and
				// another for/of the nodes that will be removed in
				// exportDone after a successful drop.
				List<DefaultMutableTreeNode> copies = new ArrayList<DefaultMutableTreeNode>();
				List<DefaultMutableTreeNode> toRemove = new ArrayList<DefaultMutableTreeNode>();
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) paths[0].getLastPathComponent();
				DefaultMutableTreeNode copy = copy(node);
				copies.add(copy);
				toRemove.add(node);
				for (int i = 1; i < paths.length; i++) {
					DefaultMutableTreeNode next = (DefaultMutableTreeNode) paths[i].getLastPathComponent();
					// Do not allow higher level nodes to be added to list.
					if (next.getLevel() < node.getLevel()) {
						break;
					} else if (next.getLevel() > node.getLevel()) { // child
																	// node
						copy.add(copy(next));
						// node already contains child
					} else { // sibling
						copies.add(copy(next));
						toRemove.add(next);
					}
				}
				DefaultMutableTreeNode[] nodes = copies.toArray(new DefaultMutableTreeNode[copies.size()]);
				nodesToRemove = toRemove.toArray(new DefaultMutableTreeNode[toRemove.size()]);
				return new NodesTransferable(nodes);
			}
			return null;
		}

		/** Defensive copy used in createTransferable. */
		private DefaultMutableTreeNode copy(TreeNode node) {
			return new DefaultMutableTreeNode(node);
		}

		protected void exportDone(JComponent source, Transferable data, int action) {
			if ((action & MOVE) == MOVE) {
				JTree tree = (JTree) source;
				DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
				// Remove nodes saved in nodesToRemove in createTransferable.
				for (int i = 0; i < nodesToRemove.length; i++) {
					model.removeNodeFromParent(nodesToRemove[i]);
				}
			}
			modelToTaxonset();
		}


		public int getSourceActions(JComponent c) {
			return COPY_OR_MOVE;
		}

		public boolean importData(TransferHandler.TransferSupport support) {
			if (!canImport(support)) {
				return false;
			}
			// Extract transfer data.
			DefaultMutableTreeNode[] nodes = null;
			try {
				Transferable t = support.getTransferable();
				nodes = (DefaultMutableTreeNode[]) t.getTransferData(nodesFlavor);
			} catch (UnsupportedFlavorException ufe) {
				System.out.println("UnsupportedFlavor: " + ufe.getMessage());
			} catch (java.io.IOException ioe) {
				System.out.println("I/O error: " + ioe.getMessage());
			}
			// Get drop location info.
			JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
			int childIndex = dl.getChildIndex();
			TreePath dest = dl.getPath();

			DefaultMutableTreeNode parent = (DefaultMutableTreeNode) dest.getLastPathComponent();
			JTree tree = (JTree) support.getComponent();
			DefaultTreeModel model = (DefaultTreeModel) tree.getModel();

			// only drop into a first level entry
			DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
			if (parent == root) {
				for (int i = 0; i < nodes.length; i++) {
					if (nodes[i].getParent() != root) {
						return false;
					}
				}
			} else {
				if (parent.getParent() != root) {
					parent = (DefaultMutableTreeNode) parent.getParent();
				}
				// only drop second level entries
				for (int i = 0; i < nodes.length; i++) {
					if (nodes[i].getParent() == root) {
						return false;
					}
				}
			}
			// Configure for drop mode.
			int index = childIndex; // DropMode.INSERT
			if (childIndex == -1) { // DropMode.ON
				index = parent.getChildCount();
			}
			// Add data to model.
			for (int i = 0; i < nodes.length; i++) {
				model.insertNodeInto(nodes[i], parent, index++);
			}

			return true;
		}


		public String toString() {
			return getClass().getName();
		}

		public class NodesTransferable implements Transferable {
			DefaultMutableTreeNode[] nodes;

			public NodesTransferable(DefaultMutableTreeNode[] nodes) {
				this.nodes = nodes;
			}

			public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
				if (!isDataFlavorSupported(flavor))
					throw new UnsupportedFlavorException(flavor);
				return nodes;
			}

			public DataFlavor[] getTransferDataFlavors() {
				return flavors;
			}

			public boolean isDataFlavorSupported(DataFlavor flavor) {
				return nodesFlavor.equals(flavor);
			}
		}
	}

	@Override
	public void treeNodesChanged(TreeModelEvent e) {
		TreePath tp = e.getTreePath();
		Object[] children = e.getChildren();
		DefaultMutableTreeNode changedNode;
		if (children != null)
			changedNode = (DefaultMutableTreeNode) children[0];
		else
			changedNode = (DefaultMutableTreeNode) tp.getLastPathComponent();

		System.out.println("Model change path: " + tp + "New data: " + changedNode.getUserObject());
		modelToTaxonset();
	}

	@Override
	public void treeNodesInserted(TreeModelEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void treeNodesRemoved(TreeModelEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void treeStructureChanged(TreeModelEvent e) {
		// TODO Auto-generated method stub

	}


}
