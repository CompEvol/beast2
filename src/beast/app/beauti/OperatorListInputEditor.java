package beast.app.beauti;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import beast.app.draw.ListInputEditor;
import beast.core.Operator;
import beast.core.Plugin;
import beast.core.StateNode;

public class OperatorListInputEditor extends ListInputEditor {
	private static final long serialVersionUID = 1L;
	List<JTextField> textFields = new ArrayList<JTextField>(); 
	List<Operator> operators = new ArrayList<Operator>();

	@Override
	public Class<?> type() {
		return List.class;
	}
	@Override
	public Class<?> baseType() {
		return Operator.class;
	}

	@Override
    protected void addPluginItem(Box itemBox, Plugin plugin) {
        Operator operator = (Operator) plugin;
        
        JTextField entry = new JTextField(getLabel(operator));
        entry.setMinimumSize(new Dimension(200, 16));
        entry.setMaximumSize(new Dimension(200, 20));
        m_entries.add(entry);
        itemBox.add(Box.createRigidArea(new Dimension(5,1)));
        itemBox.add(entry);
        entry.setEditable(false);
//        JLabel label = new JLabel(getLabel(operator));
//        label.setBackground(Color.WHITE);
//        m_labels.add(label);
//        itemBox.add(label);

        
        
        itemBox.add(Box.createHorizontalGlue());
        JTextField weightEntry = new JTextField();
        weightEntry.setToolTipText(operator.m_pWeight.getTipText());
        weightEntry.setText(operator.m_pWeight.get() + "");
        weightEntry.getDocument().addDocumentListener(new OperatorDocumentListener(operator, weightEntry));    
        Dimension size = new Dimension(50, 16);
        weightEntry.setMinimumSize(size);
        weightEntry.setPreferredSize(size);
        weightEntry.setMaximumSize(size);
        itemBox.add(weightEntry);
        
    }
    
    
    /** class to set weight-input on an operator when it changes in the list **/
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
				Double fWeight = Double.parseDouble(m_weightEntry.getText());
				m_operator.m_pWeight.setValue(fWeight, m_operator);
			} catch (Exception e) {
				// ignore
			}
			validate();
		}
	};     
    
	@Override
    protected void updateState() {
		super.updateState();
		for (int i = 0; i < textFields.size(); i++) {
			textFields.get(i).setText(operators.get(i).m_pWeight.get() + "");
			//m_labels.get(i).setText(getLabel(m_operators.get(i)));
			m_entries.get(i).setText(getLabel(operators.get(i)));
		}
    }
	
	String getLabel(Operator operator) {
        String sName = operator.getID();
        if (sName == null || sName.length() == 0) {
            sName = operator.getClass().getName();
            sName = sName.substring(sName.lastIndexOf('.') + 1);
        }
        sName  += ": ";
        try {
        	for (Plugin plugin2 : operator.listActivePlugins()) {
        		if (plugin2 instanceof StateNode && ((StateNode) plugin2).m_bIsEstimated.get()) {
        			sName += plugin2.getID() + " "; 
        		}
        	}
        } catch (Exception e) {
			// ignore
		}
        return sName;
	}
} // OperatorListInputEditor
