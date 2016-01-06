package beast.app.beauti;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;

import beast.app.draw.InputEditor;
import beast.app.draw.SmallButton;
import beast.core.BEASTInterface;
import beast.core.Input;
import beast.core.util.Log;
import beast.evolution.alignment.Taxon;
import beast.evolution.alignment.TaxonSet;
import beast.math.distributions.MRCAPrior;
import beast.math.distributions.OneOnX;



public class MRCAPriorInputEditor extends InputEditor.Base {
	private static final long serialVersionUID = 1L;

	public MRCAPriorInputEditor(BeautiDoc doc) {
		super(doc);
	}

	@Override
	public Class<?> type() {
		return MRCAPrior.class;
	}

	@Override
	public void init(Input<?> input, BEASTInterface beastObject, final int listItemNr, ExpandOption bExpandOption, boolean bAddButtons) {
        m_bAddButtons = bAddButtons;
        m_input = input;
        m_beastObject = beastObject;
        this.itemNr= listItemNr;
		
        Box itemBox = Box.createHorizontalBox();

        MRCAPrior prior = (MRCAPrior) beastObject;
        String sText = prior.getID();

        JButton taxonButton = new JButton(sText);
        taxonButton.setMinimumSize(Base.PREFERRED_SIZE);
        taxonButton.setPreferredSize(Base.PREFERRED_SIZE);
        itemBox.add(taxonButton);
        taxonButton.addActionListener(e -> {
                List<?> list = (List<?>) m_input.get();
                MRCAPrior prior2 = (MRCAPrior) list.get(itemNr);
                try {
                    TaxonSet taxonset = prior2.taxonsetInput.get();
                    Set<Taxon> candidates = getTaxonCandidates(prior2);
                    TaxonSetDialog dlg = new TaxonSetDialog(taxonset, candidates, doc);
                    if (dlg.showDialog()) {
                        prior2.setID(dlg.taxonSet.getID());
                        prior2.taxonsetInput.setValue(dlg.taxonSet, prior2);
                    }
                } catch (Exception e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
                refreshPanel();
            });


        if (prior.distInput.getType() == null) {
            try {
                prior.distInput.setValue(new OneOnX(), prior);
                prior.distInput.setValue(null, prior);
            } catch (Exception e) {
                // TODO: handle exception
            }

        }

        List<BeautiSubTemplate> availableBEASTObjects = doc.getInputEditorFactory().getAvailableTemplates(prior.distInput, prior, null, doc);
        JComboBox<BeautiSubTemplate> comboBox = new JComboBox<>(availableBEASTObjects.toArray(new BeautiSubTemplate[]{}));
        comboBox.setName(sText+".distr");

        if (prior.distInput.get() != null) {
            String sID = prior.distInput.get().getID();
            //sID = BeautiDoc.parsePartition(sID);
            sID = sID.substring(0, sID.indexOf('.'));
            for (BeautiSubTemplate template : availableBEASTObjects) {
                if (template.sClassInput.get() != null && template.sShortClassName.equals(sID)) {
                    comboBox.setSelectedItem(template);
                }
            }
        } else {
            comboBox.setSelectedItem(BeautiConfig.NULL_TEMPLATE);
        }
        comboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                @SuppressWarnings("unchecked")
				JComboBox<BeautiSubTemplate> comboBox = (JComboBox<BeautiSubTemplate>) e.getSource();
                BeautiSubTemplate template = (BeautiSubTemplate) comboBox.getSelectedItem();
                List<?> list = (List<?>) m_input.get();
                MRCAPrior prior = (MRCAPrior) list.get(itemNr);

//System.err.println("PRIOR" + beastObject2);
//            	try {
//					prior.m_distInput.setValue(beastObject2, prior);
//				} catch (Exception e1) {
//					// TODO Auto-generated catch block
//					e1.printStackTrace();
//				}
                try {
                    //BEASTObject beastObject2 =
                    template.createSubNet(new PartitionContext(""), prior, prior.distInput, true);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
                refreshPanel();
            }
        });
        itemBox.add(comboBox);

        JCheckBox isMonophyleticdBox = new JCheckBox(doc.beautiConfig.getInputLabel(prior, prior.isMonophyleticInput.getName()));
        isMonophyleticdBox.setName(sText+".isMonophyletic");
        isMonophyleticdBox.setSelected(prior.isMonophyleticInput.get());
        isMonophyleticdBox.setToolTipText(prior.isMonophyleticInput.getHTMLTipText());
        isMonophyleticdBox.addActionListener(new MRCAPriorActionListener(prior));
        itemBox.add(isMonophyleticdBox);

        JButton deleteButton = new SmallButton("-", true);
        deleteButton.setToolTipText("Delete this calibration");
        deleteButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Log.warning.println("Trying to delete a calibration");
				List<?> list = (List<?>) m_input.get();
				MRCAPrior prior = (MRCAPrior) list.get(itemNr);
				doc.disconnect(prior, "prior", "distribution");
				doc.disconnect(prior, "tracelog", "log");
				doc.unregisterPlugin(prior);
				refreshPanel();
			}        	
        });
        itemBox.add(Box.createGlue());
        itemBox.add(deleteButton);

        add(itemBox);
	}
	
    Set<Taxon> getTaxonCandidates(MRCAPrior prior) {
        Set<Taxon> candidates = new HashSet<>();
        for (String sTaxon : prior.treeInput.get().getTaxaNames()) {
            Taxon taxon = doc.getTaxon(sTaxon);
            candidates.add(taxon);
        }
        return candidates;
    }

    /**
     * class to deal with toggling monophyletic flag on an MRCAPrior *
     */
    class MRCAPriorActionListener implements ActionListener {
        MRCAPrior m_prior;

        MRCAPriorActionListener(MRCAPrior prior) {
            m_prior = prior;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                m_prior.isMonophyleticInput.setValue(((JCheckBox) e.getSource()).isSelected(), m_prior);
                refreshPanel();
            } catch (Exception ex) {
            	Log.warning.println("PriorListInputEditor " + ex.getMessage());
            }
        }
    }

}
