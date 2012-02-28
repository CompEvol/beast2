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

    IntegerInputEditor categoryCountEditor;
    JTextField categoryCountEntry;
    InputEditor gammaShapeEditor;

	public SiteModelInputEditor(BeautiDoc doc) {
		super(doc);
	}

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
        categoryCountEditor = (IntegerInputEditor) doc.getInpuEditorFactory().createInputEditor(input, m_plugin, doc);
        categoryCountEntry = categoryCountEditor.getEntry();
        categoryCountEntry.getDocument().addDocumentListener(new DocumentListener() {
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

        return categoryCountEditor;
    }

    void processEntry2() {
        String sCategories = categoryCountEntry.getText();
        try {
            int nCategories = Integer.parseInt(sCategories);
            gammaShapeEditor.getComponent().setVisible(nCategories >= 2);
            repaint();
        } catch (java.lang.NumberFormatException e) {
            // ignore.
        }
    }

    public InputEditor createShapeEditor() throws Exception {
        Input<?> input = ((SiteModel) m_input.get()).shapeParameterInput;
        gammaShapeEditor = doc.getInpuEditorFactory().createInputEditor(input, m_plugin, doc);
        gammaShapeEditor.getComponent().setVisible(((SiteModel) m_input.get()).gammaCategoryCount.get() >= 2);
        return gammaShapeEditor;
    }
}
