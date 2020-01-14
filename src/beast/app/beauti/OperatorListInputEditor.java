package beast.app.beauti;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import beast.app.draw.BEASTObjectInputEditor;
import beast.app.draw.InputEditor;
import beast.app.draw.InputEditorFactory;
import beast.app.draw.ListInputEditor;
import beast.core.BEASTInterface;
import beast.core.Input;
import beast.core.Operator;
import beast.core.StateNode;



public class OperatorListInputEditor extends ListInputEditor {
    private static final long serialVersionUID = 1L;
    List<JTextField> textFields = new ArrayList<>();
    List<Operator> operators = new ArrayList<>();

	public OperatorListInputEditor(BeautiDoc doc) {
		super(doc);
	}

    @Override
    public Class<?> type() {
        return List.class;
    }

    @Override
    public Class<?> baseType() {
        return Operator.class;
    }

    @Override
    public void init(Input<?> input, BEASTInterface beastObject, int itemNr, ExpandOption isExpandOption, boolean addButtons) {
    	Box box = Box.createHorizontalBox();
    	box.add(Box.createHorizontalStrut(25));
    	box.add(new JLabel("Operator"));
    	box.add(Box.createGlue());
    	box.add(new JLabel("Weight"));
    	box.add(Box.createHorizontalStrut(20));
    	add(box);
    	

    	m_buttonStatus = ButtonStatus.NONE;
    	super.init(input, beastObject, itemNr, isExpandOption, addButtons);
    	
    	BEASTObjectInputEditor osEditor = new BEASTObjectInputEditor(doc);
    	osEditor.init(((BEASTInterface) doc.mcmc.get()).getInput("operatorschedule"), (BEASTInterface) doc.mcmc.get(), -1, isExpandOption, addButtons);
    	add(osEditor);
    }
    
    @Override
    protected InputEditor addPluginItem(Box itemBox, BEASTInterface beastObject) {
        Operator operator = (Operator) beastObject;

        JTextField entry = new JTextField(" " + getLabel(operator));
        entry.setMinimumSize(new Dimension(200, 16));
        //entry.setMaximumSize(new Dimension(200, 20));
        m_entries.add(entry);
        entry.setBackground(getBackground());
        entry.setBorder(null);
        itemBox.add(Box.createRigidArea(new Dimension(5, 1)));
        itemBox.add(entry);
        entry.setEditable(false);

//        JLabel label = new JLabel(getLabel(operator));
//        label.setBackground(Color.WHITE);
//        m_labels.add(label);
//        m_entries.add(null);
//        itemBox.add(label);


        itemBox.add(Box.createHorizontalGlue());
        JTextField weightEntry = new JTextField();
        weightEntry.setToolTipText(operator.m_pWeight.getHTMLTipText());
        weightEntry.setText(operator.m_pWeight.get() + "");
        weightEntry.getDocument().addDocumentListener(new OperatorDocumentListener(operator, weightEntry));
        Dimension size = new Dimension(50, 25);
        weightEntry.setMinimumSize(size);
        weightEntry.setPreferredSize(size);
        int fontsize = weightEntry.getFont().getSize();
        weightEntry.setMaximumSize(new Dimension(50 * fontsize/13, 50 * fontsize/13));
        itemBox.add(weightEntry);

        return this;
    }


    /**
     * class to set weight-input on an operator when it changes in the list *
     */
    class OperatorDocumentListener implements DocumentListener {
        Operator m_operator;
        JTextField m_weightEntry;

        OperatorDocumentListener(Operator operator, JTextField weightEntry) {
            m_operator = operator;
            m_weightEntry = weightEntry;
            textFields.add(weightEntry);
            operators.add(operator);
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            processEntry();
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            processEntry();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            processEntry();
        }

        void processEntry() {
            try {
                Double weight = Double.parseDouble(m_weightEntry.getText());
                m_operator.m_pWeight.setValue(weight, m_operator);
            } catch (Exception e) {
                // ignore
            }
            validate();
        }
    }

    ;

    @Override
    public void updateState() {
        super.updateState();
        for (int i = 0; i < textFields.size(); i++) {
            textFields.get(i).setText(operators.get(i).m_pWeight.get() + "");
            //m_labels.get(i).setText(getLabel(m_operators.get(i)));
            m_entries.get(i).setText(getLabel(operators.get(i)));
        }
    }

    String getLabel(Operator operator) {
        String name = operator.getClass().getName();
        name = name.substring(name.lastIndexOf('.') + 1);
        name = name.replaceAll("Operator", "");
        if (name.matches(".*[A-Z].*")) {
            name = name.replaceAll("(.)([A-Z])", "$1 $2");
        }
        name += ": ";
        try {
            for (BEASTInterface beastObject2 : operator.listActiveBEASTObjects()) {
                if (beastObject2 instanceof StateNode && ((StateNode) beastObject2).isEstimatedInput.get()) {
                    name += beastObject2.getID() + " ";
                }
                // issue https://github.com/CompEvol/beast2/issues/661
                if (name.length() > 100) {
                	name += "... ";
                	break;
                }
            }
        } catch (Exception e) {
            // ignore
        }
        String tipText = getDoc().tipTextMap.get(operator.getID());
        if (tipText != null) {
            name += " " + tipText;
        }
        return name;
    }
} // OperatorListInputEditor
