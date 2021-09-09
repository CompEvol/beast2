package beast.app.beauti;


import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JTextField;

import beast.app.inputeditor.BeautiDoc;
import beast.app.inputeditor.InputEditor;
import beast.app.inputeditor.ListInputEditor;
import beast.app.inputeditor.SmallLabel;
import beast.app.inputeditor.InputEditor.ButtonStatus;
import beast.app.inputeditor.InputEditor.ExpandOption;
import beast.base.BEASTInterface;
import beast.base.Input;
import beast.base.Log;
import beast.evolution.alignment.Alignment;
import beast.evolution.branchratemodel.BranchRateModel;
import beast.inference.MCMC;
import beast.inference.Operator;
import beast.inference.operator.DeltaExchangeOperator;
import beast.inference.parameter.IntegerParameter;
import beast.inference.parameter.RealParameter;





public class ClockModelListInputEditor extends ListInputEditor {
    private static final long serialVersionUID = 1L;
    List<JTextField> textFields = new ArrayList<>();
    List<Operator> operators = new ArrayList<>();

	public ClockModelListInputEditor(BeautiDoc doc) {
		super(doc);
	}

    @Override
    public Class<?> type() {
        return List.class;
    }

    @Override
    public Class<?> baseType() {
    	// disable this editor
    	return ClockModelListInputEditor.class;
        //return BranchRateModel.Base.class;
    }

    JCheckBox fixMeanRatesCheckBox;
    
    DeltaExchangeOperator operator;
    protected SmallLabel fixMeanRatesValidateLabel;
    
    @Override
    public void init(Input<?> input, BEASTInterface beastObject, int itemNr, ExpandOption isExpandOption, boolean addButtons) {
    	fixMeanRatesCheckBox = new JCheckBox("Fix mean rate of clock models");
    	m_buttonStatus = ButtonStatus.NONE;
    	super.init(input, beastObject, itemNr, isExpandOption, addButtons);
    	
		List<Operator> operators = ((MCMC) doc.mcmc.get()).operatorsInput.get();
    	fixMeanRatesCheckBox.addActionListener(e -> {
				JCheckBox averageRatesBox = (JCheckBox) e.getSource();
				boolean averageRates = averageRatesBox.isSelected();
				List<Operator> operators2 = ((MCMC) doc.mcmc.get()).operatorsInput.get();
				if (averageRates) {
					// connect DeltaExchangeOperator
					if (!operators2.contains(operator)) {
						operators2.add(operator);
					}
					// set up relative weights
					setUpOperator();
				} else {
					operators2.remove(operator);
					fixMeanRatesValidateLabel.setVisible(false);
					repaint();
				}
			});

    	operator = (DeltaExchangeOperator) doc.pluginmap.get("FixMeanRatesOperator");
    	if (operator == null) {
    		operator = new DeltaExchangeOperator();
    		try {
    			operator.setID("FixMeanRatesOperator");
				operator.initByName("weight", 2.0, "delta", 0.75);
			} catch (Exception e1) {
				// ignore initAndValidate exception
			}
    		doc.addPlugin(operator);
    	}
		fixMeanRatesCheckBox.setSelected(operators.contains(operator));
		Box box = Box.createHorizontalBox();
		box.add(fixMeanRatesCheckBox);
		box.add(Box.createHorizontalGlue());
		fixMeanRatesValidateLabel = new SmallLabel("x", Color.GREEN);
		fixMeanRatesValidateLabel.setVisible(false);
		box.add(fixMeanRatesValidateLabel);
		
    	if (((List<?>) input.get()).size() > 1 && operator != null) {
    		add(box);
    	}
		setUpOperator();
    }
    
    @Override
    public void validateInput() {
    	super.validateInput();
    	Log.warning.println("validateInput()");
    }
    
    /** set up relative weights and parameter input **/
    private void setUpOperator() {
    	String weights = "";
    	List<RealParameter> parameters = operator.parameterInput.get();
    	parameters.clear();
    	double commonClockRate = -1;
    	boolean isAllClocksAreEqual = true;
		try {
	    	for (int i = 0; i < doc.alignments.size(); i++) {
	    		Alignment data = doc.alignments.get(i); 
	    		int weight = data.getSiteCount();
	    		BranchRateModel.Base clockModel = (BranchRateModel.Base) doc.clockModels.get(i);
	    		RealParameter clockRate = clockModel.meanRateInput.get();
	    		//clockRate.m_bIsEstimated.setValue(true, clockRate);
	    		if (clockRate.isEstimatedInput.get()) {
	    			if (commonClockRate < 0) {
	    				commonClockRate = clockRate.valuesInput.get().get(0);
	    			} else {
	    				if (Math.abs(commonClockRate - clockRate.valuesInput.get().get(0)) > 1e-10) {
	    					isAllClocksAreEqual = false;
	    				}
	    			}
    				weights += weight + " ";
	    			parameters.add(clockRate);
	    		}
	    		//doc.autoSetClockRate = false;
	    	}
	    	if (!fixMeanRatesCheckBox.isSelected()) {
	    		fixMeanRatesValidateLabel.setVisible(false);
	    		return;
	    	}
	    	if (parameters.size() == 0) {
	    		fixMeanRatesValidateLabel.setVisible(true);
	    		fixMeanRatesValidateLabel.m_circleColor = Color.red;
	    		fixMeanRatesValidateLabel.setToolTipText("The model is invalid: At least one clock rate should be estimated.");
	    		return;
	    	}

	    	IntegerParameter weightParameter = new IntegerParameter(weights);
			weightParameter.setID("weightparameter");
			weightParameter.isEstimatedInput.setValue(false, weightParameter);
	    	operator.parameterWeightsInput.setValue(weightParameter, operator);
	    	if (!isAllClocksAreEqual) {
	    		fixMeanRatesValidateLabel.setVisible(true);
	    		fixMeanRatesValidateLabel.m_circleColor = Color.orange;
	    		fixMeanRatesValidateLabel.setToolTipText("Not all clocks are equal. Are you sure this is what you want?");
	    	} else if (parameters.size() == 1) {
	    		fixMeanRatesValidateLabel.setVisible(true);
	    		fixMeanRatesValidateLabel.m_circleColor = Color.orange;
	    		fixMeanRatesValidateLabel.setToolTipText("At least 2 clock models should have their rate estimated");
	    	} else if (parameters.size() < doc.alignments.size()) {
	    		fixMeanRatesValidateLabel.setVisible(true);
	    		fixMeanRatesValidateLabel.m_circleColor = Color.orange;
	    		fixMeanRatesValidateLabel.setToolTipText("Not all partitions have their rate estimated");
	    	} else {
	    		fixMeanRatesValidateLabel.setVisible(false);
	    	}
			repaint();
    		//doc.autoSetClockRate = true;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

} // OperatorListInputEditor
