package beast.app.beauti;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JComboBox;

import beast.app.draw.PluginInputEditor;
import beast.core.Input;
import beast.core.Plugin;
import beast.core.parameter.RealParameter;
import beast.evolution.alignment.Alignment;
import beast.evolution.substitutionmodel.Frequencies;

public class FrequenciesInputEditor extends PluginInputEditor {
	RealParameter m_freqsParameter;
	Alignment m_alignment;

	private static final long serialVersionUID = 1L;
	boolean m_bUseDefaultBehavior;

	@Override
	public Class<?> type() {
		return Frequencies.class;
	}
	
    @Override
    public void init(Input<?> input, Plugin plugin, EXPAND bExpand, boolean bAddButtons) {
   		super.init(input, plugin, bExpand, bAddButtons);
    } // init


	@Override
	/** suppress combobox **/
	protected void addComboBox(Box box, Input<?> input, Plugin plugin) {
		Frequencies freqs = (Frequencies) input.get();
		
		JComboBox comboBox = new JComboBox(new String[] {"Estimated","Empirical","All equal"});
		if (freqs.frequencies.get() != null) {
			comboBox.setSelectedIndex(0);
			m_freqsParameter = freqs.frequencies.get();
			m_alignment = (Alignment) getCandidate(freqs.m_data, freqs);
		} else if (freqs.m_bEstimate.get()) {
			comboBox.setSelectedIndex(1);
			m_alignment = freqs.m_data.get();
			m_freqsParameter = (RealParameter) getCandidate(freqs.frequencies, freqs);
		} else {
			comboBox.setSelectedIndex(2);
			m_alignment = freqs.m_data.get();
			m_freqsParameter = (RealParameter) getCandidate(freqs.frequencies, freqs);
		}
		comboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JComboBox comboBox = (JComboBox) e.getSource();
                int iSelected = comboBox.getSelectedIndex();
				Frequencies freqs = (Frequencies) m_input.get();
				try {
				switch (iSelected) {
					case 0:
						freqs.frequencies.setValue(m_freqsParameter, freqs);
						freqs.m_data.setValue(null, freqs);
						break;
					case 1:
						freqs.frequencies.setValue(null, freqs);
						freqs.m_data.setValue(m_alignment, freqs);
						freqs.m_bEstimate.setValue(true, freqs);
						break;
					case 2:
						freqs.frequencies.setValue(null, freqs);
						freqs.m_data.setValue(m_alignment, freqs);
						freqs.m_bEstimate.setValue(false, freqs);
						break;
					}
				} catch (Exception e2) {
					e2.printStackTrace();
				}
				//System.err.println(freqs.frequencies.get() + " " + freqs.m_data.get() + " " + freqs.m_bEstimate.get());
			}
		});
		box.add(comboBox);
	}

	private Plugin getCandidate(Input<?> input, Frequencies freqs) {
		return BeautiDoc.g_doc.getPartition(freqs);
//		List<String> sCandidates = PluginPanel.getAvailablePlugins(input, freqs, null);
//		String sID = sCandidates.get(0);
//		Plugin plugin = PluginPanel.g_plugins.get(sID);
//		return plugin;
	}


	
	@Override
	/** suppress input label**/
	protected void addInputLabel() {
		super.addInputLabel();
	}
	
}
