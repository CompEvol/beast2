package beast.app.beauti;

import java.awt.Dimension;
import java.util.Arrays;
import java.util.List;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import beast.app.draw.BEASTObjectDialog;
import beast.app.draw.InputEditor;
import beast.core.BEASTInterface;
import beast.core.Input;
import beast.core.parameter.IntegerParameter;
import beast.core.parameter.RealParameter;
import beast.math.distributions.Prior;

public class PriorInputEditor extends InputEditor.Base {
	private static final long serialVersionUID = 1L;

	public PriorInputEditor(BeautiDoc doc) {
		super(doc);
	}

	@Override
	public Class<?> type() {
		return Prior.class;
	}

	@Override
	public void init(Input<?> input, BEASTInterface plugin, int listItemNr, ExpandOption bExpandOption, boolean bAddButtons) {
        m_bAddButtons = bAddButtons;
        m_input = input;
        m_plugin = plugin;
        this.itemNr= listItemNr;
		
        Box itemBox = Box.createHorizontalBox();

        Prior prior = (Prior) plugin;
        String sText = prior.getParameterName();
        JLabel label = new JLabel(sText);
        label.setMinimumSize(PREFERRED_SIZE);
        label.setPreferredSize(PREFERRED_SIZE);
        itemBox.add(label);

        List<BeautiSubTemplate> sAvailablePlugins = doc.getInputEditorFactory().getAvailableTemplates(prior.distInput, prior, null, doc);
        JComboBox<BeautiSubTemplate> comboBox = new JComboBox<BeautiSubTemplate>(sAvailablePlugins.toArray(new BeautiSubTemplate[]{}));
        comboBox.setName(sText+".distr");

        String sID = prior.distInput.get().getID();
        System.err.println("id=" + sID);
        sID = sID.substring(0, sID.indexOf('.'));
        for (BeautiSubTemplate template : sAvailablePlugins) {
            if (template.sClassInput.get() != null && template.sShortClassName.equals(sID)) {
                comboBox.setSelectedItem(template);
            }
        }
        comboBox.addActionListener(e -> {
            @SuppressWarnings("unchecked")
			JComboBox<BeautiSubTemplate> comboBox1 = (JComboBox<BeautiSubTemplate>) e.getSource();

            List<?> list = (List<?>) m_input.get();

            BeautiSubTemplate template = (BeautiSubTemplate) comboBox1.getSelectedItem();
            //String sID = ((Plugin) list.get(iItem)).getID();
            //String sPartition = BeautiDoc.parsePartition(sID);
            PartitionContext context = doc.getContextFor((BEASTInterface) list.get(itemNr));
            Prior prior1 = (Prior) list.get(itemNr);
            try {
                template.createSubNet(context, prior1, prior1.distInput, true);
            } catch (Exception e1) {
                e1.printStackTrace();
            }

            sync();
            refreshPanel();
        });
        itemBox.add(comboBox);

        if (prior.m_x.get() instanceof RealParameter) {
            // add range button for real parameters
            RealParameter p = (RealParameter) prior.m_x.get();
            JButton rangeButton = new JButton(paramToString(p));
            rangeButton.addActionListener(e -> {
                JButton rangeButton1 = (JButton) e.getSource();

                List<?> list = (List<?>) m_input.get();
                Prior prior1 = (Prior) list.get(itemNr);
                RealParameter p1 = (RealParameter) prior1.m_x.get();
                BEASTObjectDialog dlg = new BEASTObjectDialog(p1, RealParameter.class, doc);
                if (dlg.showDialog()) {
                    dlg.accept(p1, doc);
                    rangeButton1.setText(paramToString(p1));
                    refreshPanel();
                }
            });
            itemBox.add(Box.createHorizontalStrut(10));
            itemBox.add(rangeButton);
        } else if (prior.m_x.get() instanceof IntegerParameter) {
            // add range button for real parameters
            IntegerParameter p = (IntegerParameter) prior.m_x.get();
            JButton rangeButton = new JButton(paramToString(p));
            rangeButton.addActionListener(e -> {
                JButton rangeButton1 = (JButton) e.getSource();

                List<?> list = (List<?>) m_input.get();
                Prior prior1 = (Prior) list.get(itemNr);
                IntegerParameter p1 = (IntegerParameter) prior1.m_x.get();
                BEASTObjectDialog dlg = new BEASTObjectDialog(p1, IntegerParameter.class, doc);
                if (dlg.showDialog()) {
                    dlg.accept(p1, doc);
                    rangeButton1.setText(paramToString(p1));
                    refreshPanel();
                }
            });
            itemBox.add(Box.createHorizontalStrut(10));
            itemBox.add(rangeButton);
        }
        comboBox.setMaximumSize(new Dimension(1024, 24));

        String sTipText = getDoc().tipTextMap.get(plugin.getID());
        //System.out.println(plugin.getID());
        if (sTipText != null) {
            JLabel tipTextLabel = new JLabel(" " + sTipText);
            itemBox.add(tipTextLabel);
        }
        itemBox.add(Box.createGlue());

        add(itemBox);
	}

    String paramToString(RealParameter p) {
        Double lower = p.lowerValueInput.get();
        Double upper = p.upperValueInput.get();
        return "initial = " + Arrays.toString(p.valuesInput.get().toArray()) +
                " [" + (lower == null ? "-\u221E" : lower + "") +
                "," + (upper == null ? "\u221E" : upper + "") + "]";
    }

    String paramToString(IntegerParameter p) {
        Integer lower = p.lowerValueInput.get();
        Integer upper = p.upperValueInput.get();
        return "initial = " + Arrays.toString(p.valuesInput.get().toArray()) +
                " [" + (lower == null ? "-\u221E" : lower + "") +
                "," + (upper == null ? "\u221E" : upper + "") + "]";
    }
}
