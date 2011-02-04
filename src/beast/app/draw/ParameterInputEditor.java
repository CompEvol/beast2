package beast.app.draw;

import javax.swing.Box;
import javax.swing.JLabel;

import beast.core.Input;
import beast.core.Plugin;
import beast.core.parameter.RealParameter;

public class ParameterInputEditor extends PluginInputEditor {

	JLabel m_parameterlabel;
	
    @Override
    public Class<?> type() {
        return RealParameter.class;
    }
	
	@Override
    void addComboBox(Box box, Input <?> input, Plugin plugin) {
		RealParameter parameter = (RealParameter) input.get();
		if (parameter != null) {
			m_parameterlabel = new JLabel("");
			refresh();
			box.add(m_parameterlabel); 
			box.add(Box.createHorizontalGlue());
		} else {
			super.addComboBox(box, input, plugin);
		}
    }

	@Override
    void refresh() {
		RealParameter parameter = (RealParameter)m_input.get();
		String sStr = parameter.m_pValues.get() + " (";
		if (parameter.lowerValueInput.get() != null) {
			sStr += parameter.lowerValueInput.get() +",";
		} else {
			sStr += "0,";
		}
		if (parameter.upperValueInput.get() != null) {
			sStr += parameter.upperValueInput.get() +")";
		} else {
			sStr += "Infinity)";
		}
		m_parameterlabel.setText(sStr);
		m_parameterlabel.repaint();
	}

}
