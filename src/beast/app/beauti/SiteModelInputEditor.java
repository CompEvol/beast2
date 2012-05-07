package beast.app.beauti;


import java.awt.Color;

import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import beast.app.draw.InputEditor;
import beast.app.draw.IntegerInputEditor;
import beast.app.draw.ParameterInputEditor;
import beast.app.draw.PluginInputEditor;
import beast.core.Input;
import beast.core.Plugin;
import beast.core.parameter.RealParameter;
import beast.evolution.sitemodel.SiteModel;

public class SiteModelInputEditor extends PluginInputEditor {
    private static final long serialVersionUID = 1L;

    IntegerInputEditor categoryCountEditor;
    JTextField categoryCountEntry;
    InputEditor gammaShapeEditor;
    ParameterInputEditor inVarEditor;

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
    	SiteModel sitemodel = ((SiteModel) m_input.get()); 
        Input<?> input = sitemodel.gammaCategoryCount;
        categoryCountEditor = new IntegerInputEditor(doc) {
			private static final long serialVersionUID = 1L;

			public void validateInput() {
        		super.validateInput();
            	SiteModel sitemodel = (SiteModel) m_plugin; 
                if (sitemodel.gammaCategoryCount.get() < 2 && ((RealParameter)sitemodel.shapeParameterInput.get()).m_bIsEstimated.get()) {
                	m_validateLabel.m_circleColor = Color.orange;
                	m_validateLabel.setToolTipText("shape parameter is estimated, but not used");
                	m_validateLabel.setVisible(true);
                }
        	};
        };
        
        categoryCountEditor.init(input, sitemodel, -1, ExpandOption.FALSE, true);
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
        
       	categoryCountEditor.validateInput();
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
        gammaShapeEditor = doc.getInpuEditorFactory().createInputEditor(input, (Plugin) m_input.get(), doc);
        gammaShapeEditor.getComponent().setVisible(((SiteModel) m_input.get()).gammaCategoryCount.get() >= 2);
        return gammaShapeEditor;
    }

    public InputEditor createProportionInvariantEditor() throws Exception {
        Input<?> input = ((SiteModel) m_input.get()).invarParameterInput;
        inVarEditor = new ParameterInputEditor(doc) {
			private static final long serialVersionUID = 1L;

			@Override
            public void validateInput() {
				RealParameter p = (RealParameter) m_input.get();
				if (p.m_bIsEstimated.get() && Double.parseDouble(p.m_pValues.get()) <= 0.0) {
                    m_validateLabel.setVisible(true);
                    m_validateLabel.setToolTipText("<html><p>Proportion invariant should be non-zero when estimating</p></html>");
                    return;
				}
            	super.validateInput();
            }
        };
        inVarEditor.init(input, (Plugin) m_input.get(), -1, ExpandOption.FALSE, true);
        inVarEditor.addValidationListener(this);
        return inVarEditor;
    }

}
