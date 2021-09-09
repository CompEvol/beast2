package beast.app.inputeditor;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;

import beast.base.BEASTInterface;
import beast.base.Input;
import beast.base.Log;
import beast.evolution.alignment.Taxon;
import beast.evolution.alignment.TaxonSet;
import beast.evolution.operator.TipDatesRandomWalker;
import beast.evolution.tree.MRCAPrior;
import beast.evolution.tree.Tree;
import beast.inference.Distribution;
import beast.inference.Operator;
import beast.inference.State;
import beast.inference.distribution.OneOnX;
import beast.inference.util.CompoundDistribution;
import beast.parser.PartitionContext;



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
	public void init(Input<?> input, BEASTInterface beastObject, final int listItemNr, ExpandOption isExpandOption, boolean addButtons) {
        m_bAddButtons = addButtons;
        m_input = input;
        m_beastObject = beastObject;
        this.itemNr= listItemNr;
		
        Box itemBox = Box.createHorizontalBox();

        MRCAPrior prior = (MRCAPrior) beastObject;
        String text = prior.getID();

        JButton taxonButton = new JButton(text);
//        taxonButton.setMinimumSize(Base.PREFERRED_SIZE);
//        taxonButton.setPreferredSize(Base.PREFERRED_SIZE);
        itemBox.add(taxonButton);
        taxonButton.addActionListener(e -> {
                List<?> list = (List<?>) m_input.get();
                MRCAPrior prior2 = (MRCAPrior) list.get(itemNr);
                try {
                    TaxonSet taxonset = prior2.taxonsetInput.get();
                    List<Taxon> originalTaxa = new ArrayList<>();
                    originalTaxa.addAll(taxonset.taxonsetInput.get());
                    Set<Taxon> candidates = getTaxonCandidates(prior2);
                    TaxonSetDialog dlg = new TaxonSetDialog(taxonset, candidates, doc);
                    if (dlg.showDialog()) {
        	            if (dlg.taxonSet.taxonsetInput.get().size() == 0) {
        	            	JOptionPane.showMessageDialog(doc.beauti, "At least one taxon should be included in the taxon set",
        	            			"Error specifying taxon set", JOptionPane.ERROR_MESSAGE);
        	            	taxonset.taxonsetInput.get().addAll(originalTaxa);
        	            	return;
        	            }

                        prior2.taxonsetInput.setValue(dlg.taxonSet, prior2);
                        int i = 1;
                        String id = dlg.taxonSet.getID();
                        while (doc.pluginmap.containsKey(dlg.taxonSet.getID()) && doc.pluginmap.get(dlg.taxonSet.getID()) != dlg.taxonSet) {
                        	dlg.taxonSet.setID(id + i);
                        	i++;
                        }
                        BEASTObjectPanel.addPluginToMap(dlg.taxonSet, doc);
                        prior2.setID(dlg.taxonSet.getID() + ".prior");

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
        comboBox.setName(text+".distr");

        if (prior.distInput.get() != null) {
            String id = prior.distInput.get().getID();
            //id = BeautiDoc.parsePartition(id);
            id = id.substring(0, id.indexOf('.'));
            for (BeautiSubTemplate template : availableBEASTObjects) {
                if (template.classInput.get() != null && template.shortClassName.equals(id)) {
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
        isMonophyleticdBox.setName(text+".isMonophyletic");
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
				if (prior.onlyUseTipsInput.get()) {
					disableTipSampling(m_beastObject, doc);
				}
				doc.unregisterPlugin(prior);
				refreshPanel();
			}

        });
        itemBox.add(Box.createGlue());
        itemBox.add(deleteButton);

        add(itemBox);
	}
	
	public static void customConnector(BeautiDoc doc) {
		Object o0 = doc.pluginmap.get("prior");
		if (o0 != null && o0 instanceof CompoundDistribution) {
			CompoundDistribution p =  (CompoundDistribution) o0;
			for (Distribution p0 : p.pDistributions.get()) {
				if (p0 instanceof MRCAPrior) {
					MRCAPrior prior = (MRCAPrior) p0;
			        if (prior.treeInput.get() != null) {
			        	boolean isInState = false;
			        	for (BEASTInterface o : prior.treeInput.get().getOutputs()) {
			        		if (o instanceof State) {
			        			isInState = true;
			        			break;
			        		}
			        	}
			        	if (!isInState) {
			        		doc.disconnect(prior, "prior", "distribution");
			        		doc.disconnect(prior, "tracelog", "log");
			        		if (prior.onlyUseTipsInput.get()) {
			        			disableTipSampling(prior, doc);
			        		}
			        		doc.unregisterPlugin(prior);
			        		return;
			        	}
					}
				}
			}
		}

	}
	
	Set<Taxon> getTaxonCandidates(MRCAPrior prior) {
        Set<Taxon> candidates = new HashSet<>();
        Tree tree = prior.treeInput.get();
        String [] taxa = null;
        if (tree.m_taxonset.get() != null) {
        	try {
            	TaxonSet set = tree.m_taxonset.get();
        		set.initAndValidate();
            	taxa = set.asStringList().toArray(new String[0]);
        	} catch (Exception e) {
            	taxa = prior.treeInput.get().getTaxaNames();
			}
        } else {
        	taxa = prior.treeInput.get().getTaxaNames();
        }
        
        for (String taxon : taxa) {
            candidates.add(doc.getTaxon(taxon));
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
    
    
    InputEditor tipsonlyEditor;
    
    public InputEditor createTipsonlyEditor() throws NoSuchMethodException, SecurityException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        BooleanInputEditor e = new BooleanInputEditor (doc) {
			private static final long serialVersionUID = 1L;

			@Override
        	public void init(Input<?> input, BEASTInterface beastObject, int itemNr, ExpandOption isExpandOption,
        			boolean addButtons) {
        		super.init(input, beastObject, itemNr, isExpandOption, addButtons);
        		// hack to get to JCheckBox
        		Component [] components = getComponents();       		
        		((JCheckBox) components[0]).addActionListener(e -> {
                	JCheckBox src = (JCheckBox) e.getSource();
                	if (src.isSelected()) {
                		enableTipSampling();
                	} else {
                		disableTipSampling(m_beastObject, doc);
                	}
                });
        	}
        	
        };

        MRCAPrior prior = (MRCAPrior) m_beastObject;
        Input<?> input = prior.onlyUseTipsInput;
        e.init(input, prior, -1, ExpandOption.FALSE, false);
        return e;
    }

    // add TipDatesRandomWalker (if not present) and add to list of operators
    private void enableTipSampling() {
    	// First, create/find the operator
    	TipDatesRandomWalker operator = null;
    	MRCAPrior prior = (MRCAPrior) m_beastObject;
    	TaxonSet taxonset = prior.taxonsetInput.get();
    	taxonset.initAndValidate();
    	
    	// see if an old operator still hangs around -- happens when toggling the TipsOnly checkbox a few times
    	for (BEASTInterface o : taxonset.getOutputs()) {
    		if (o instanceof TipDatesRandomWalker) {
    			operator = (TipDatesRandomWalker) o;
    		}
    	}
    	
    	if (operator == null) {
    		operator = new TipDatesRandomWalker();
    		operator.initByName("tree", prior.treeInput.get(), "taxonset", taxonset, "windowSize", 1.0, "weight", 1.0);
    	}
   		operator.setID("tipDatesSampler." + taxonset.getID());
   	    	
    	doc.mcmc.get().setInputValue("operator", operator);
	}

    // remove TipDatesRandomWalker from list of operators
	private static void disableTipSampling(BEASTInterface m_beastObject, BeautiDoc doc) {
    	// First, find the operator
    	TipDatesRandomWalker operator = null;
    	MRCAPrior prior = (MRCAPrior) m_beastObject;
    	TaxonSet taxonset = prior.taxonsetInput.get();
    	
    	// We cannot rely on the operator ID created in enableTipSampling()
    	// since the taxoneset name may have changed.
    	// However, if there is an TipDatesRandomWalker with taxonset as input, we want to remove it.
    	for (BEASTInterface o : taxonset.getOutputs()) {
    		if (o instanceof TipDatesRandomWalker) {
    			operator = (TipDatesRandomWalker) o;
    		}
    	}
    	
    	if (operator == null) {
    		// should never happen
    		return;
    	}
    	
    	// remove from list of operators
    	Object o = doc.mcmc.get().getInput("operator");
    	if (o instanceof Input<?>) {
    		Input<List<Operator>> operatorInput = (Input<List<Operator>>) o;
    		List<Operator> operators = operatorInput.get();
    		operators.remove(operator);
    	}
	}

}
