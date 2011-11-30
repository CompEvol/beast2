package beast.app.beauti;

import java.util.List;

import beast.app.draw.PluginPanel;
import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.Plugin;

@Description("Specifies which part of the template get connected to the main network")
public class BeautiConnector extends Plugin {
	public Input<String> sSourceIDInput = new Input<String>("srcID","ID of the plugin to be connected", Validate.REQUIRED);
	public Input<String> sTargetIDInput = new Input<String>("targetID","ID of plugin to connect to", Validate.REQUIRED);
	public Input<String> sInputNameInput = new Input<String>("inputName","name of the input of the plugin to connect to", Validate.REQUIRED);
	public Input<String> sTipText = new Input<String>("value","associate some tip text with the srcID plugin, useful for displaying prior and operator specific information");
	
	public Input<String> sConditionInput = new Input<String>("if","condition under which this connector should be executed." +
			"These should be of the form " +
			"inposterior(id) or id/input=value, e.g. inposterior(kappa), kappa/estimate=true. " +
			"For partition specific ids, use $(n), e.g. e.g. kappa.$(n)/estimate=true. " +
			"For multiple conditions, separate by 'and', e.g. inposterior(kappa.$(n)) and kappa.$(n)/estimate=true");
//	public enum ConnectCondition {always, ifunlinked};
//	public Input<ConnectCondition> connectCondition = new Input<ConnectCondition>("connect","condition when to connect. Default is 'always'. " +
//			"With ifunlinked, the connector is only activated if the link does not already exists. " +
//			"Possible values: " + ConnectCondition.values(),
//			ConnectCondition.always, ConnectCondition.values());

	
	enum Operation {EQUALS, NOT_EQUALS, IS_IN_POSTERIOR, AT_INITIALISATION_ONLY}
//	final static String IS_IN_POSTERIOR = "x";
//	final static String AT_INITIALISATION_ONLY = "y";
	
	String sSourceID;
	String sTargetID;
	String sTargetInput;
	
	String [] sConditionIDs;
	String [] sConditionInputs;
	Operation [] conditionOperations;
	String [] sConditionValues;

	@Override
	public void initAndValidate() throws Exception {
		sSourceID = sSourceIDInput.get();
		sTargetID = sTargetIDInput.get();
		sTargetInput = sInputNameInput.get();

		if (sConditionInput.get() != null) {
			String [] sConditions = sConditionInput.get().split("\\s+and\\s+");
			sConditionIDs = new String[sConditions.length];
			sConditionInputs = new String[sConditions.length];
			sConditionValues = new String[sConditions.length];
			conditionOperations = new Operation[sConditions.length];
			for (int i = 0; i < sConditions.length; i++) {
				String s = sConditions[i];
				if (s.startsWith("inposterior(")) {
					sConditionIDs[i] = s.substring(s.indexOf("(") + 1, s.lastIndexOf(")"));
					sConditionInputs[i] = null;
					conditionOperations[i] = Operation.IS_IN_POSTERIOR;
					sConditionValues[i] = null;
				} else if (s.startsWith("isInitializing")) {
					sConditionIDs[i] = null;
					conditionOperations[i] = Operation.AT_INITIALISATION_ONLY;
					sConditionInputs[i] = null;
					sConditionValues[i] = null;
				} else {
					sConditionIDs[i] = s.substring(0, s.indexOf("/"));
					sConditionInputs[i] = s.substring(s.indexOf("/") + 1, s.indexOf("="));
					sConditionValues[i] = s.substring(s.indexOf("=") + 1);
					conditionOperations[i] = Operation.EQUALS;
					if (sConditionInputs[i].endsWith("!")) {
						sConditionInputs[i] = sConditionInputs[i].substring(0, sConditionInputs[i].length() - 1);
						conditionOperations[i] = Operation.NOT_EQUALS;
					}
				}
			}
		} else {
			sConditionIDs = new String[0];
			sConditionInputs = new String[0];
			conditionOperations = new Operation[0];
			sConditionValues = new String[0];
		}
	}
	
	
	public boolean atInitialisationOnly() {
		if (conditionOperations.length > 0) {
			return conditionOperations[0].equals(Operation.AT_INITIALISATION_ONLY);
		} else {
			return false;
		}
	}
	
	/** check that conditions in the 'if' input are met **/
	public boolean isActivated(String sPartition, List<Plugin> posteriorPredecessors) {
		if (atInitialisationOnly()) {
			return false;
		}
		
		
		boolean bIsActive = true;
		for (int i = 0; i < sConditionIDs.length; i++) {
			String sID = sConditionIDs[i].replaceAll("\\$\\(n\\)", sPartition);
			Plugin plugin = PluginPanel.g_plugins.get(sID);
			if (plugin == null) {
				//System.err.println("isActivated::no plugin found");
				return false;
			}
			//System.err.println("isActivated::found " + sID);
			try {
				switch (conditionOperations[i]) {
				case IS_IN_POSTERIOR:
					if (!posteriorPredecessors.contains(plugin)) {
						//System.err.println(posteriorPredecessors);
						//System.err.println("isActivated::is not in posterior, return false");
						return false;
					}
					break;
					//System.err.println("isActivated::is in posterior");
				case EQUALS:
					Input<?> input = plugin.getInput(sConditionInputs[i]);
					//System.err.println("isActivated::input " + input.get().toString() + " expected " + sConditionValues[i]);
					if (!input.get().toString().equals(sConditionValues[i])) {
						//System.err.println("isActivated::return false");
						return false;
					}
					break;
				case NOT_EQUALS:
					Input<?> input2 = plugin.getInput(sConditionInputs[i]);
					//System.err.println("isActivated::input " + input.get().toString() + " expected " + sConditionValues[i]);
					if (input2.get().toString().equals(sConditionValues[i])) {
						//System.err.println("isActivated::return false");
						return false;
					}
					break;
					default:
						throw new Exception("Unexpected operation: " + conditionOperations[i]);
				}
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}
		//if (sConditionIDs.length > 0) {
		//	System.err.println("isActivated::return true");
		//}
		return bIsActive;
	}

	public String getTipText() {
		return sTipText.get(); 
	}
	
	public String toString() {
		return "@" + sSourceID + " -> @" + sTargetID + "/" + sTargetInput;
	}
}
