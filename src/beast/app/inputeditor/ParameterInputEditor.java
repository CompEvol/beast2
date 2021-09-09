package beast.app.inputeditor;


import java.awt.Color;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;

import beast.app.util.Utils;
import beast.base.BEASTInterface;
import beast.base.Input;
import beast.base.Log;
import beast.evolution.branchratemodel.BranchRateModel;
import beast.inference.Distribution;
import beast.inference.Operator;
import beast.inference.distribution.ParametricDistribution;
import beast.inference.parameter.Parameter;
import beast.parser.PartitionContext;





public class ParameterInputEditor extends BEASTObjectInputEditor {
	boolean isParametricDistributionParameter = false;
	
    //public ParameterInputEditor() {}
    public ParameterInputEditor(BeautiDoc doc) {
		super(doc);
	}

	private static final long serialVersionUID = 1L;
    public JCheckBox m_isEstimatedBox;

    @Override
    public Class<?> type() {
        return Parameter.Base.class;
    }
    
    
    @Override
    public void init(Input<?> input, BEASTInterface beastObject, int itemNr, ExpandOption isExpandOption, boolean addButtons) {
    	super.init(input, beastObject, itemNr, isExpandOption, addButtons);
    	m_beastObject = beastObject;
    }

    @Override
    protected void initEntry() {
        if (m_input.get() != null) {
        	if (itemNr < 0) {
        		Parameter.Base<?> parameter = (Parameter.Base<?>) m_input.get();
        		String s = "";
        		for (Object d : parameter.valuesInput.get()) {
        			s += d + " ";
        		}
        		m_entry.setText(s);
        	} else {
        		Parameter.Base<?> parameter = (Parameter.Base<?>) ((List<?>)m_input.get()).get(itemNr);
        		String s = "";
        		for (Object d : parameter.valuesInput.get()) {
        			s += d + " ";
        		}
        		m_entry.setText(s);
        	}
        }
    }

    @Override
    protected void processEntry() {
        try {
            String valueString = m_entry.getText();
            Parameter.Base<?> parameter = (Parameter.Base<?>) m_input.get();
        	String oldValue = "";
    		for (Object d : parameter.valuesInput.get()) {
    			oldValue += d + " ";
    		}
            int oldDim = parameter.getDimension();
            parameter.valuesInput.setValue(valueString, parameter);
            parameter.initAndValidate();
            int newDim = parameter.getDimension();
            if (oldDim != newDim) {
            	parameter.setDimension(oldDim);
                parameter.valuesInput.setValue(oldValue, parameter);
                parameter.initAndValidate();
                throw new IllegalArgumentException("Entry caused change in dimension");
            }
            validateInput();
        } catch (Exception ex) {
            m_validateLabel.setVisible(true);
            m_validateLabel.setToolTipText("<html><p>Parsing error: " + ex.getMessage() + ". Value was left at " + m_input.get() + ".</p></html>");
            m_validateLabel.m_circleColor = Color.orange;
            repaint();
        }
    }


