package beast.app.beauti;


import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import beast.app.draw.InputEditor;
import beast.app.draw.IntegerInputEditor;
import beast.app.draw.PluginInputEditor;
import beast.app.draw.PluginPanel;
import beast.core.Input;
import beast.evolution.sitemodel.SiteModel;

public class SiteModelInputEditor extends PluginInputEditor {
	private static final long serialVersionUID = 1L;

	IntegerInputEditor m_categoryCountEditor;
	JTextField m_categoryCountEntry;
	InputEditor m_gammaShapeEditor;
	
	@Override
    public Class<?> type() {
        return SiteModel.Base.class;
    }
//	@Override
//    public Class<?> [] types() {
//		Class<?>[] types = {SiteModel.class, SiteModel.Base.class}; 
//		return types;
//    }


	public InputEditor createGammaCategoryCountEditor() throws Exception {
		Input<?> input = ((SiteModel) m_input.get()).gammaCategoryCount;
		m_categoryCountEditor = (IntegerInputEditor) PluginPanel.createInputEditor(input, m_plugin);
		m_categoryCountEntry = m_categoryCountEditor.getEntry();
		m_categoryCountEntry.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void removeUpdate(DocumentEvent e) {
				processEntry2();
			}
			@Override
			public void insertUpdate(DocumentEvent e) {
				processEntry2();
			}
			@Override
			public void changedUpdate(DocumentEvent e) {
				processEntry2();
			}
		});
		
		return m_categoryCountEditor;
	}
	
	void processEntry2() {
		String sCategories = m_categoryCountEntry.getText();
		try {
			int nCategories = Integer.parseInt(sCategories);
			m_gammaShapeEditor.setVisible(nCategories >= 2);
			repaint();
		} catch (java.lang.NumberFormatException e) {
			// ignore. 
		}
	}
	
	public InputEditor createShapeEditor() throws Exception {
		Input<?> input = ((SiteModel) m_input.get()).shapeParameter;
		m_gammaShapeEditor = PluginPanel.createInputEditor(input, m_plugin);
		m_gammaShapeEditor.setVisible(((SiteModel) m_input.get()).gammaCategoryCount.get() >= 2);
		return m_gammaShapeEditor;
	}
}
