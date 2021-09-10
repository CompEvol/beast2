package beast.app.beauti;

import java.awt.Dimension;
import java.awt.Font;
import java.util.Arrays;
import java.util.List;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.text.StyledEditorKit.FontSizeAction;

import beast.app.inputeditor.BEASTObjectDialog;
import beast.app.inputeditor.BeautiDoc;
import beast.app.inputeditor.BeautiSubTemplate;
import beast.app.inputeditor.InputEditor;
import beast.app.inputeditor.InputEditor.Base;
import beast.app.inputeditor.InputEditor.ExpandOption;
import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beast.base.inference.distribution.Prior;
import beast.base.inference.parameter.IntegerParameter;
import beast.base.inference.parameter.RealParameter;
import beast.base.parser.PartitionContext;

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
	public void init(Input<?> input, BEASTInterface beastObject, int listItemNr, ExpandOption isExpandOption, boolean addButtons) {
        m_bAddButtons = addButtons;
        m_input = input;
        m_beastObject = beastObject;
        this.itemNr= listItemNr;
		
        Box itemBox = Box.createHorizontalBox();

        Prior prior = (Prior) beastObject;
        String text = prior.getParameterName();
        JLabel label = new JLabel(text);
        Font font = label.getFont();
        Dimension size = new Dimension(font.getSize() * 200 / 13, font.getSize() * 25/13);
        label.setMinimumSize(size);
        label.setPreferredSize(size);
        itemBox.add(label);

        List<BeautiSubTemplate> availableBEASTObjects = doc.getInputEditorFactory().getAvailableTemplates(prior.distInput, prior, null, doc);
        JComboBox<BeautiSubTemplate> comboBox = new JComboBox<BeautiSubTemplate>(availableBEASTObjects.toArray(new BeautiSubTemplate[]{}));
        comboBox.setName(text+".distr");

        String id = prior.distInput.get().getID();
        //Log.warning.println("id=" + id);
        id = id.substring(0, id.indexOf('.'));
        for (BeautiSubTemplate template : availableBEASTObjects) {
            if (template.classInput.get() != null && template.shortClassName.equals(id)) {
                comboBox.setSelectedItem(template);
            }
        }
        comboBox.addActionListener(e -> {
            @SuppressWarnings("unchecked")
			JComboBox<BeautiSubTemplate> comboBox1 = (JComboBox<BeautiSubTemplate>) e.getSource();

            List<?> list = (List<?>) m_input.get();

            BeautiSubTemplate template = (BeautiSubTemplate) comboBox1.getSelectedItem();
            //String id = ((BEASTObject) list.get(item)).getID();
            //String partition = BeautiDoc.parsePartition(id);
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
        JPanel panel = new JPanel();
        panel.add(comboBox);
        panel.setMaximumSize(size);
        itemBox.add(panel);
        
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
        int fontsize = comboBox.getFont().getSize();
        comboBox.setMaximumSize(new Dimension(1024 * fontsize / 13, 24 * fontsize / 13));

        String tipText = getDoc().tipTextMap.get(beastObject.getID());
        //System.out.println(beastObject.getID());
        if (tipText != null) {
            JLabel tipTextLabel = new JLabel(" " + tipText);
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
