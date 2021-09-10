package beast.app.beauti;


import java.awt.event.ActionEvent;

import javax.swing.JComboBox;
import javax.swing.JComponent;

import beast.app.inputeditor.BEASTObjectInputEditor;
import beast.app.inputeditor.BeautiDoc;
import beast.app.inputeditor.InputEditor;
import beast.app.inputeditor.InputEditor.ExpandOption;
import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beast.base.evolution.alignment.Alignment;
import beast.base.evolution.substitutionmodel.Frequencies;
import beast.base.inference.parameter.RealParameter;



public class FrequenciesInputEditor extends BEASTObjectInputEditor {
    RealParameter freqsParameter;
    Alignment alignment;

    private static final long serialVersionUID = 1L;
    boolean useDefaultBehavior;

	public FrequenciesInputEditor(BeautiDoc doc) {
		super(doc);
	}

    @Override
    public Class<?> type() {
        return ActionEvent.class;
        //return Frequencies.class;
    }

    @Override
    public void init(Input<?> input, BEASTInterface beastObject, int itemNr, ExpandOption isExpandOption, boolean addButtons) {
        super.init(input, beastObject, itemNr, isExpandOption, addButtons);
    } // init


    @Override
    /** suppress combobox **/
    protected void addComboBox(JComponent box, Input<?> input, BEASTInterface beastObject) {
        Frequencies freqs = (Frequencies) input.get();

        JComboBox<String> comboBox = new JComboBox<>(new String[]{"Estimated", "Empirical", "All equal"});
        if (freqs.frequenciesInput.get() != null) {
            comboBox.setSelectedIndex(0);
            freqsParameter = freqs.frequenciesInput.get();
            alignment = (Alignment) getCandidate(freqs.dataInput, freqs);
        } else if (freqs.estimateInput.get()) {
            comboBox.setSelectedIndex(1);
            alignment = freqs.dataInput.get();
            freqsParameter = (RealParameter) getCandidate(freqs.frequenciesInput, freqs);
        } else {
            comboBox.setSelectedIndex(2);
            alignment = freqs.dataInput.get();
            freqsParameter = (RealParameter) getCandidate(freqs.frequenciesInput, freqs);
        }
        comboBox.addActionListener(e -> {
                //@SuppressWarnings("unchecked")
				//JComboBox<String> comboBox = (JComboBox<String>) e.getSource();
                int selected = comboBox.getSelectedIndex();
                //Frequencies freqs = (Frequencies) m_input.get();
                try {
                    switch (selected) {
                        case 0:
                            freqs.frequenciesInput.setValue(freqsParameter, freqs);
                            freqs.dataInput.setValue(null, freqs);
                            break;
                        case 1:
                            freqs.frequenciesInput.setValue(null, freqs);
                            freqs.dataInput.setValue(alignment, freqs);
                            freqs.estimateInput.setValue(true, freqs);
                            break;
                        case 2:
                            freqs.frequenciesInput.setValue(null, freqs);
                            freqs.dataInput.setValue(alignment, freqs);
                            freqs.estimateInput.setValue(false, freqs);
                            break;
                    }
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
                //System.err.println(freqs.frequencies.get() + " " + freqs.m_data.get() + " " + freqs.m_bEstimate.get());
            });
        box.add(comboBox);
    }

    private BEASTInterface getCandidate(Input<?> input, Frequencies freqs) {
        return getDoc().getPartition(freqs);
//		List<String> candidates = PluginPanel.getAvailablePlugins(input, freqs, null);
//		String id = candidates.get(0);
//		BEASTObject beastObject = PluginPanel.g_plugins.get(id);
//		return beastObject;
    }


    @Override
    /** suppress input label**/
    protected void addInputLabel() {
        super.addInputLabel();
    }

}
