package beast.app.draw;


import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JCheckBox;

import beast.app.beauti.BeautiConfig;
import beast.core.Input;
import beast.core.Plugin;
import beast.core.parameter.RealParameter;

public class ParameterInputEditor extends PluginInputEditor {
	private static final long serialVersionUID = 1L;
	JCheckBox m_isEstimatedBox;
	
	
    @Override
    public Class<?> type() {
        return RealParameter.class;
    }

	@Override
	void initEntry() {
		if (m_input.get()!= null) {
			RealParameter parameter = (RealParameter)m_input.get();
			m_entry.setText(parameter.m_pValues.get());
		}
	}

	@Override
    void processEntry() {
		try {
			String sValue = m_entry.getText();
			RealParameter parameter = (RealParameter)m_input.get();
			parameter.m_pValues.setValue(sValue, parameter);
			parameter.initAndValidate();
			checkValidation();
		} catch (Exception ex) {
			m_validateLabel.setVisible(true);
			m_validateLabel.setToolTipText("<html><p>Parsing error: " + ex.getMessage() + ". Value was left at " + m_input.get() +".</p></html>");
			m_validateLabel.m_circleColor = Color.orange;
			repaint();
		}
	}
    
    
	@Override
    void addComboBox(Box box, Input <?> input, Plugin plugin) {
		Box paramBox = Box.createHorizontalBox();
		RealParameter parameter = (RealParameter)input.get();
		
		if (parameter == null) {
			super.addComboBox(box, input, plugin);
		} else {
			setUpEntry();
			paramBox.add(m_entry);
			paramBox.add(Box.createHorizontalGlue());
	
			m_isEstimatedBox = new JCheckBox(BeautiConfig.getInputLabel(parameter, parameter.m_bIsEstimated.getName()));
			if (input.get() != null) {
				m_isEstimatedBox.setSelected(parameter.m_bIsEstimated.get());
			}
			m_isEstimatedBox.setToolTipText(parameter.m_bIsEstimated.getTipText());
			m_isEstimatedBox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					try {
						RealParameter parameter = (RealParameter)m_input.get();
						parameter.m_bIsEstimated.setValue(m_isEstimatedBox.isSelected(), parameter);
						refreshPanel();
					} catch (Exception ex) {
						System.err.println("ParameterInputEditor " + ex.getMessage());
					}
				}
			});
			paramBox.add(m_isEstimatedBox);
			
			
			box.add(paramBox);
		}
    }

	@Override
	void refresh() {
		RealParameter parameter = (RealParameter)m_input.get();
		m_entry.setText(parameter.m_pValues.get());
		m_isEstimatedBox.setSelected(parameter.m_bIsEstimated.get());
		repaint();
	}

}
