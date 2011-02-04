package beast.app.beauti;


import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JOptionPane;

import beast.app.draw.ListInputEditor;
import beast.app.draw.PluginPanel;
import beast.core.Input;
import beast.core.Plugin;
import beast.evolution.alignment.Taxon;
import beast.evolution.alignment.TaxonSet;

public class TaxonSetInputEditor extends ListInputEditor {
	private static final long serialVersionUID = 1L;

	@Override
	public Class<?> type() {
		return List.class;
	}
	@Override
	public Class<?> baseType() {
		return TaxonSet.class;
	}
	
	@Override
    public void init(Input<?> input, Plugin plugin, boolean bExpand, boolean bAddButtons) {
		super.init(input, plugin, bExpand, bAddButtons);
		JButton guessButton = new JButton("Guess taxon sets");
		guessButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String sRegexp = "^(.+)[_\\. ].*$";
				sRegexp = JOptionPane.showInputDialog("<html>Try to match this expression (hit ok if you don't know what this means. It's ok. Trust me...)" +
						" The pattern in brackets will be matched and grouped together.</html>", sRegexp);
				// build map of names onto taxon sets from Taxon IDs
				HashMap<String, TaxonSet> map = new HashMap<String, TaxonSet>();
		    	Pattern m_pattern = Pattern.compile(sRegexp);
		    	for (Taxon taxon : BeautiDoc.m_taxa) {
		    		Matcher matcher = m_pattern.matcher(taxon.getID());
					if (matcher.find()) {
						String sMatch = matcher.group(1);
						try {
							if (map.containsKey(sMatch)) {
								TaxonSet set = map.get(sMatch);
								set.m_taxonset.setValue(taxon, set);
							} else {
								TaxonSet set = new TaxonSet();
								set.setID(sMatch);
								set.m_taxonset.setValue(taxon, set);
								map.put(sMatch, set);
							}
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}
		    	}
				// add taxon sets that contain at least 2 items to the list
		    	for (TaxonSet set : map.values()) {
		    		if (set.m_taxonset.get().size() > 1) {
		    			// add
		                try {
		                    m_input.setValue(set, m_plugin);
		                } catch (Exception ex) {
		                    System.err.println(ex.getClass().getName() + " " + ex.getMessage());
		                }
		                addSingleItem(set);
		    		}
		    	}
                checkValidation();
                updateState();
                repaint();
			}
		});
		
    	Component c = m_listBox.getComponent(m_listBox.getComponentCount() - 1);
		((Box)c).add(guessButton);
		add(Box.createVerticalGlue());
	} // init

	@Override
	protected Object editItem(Object o) {
		int i = ((List<?>)m_input.get()).indexOf(o);
		TaxonSet taxonset = (TaxonSet) ((List<?>)m_input.get()).get(i);
        TaxonSetDialog dlg = new TaxonSetDialog(taxonset, BeautiDoc.m_taxa);
        dlg.setVisible(true);
        if (dlg.m_bOK) {
        	m_labels.get(i).setText(dlg.m_taxonSet.getID());
        	o = dlg.m_taxonSet;
        }
        PluginPanel.m_position.x -= 20;
        PluginPanel.m_position.y -= 20;
        checkValidation();
        updateState();
        repaint();
        return o;
	} // editItem
}
