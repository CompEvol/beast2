package beast.app.draw;


import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;

import beast.app.beauti.BeautiDoc;
import beast.app.beauti.BeautiPanel;
import beast.core.Input;
import beast.core.Operator;
import beast.core.Plugin;
import beast.core.parameter.RealParameter;
import beast.evolution.branchratemodel.BranchRateModel;

public class ParameterInputEditor extends PluginInputEditor {
    //public ParameterInputEditor() {}
    public ParameterInputEditor(BeautiDoc doc) {
		super(doc);
	}

	private static final long serialVersionUID = 1L;
    JCheckBox m_isEstimatedBox;

    @Override
    public Class<?> type() {
        return RealParameter.class;
    }
    
    
    @Override
    public void init(Input<?> input, Plugin plugin, int itemNr, ExpandOption bExpandOption, boolean bAddButtons) {
    	super.init(input, plugin, itemNr, bExpandOption, bAddButtons);
    	m_plugin = plugin;
    }

    @Override
    protected void initEntry() {
        if (m_input.get() != null) {
        	if (itemNr < 0) {
        		RealParameter parameter = (RealParameter) m_input.get();
        		m_entry.setText(parameter.m_pValues.get());
        	} else {
        		RealParameter parameter = (RealParameter) ((List)m_input.get()).get(itemNr);
        		m_entry.setText(parameter.m_pValues.get());
        	}
        }
    }

    @Override
    protected void processEntry() {
        try {
            String sValue = m_entry.getText();
            RealParameter parameter = (RealParameter) m_input.get();
            parameter.m_pValues.setValue(sValue, parameter);
            parameter.initAndValidate();
            validateInput();
        } catch (Exception ex) {
            m_validateLabel.setVisible(true);
            m_validateLabel.setToolTipText("<html><p>Parsing error: " + ex.getMessage() + ". Value was left at " + m_input.get() + ".</p></html>");
            m_validateLabel.m_circleColor = Color.orange;
            repaint();
        }
    }


    @Override
    protected void addComboBox(JComponent box, Input<?> input, Plugin plugin) {
        Box paramBox = Box.createHorizontalBox();
        RealParameter parameter = null;
        if (itemNr >= 0) {
        	parameter = (RealParameter) ((List) input.get()).get(itemNr);
        } else {
        	parameter = (RealParameter) input.get();
        }

        if (parameter == null) {
            super.addComboBox(box, input, plugin);
        } else {
            setUpEntry();
            paramBox.add(m_entry);
            if (doc.bAllowLinking) {
	            boolean isLinked = doc.isLinked(m_input);
	            JButton linkbutton = new JButton(BeautiPanel.getIcon(BeautiPanel.ICONPATH + 
	            		(isLinked ? "link.png" : "unlink.png")));
	            linkbutton.setBorder(BorderFactory.createEmptyBorder());
	            linkbutton.setToolTipText("link/unlink this parameter with another compatible parameter");
	            linkbutton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						if (doc.isLinked(m_input)) {
							// unlink
							try {
								Plugin candidate = doc.getUnlinkCandidate(m_input, m_plugin);
								m_input.setValue(candidate, m_plugin);
								doc.deLink(m_input);
							} catch (Exception e2) {
								e2.printStackTrace();
							}
							
						} else {
							// create a link
							List<Plugin> candidates = doc.suggestedLinks((Plugin) m_input.get());
							JComboBox jcb = new JComboBox(candidates.toArray());
							JOptionPane.showMessageDialog( null, jcb, "select parameter to link with", JOptionPane.QUESTION_MESSAGE);
							Plugin candidate = (Plugin) jcb.getSelectedItem();
							if (candidate != null) {
								try {
									m_input.setValue(candidate, m_plugin);
									doc.addLink(m_input);
								} catch (Exception e2) {
									e2.printStackTrace();
								}
							}
						}
						refreshPanel();
					}
				});
	            paramBox.add(linkbutton);
            }            
            
            paramBox.add(Box.createHorizontalGlue());

            m_isEstimatedBox = new JCheckBox(doc.beautiConfig.getInputLabel(parameter, parameter.m_bIsEstimated.getName()));
            if (input.get() != null) {
                m_isEstimatedBox.setSelected(parameter.m_bIsEstimated.get());
            }
            m_isEstimatedBox.setToolTipText(parameter.m_bIsEstimated.getTipText());

            boolean bIsClockRate = false;
            for (Plugin output : parameter.outputs) {
                if (output instanceof BranchRateModel.Base) {
                    bIsClockRate |= ((BranchRateModel.Base) output).meanRateInput.get() == parameter;
                }
            }
            m_isEstimatedBox.setEnabled(!bIsClockRate || !getDoc().bAutoSetClockRate);


            m_isEstimatedBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        RealParameter parameter = (RealParameter) m_input.get();
                        parameter.m_bIsEstimated.setValue(m_isEstimatedBox.isSelected(), parameter);
                        refreshPanel();
                    } catch (Exception ex) {
                        System.err.println("ParameterInputEditor " + ex.getMessage());
                    }
                }
            });
            paramBox.add(m_isEstimatedBox);

            // only show the estimate flag if there is an operator that works on this parameter
            m_isEstimatedBox.setVisible(doc.isExpertMode());
            m_isEstimatedBox.setToolTipText("Estimate value of this parameter in the MCMC chain");
            //m_editPluginButton.setVisible(false);
            //m_bAddButtons = false;
            if (itemNr < 0) {
	            for (Plugin plugin2 : ((Plugin) m_input.get()).outputs) {
	                if (plugin2 instanceof Operator) {
	                    m_isEstimatedBox.setVisible(true);
	                    //m_editPluginButton.setVisible(true);
	                    break;
	                }
	            }
            } else {
	            for (Plugin plugin2 : ((Plugin) ((List)m_input.get()).get(itemNr)).outputs) {
	                if (plugin2 instanceof Operator) {
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
        if (m_editPluginButton != null)
            m_editPluginButton.setVisible(m_isEstimatedBox.isVisible());
    }

    @Override
    void refresh() {
        RealParameter parameter = (RealParameter) m_input.get();
        m_entry.setText(parameter.m_pValues.get());
        m_isEstimatedBox.setSelected(parameter.m_bIsEstimated.get());
        repaint();
    }

}