    @Override
    protected void addComboBox(JComponent box, Input<?> input, BEASTInterface beastObject) {
        Box paramBox = Box.createHorizontalBox();
        Parameter.Base<?> parameter = null;
        if (itemNr >= 0) {
        	parameter = (Parameter.Base<?>) ((List<?>) input.get()).get(itemNr);
        } else {
        	parameter = (Parameter.Base<?>) input.get();
        }

        if (parameter == null) {
            super.addComboBox(box, input, beastObject);
        } else {
            setUpEntry();
            paramBox.add(m_entry);
            if (doc.allowLinking) {
	            boolean isLinked = doc.isLinked(m_input);
				if (isLinked || doc.suggestedLinks((BEASTInterface) m_input.get()).size() > 0) {
		            JButton linkbutton = new JButton(Utils.getIcon(ListInputEditor.ICONPATH + 
		            		(isLinked ? "link.png" : "unlink.png")));
		            linkbutton.setBorder(BorderFactory.createEmptyBorder());
		            linkbutton.setToolTipText("link/unlink this parameter with another compatible parameter");
		            linkbutton.addActionListener(e -> {
							if (doc.isLinked(m_input)) {
								// unlink
								try {
									BEASTInterface candidate = doc.getUnlinkCandidate(m_input, m_beastObject);
									m_input.setValue(candidate, m_beastObject);
									doc.deLink(m_input);
								} catch (RuntimeException e2) {
									e2.printStackTrace();
									JOptionPane.showMessageDialog(this, "Could not unlink: " + e2.getMessage());
								}
								
							} else {
								// create a link
								List<BEASTInterface> candidates = doc.suggestedLinks((BEASTInterface) m_input.get());
								JComboBox<BEASTInterface> jcb = new JComboBox<>(candidates.toArray(new BEASTInterface[]{}));
								JOptionPane.showMessageDialog( null, jcb, "select parameter to link with", JOptionPane.QUESTION_MESSAGE);
								BEASTInterface candidate = (BEASTInterface) jcb.getSelectedItem();
								if (candidate != null) {
									try {
										m_input.setValue(candidate, m_beastObject);
										doc.addLink(m_input);
									} catch (Exception e2) {
										e2.printStackTrace();
									}
								}
							}
							refreshPanel();
						});
		            paramBox.add(linkbutton);
				}
            }            
            
            paramBox.add(Box.createHorizontalGlue());

            m_isEstimatedBox = new JCheckBox(doc.beautiConfig.getInputLabel(parameter, parameter.isEstimatedInput.getName()));
            m_isEstimatedBox.setName(input.getName() + ".isEstimated");
            if (input.get() != null) {
                m_isEstimatedBox.setSelected(parameter.isEstimatedInput.get());
            }
            m_isEstimatedBox.setToolTipText(parameter.isEstimatedInput.getHTMLTipText());

            boolean isClockRate = false;
            for (Object output : parameter.getOutputs()) {
                if (output instanceof BranchRateModel.Base) {
                    isClockRate |= ((BranchRateModel.Base) output).meanRateInput.get() == parameter;
                }
            }
            m_isEstimatedBox.setEnabled(!isClockRate || !getDoc().autoSetClockRate);


            m_isEstimatedBox.addActionListener(e -> {
                    try {
                        Parameter.Base<?> parameter2 = (Parameter.Base<?>) m_input.get();
                        parameter2.isEstimatedInput.setValue(m_isEstimatedBox.isSelected(), parameter2);
                        if (isParametricDistributionParameter) {
                        	String id = parameter2.getID();
                        	

                        	if (id.startsWith("RealParameter")) {
                            	ParametricDistribution parent = null; 
                	            for (Object beastObject2 : parameter2.getOutputs()) {
                	                if (beastObject2 instanceof ParametricDistribution) {
                                		parent = (ParametricDistribution) beastObject2; 
                	                    break;
                	                }
                	            }
                	            Distribution grandparent = null; 
                	            for (Object beastObject2 : parent.getOutputs()) {
                	                if (beastObject2 instanceof Distribution) {
                                		grandparent = (Distribution) beastObject2; 
                	                    break;
                	                }
                	            }
                        		id = "parameter.hyper" + parent.getClass().getSimpleName() + "-" + 
                        				m_input.getName() + "-" + grandparent.getID();
                        		doc.pluginmap.remove(parameter2.getID());
                        		parameter2.setID(id);
                        		doc.addPlugin(parameter2);
                        	}
                        	
                        	
                        	PartitionContext context = new PartitionContext(id.substring("parameter.".length()));
                        	Log.warning.println(context + " " + id);
                        	doc.beautiConfig.hyperPriorTemplate.createSubNet(context, true);
                        }
                        refreshPanel();
                    } catch (Exception ex) {
                        Log.err.println("ParameterInputEditor " + ex.getMessage());
                    }
                });
            paramBox.add(m_isEstimatedBox);

            // only show the estimate flag if there is an operator that works on this parameter
            m_isEstimatedBox.setVisible(doc.isExpertMode());
            m_isEstimatedBox.setToolTipText("Estimate value of this parameter in the MCMC chain");
            //m_editPluginButton.setVisible(false);
            //m_bAddButtons = false;
            if (itemNr < 0) {
	            for (Object beastObject2 : ((BEASTInterface) m_input.get()).getOutputs()) {
	                if (beastObject2 instanceof ParametricDistribution) {
	                    m_isEstimatedBox.setVisible(true);
	                	isParametricDistributionParameter = true;
	                    break;
	                }
	            }
	            for (Object beastObject2 : ((BEASTInterface) m_input.get()).getOutputs()) {
	                if (beastObject2 instanceof Operator) {
	                    m_isEstimatedBox.setVisible(true);
	                    //m_editPluginButton.setVisible(true);
	                    break;
	                }
	            }
            } else {
	            for (Object beastObject2 : ((BEASTInterface) ((List<?>)m_input.get()).get(itemNr)).getOutputs()) {
	                if (beastObject2 instanceof Operator) {
	                    m_isEstimatedBox.setVisible(true);
	                    //m_editPluginButton.setVisible(true);
	                    break;
	                }
	            }
            }

            box.add(paramBox);
        }
    }

    @Override
    protected void addValidationLabel() {
        super.addValidationLabel();
        // make edit button invisible (if it exists) when this parameter is not estimateable
        if (m_editBEASTObjectButton != null)
            m_editBEASTObjectButton.setVisible(m_isEstimatedBox.isVisible());
    }

    @Override
    void refresh() {
        Parameter.Base<?> parameter = (Parameter.Base<?>) m_input.get();
		String s = "";
		for (Object d : parameter.valuesInput.get()) {
			s += d + " ";
		}
		m_entry.setText(s);
        m_isEstimatedBox.setSelected(parameter.isEstimatedInput.get());
        repaint();
    }

}
