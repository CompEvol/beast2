package beast.app.beauti;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import beast.app.inputeditor.BeautiDoc;
import beast.app.inputeditor.BeautiSubTemplate;
import beast.app.inputeditor.InputEditor;
import beast.app.inputeditor.SmallLabel;
import beast.app.inputeditor.InputEditor.Base;
import beast.app.inputeditor.InputEditor.ExpandOption;
import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beast.base.evolution.tree.Tree;
import beast.base.evolution.tree.TreeDistribution;
import beast.base.parser.PartitionContext;

//import beast.evolution.speciation.BirthDeathGernhard08Model;
//import beast.evolution.speciation.YuleModel;
public class TreeDistributionInputEditor extends InputEditor.Base {

    private static final long serialVersionUID = 1L;

    public TreeDistributionInputEditor(BeautiDoc doc) {
        super(doc);
    }

    @Override
    public Class<?> type() {
        return TreeDistribution.class;
    }
//	@Override
//	public Class<?>[] types() {
//		ArrayList<Class> types = new ArrayList<>();
//		types.add(TreeDistribution.class);
//		types.add(BirthDeathGernhard08Model.class);
//		types.add(YuleModel.class);
//		types.add(Coalescent.class);
//		types.add(BayesianSkyline.class);
//		return types.toArray(new Class[0]);
//	}
    ActionEvent m_e;

    @Override
    public void init(Input<?> input, BEASTInterface beastObject, int listItemNr, ExpandOption isExpandOption, boolean addButtons) {
        m_bAddButtons = addButtons;
        m_input = input;
        m_beastObject = beastObject;
        this.itemNr = listItemNr;

        Box itemBox = Box.createHorizontalBox();

        TreeDistribution distr = (TreeDistribution) beastObject;
        String text = ""/* beastObject.getID() + ": " */;
        if (distr.treeInput.get() != null) {
            text += distr.treeInput.get().getID();
        } else {
            text += distr.treeIntervalsInput.get().treeInput.get().getID();
        }
        JLabel label = new JLabel(text);
        Font font = label.getFont();
        Dimension size = new Dimension(font.getSize() * 200 / 12, font.getSize() * 2);
        label.setMinimumSize(size);
        label.setPreferredSize(size);
        itemBox.add(label);
        // List<String> availableBEASTObjects =
        // PluginPanel.getAvailablePlugins(m_input, m_beastObject, null);

        List<BeautiSubTemplate> availableBEASTObjects = doc.getInputEditorFactory().getAvailableTemplates(m_input, m_beastObject,
                null, doc); 
        // make sure we are dealing with a TreeDistribution
        for (int i = availableBEASTObjects.size() - 1; i >= 0; i--) {
        	BeautiSubTemplate t = availableBEASTObjects.get(i);
        	Class<?> c = t._class;
        	if (!(TreeDistribution.class.isAssignableFrom(c))) {
        		availableBEASTObjects.remove(i);
        	}
        }
        
        JComboBox<BeautiSubTemplate> comboBox = new JComboBox<>(availableBEASTObjects.toArray(new BeautiSubTemplate[]{}));
        comboBox.setName("TreeDistribution");

        for (int i = availableBEASTObjects.size() - 1; i >= 0; i--) {
            if (!TreeDistribution.class.isAssignableFrom(availableBEASTObjects.get(i)._class)) {
                availableBEASTObjects.remove(i);
            }
        }

        String id = distr.getID();
        try {
            // id = BeautiDoc.parsePartition(id);
            id = id.substring(0, id.indexOf('.'));
        } catch (Exception e) {
            throw new RuntimeException("Improperly formatted ID: " + distr.getID());
        }
        for (BeautiSubTemplate template : availableBEASTObjects) {
            if (template.matchesName(id)) { // getMainID().replaceAll(".\\$\\(n\\)",
                // "").equals(id)) {
                comboBox.setSelectedItem(template);
            }
        }

        comboBox.addActionListener(e -> {
                m_e = e;
                SwingUtilities.invokeLater(new Runnable() {
					@Override
                    public void run() {
						@SuppressWarnings("unchecked")
						JComboBox<BeautiSubTemplate> currentComboBox = (JComboBox<BeautiSubTemplate>) m_e.getSource();
                        @SuppressWarnings("unchecked")
						List<BEASTInterface> list = (List<BEASTInterface>) m_input.get();
                        BeautiSubTemplate template = (BeautiSubTemplate) currentComboBox.getSelectedItem();
                        PartitionContext partitionContext = doc.getContextFor(list.get(itemNr));
                        try {
                            template.createSubNet(partitionContext, list, itemNr, true);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                        sync();
                        refreshPanel();
                    }
                });
            });
        itemBox.add(comboBox);
        itemBox.add(Box.createGlue());

        m_validateLabel = new SmallLabel("x", new Color(200, 0, 0));
        m_validateLabel.setVisible(false);
        validateInput();
        itemBox.add(m_validateLabel);
        add(itemBox);
    }

    @Override
    public void validateInput() {
        TreeDistribution distr = (TreeDistribution) m_beastObject;
        // TODO: robustify for the case the tree is not a simple binary tree
        Tree tree = (Tree) distr.treeInput.get();
        if (tree == null) {
            tree = distr.treeIntervalsInput.get().treeInput.get();
        }
        if (tree.hasDateTrait()) {
            if (!distr.canHandleTipDates()) {
                m_validateLabel.setToolTipText("This tree prior cannot handle dated tips. Choose another tree prior.");
                m_validateLabel.m_circleColor = Color.red;
                m_validateLabel.setVisible(true);
                return;
            }
        }

        super.validateInput();
    }
}
