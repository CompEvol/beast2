package beast.app.beauti;


import java.lang.reflect.Method;
import java.util.List;

import beast.core.Description;
import beast.core.Input;
import beast.core.MCMC;
import beast.core.Operator;
import beast.core.BEASTObject;
import beast.core.BEASTInterface;
import beast.core.Input.Validate;



@Description("Specifies which part of the template get connected to the main network")
public class BeautiConnector extends BEASTObject {
    public Input<String> sMethodnput = new Input<String>("method", "name of static method that should be called with BeautiDoc as " +
    		"argument. For example beast.app.beauti.SiteModelInputEditor.custmoConnector");

    public Input<String> sSourceIDInput = new Input<String>("srcID", "ID of the plugin to be connected", Validate.XOR, sMethodnput);
    public Input<String> sTargetIDInput = new Input<String>("targetID", "ID of plugin to connect to", Validate.XOR, sMethodnput);
    public Input<String> sInputNameInput = new Input<String>("inputName", "name of the input of the plugin to connect to", Validate.XOR, sMethodnput);
    public Input<String> sTipText = new Input<String>("value", "associate some tip text with the srcID plugin, useful for displaying prior and operator specific information");

    public Input<String> sConditionInput = new Input<String>("if", "condition under which this connector should be executed." +
            "These should be of the form " +
            "inposterior(id) or id/input=value, e.g. inposterior(kappa), kappa/estimate=true. " +
            "inlikelihood(id) to check there is a plugin with suplied id that is predecessor of likelihood. " +
            "nooperator(id) to check there is no operator with suplied id. " +
            "isInitialising to execute only when subtemplate is first instantiated. " +
            "For partition specific ids, use $(n), e.g. e.g. kappa.$(n)/estimate=true. " +
            "For multiple conditions, separate by 'and', e.g. inposterior(kappa.$(n)) and kappa.$(n)/estimate=true");
//	public enum ConnectCondition {always, ifunlinked};
//	public Input<ConnectCondition> connectCondition = new Input<ConnectCondition>("connect","condition when to connect. Default is 'always'. " +
//			"With ifunlinked, the connector is only activated if the link does not already exists. " +
//			"Possible values: " + ConnectCondition.values(),
//			ConnectCondition.always, ConnectCondition.values());


    enum Operation {EQUALS, NOT_EQUALS, IS_IN_POSTERIOR, IS_IN_LIKELIHOOD, IS_NOT_AN_OPERTOR, AT_INITIALISATION_ONLY}
//	final static String IS_IN_POSTERIOR = "x";
//	final static String AT_INITIALISATION_ONLY = "y";

    String sSourceID;
    String sTargetID;
    String sTargetInput;

    String[] sConditionIDs;
    String[] sConditionInputs;
    Operation[] conditionOperations;
    String[] sConditionValues;
    
    boolean isRegularConnector = true;
    
    Method method = null;

    @Override
    public void initAndValidate() throws Exception {
        sSourceID = sSourceIDInput.get();
        sTargetID = sTargetIDInput.get();
        sTargetInput = sInputNameInput.get();

        if (sConditionInput.get() != null) {
            String[] sConditions = sConditionInput.get().split("\\s+and\\s+");
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
                } else if (s.startsWith("inlikelihood(")) {
                    sConditionIDs[i] = s.substring(s.indexOf("(") + 1, s.lastIndexOf(")"));
                    sConditionInputs[i] = null;
                    conditionOperations[i] = Operation.IS_IN_LIKELIHOOD;
                    sConditionValues[i] = null;
                } else if (s.startsWith("nooperator")) {
                    sConditionIDs[i] = s.substring(s.indexOf("(") + 1, s.lastIndexOf(")"));
                    conditionOperations[i] = Operation.IS_NOT_AN_OPERTOR;
                    sConditionInputs[i] = null;
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
        if (sMethodnput.get() != null) {
        	String fullMethod = sMethodnput.get();
        	String className = fullMethod.substring(0, fullMethod.lastIndexOf('.'));
        	String methodName = fullMethod.substring(fullMethod.lastIndexOf('.') + 1);
        	Class<?> class_ = Class.forName(className);
        	method = class_.getMethod(methodName, BeautiDoc.class);
            isRegularConnector = false;
        }

    }


    public boolean atInitialisationOnly() {
        if (conditionOperations.length > 0) {
            return conditionOperations[0].equals(Operation.AT_INITIALISATION_ONLY);
        } else {
            return false;
        }
    }

    /**
     * check that conditions in the 'if' input are met *
     */
    public boolean isActivated(PartitionContext partitionContext, List<BEASTInterface> posteriorPredecessors,
    		List<BEASTInterface> likelihoodPredecessors, BeautiDoc doc) {
        if (atInitialisationOnly()) {
            return false;
        }
        if (sMethodnput.get() != null) {
//        if (method != null) {
	    	try {
            	String fullMethod = sMethodnput.get();
            	String className = fullMethod.substring(0, fullMethod.lastIndexOf('.'));
            	String methodName = fullMethod.substring(fullMethod.lastIndexOf('.') + 1);
            	Class<?> class_ = Class.forName(className);
            	method = class_.getMethod(methodName, BeautiDoc.class);
        		method.invoke(null, doc);
        	} catch (Exception e) {
        		// ignore
        	}
        }

        boolean bIsActive = true;
        for (int i = 0; i < sConditionIDs.length; i++) {
        	//String sID = sConditionIDs[i].replaceAll("\\$\\(n\\)", sPartition);
        	String sID = BeautiDoc.translatePartitionNames(sConditionIDs[i], partitionContext);
            BEASTInterface plugin = doc.pluginmap.get(sID);
            if (plugin == null) {
            	if (conditionOperations[i] != Operation.IS_NOT_AN_OPERTOR) {
                    return false;
            		
            	}
                //System.err.println("isActivated::no plugin found");
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
                    case IS_IN_LIKELIHOOD:
                        if (!likelihoodPredecessors.contains(plugin)) {
                            //System.err.println(posteriorPredecessors);
                            //System.err.println("isActivated::is not in posterior, return false");
                            return false;
                        }
                        break;
                    //System.err.println("isActivated::is in posterior");
                    case IS_NOT_AN_OPERTOR:
        				List<Operator> operators = ((MCMC) doc.mcmc.get()).operatorsInput.get();
        				if (operators.contains(plugin)) {
        					return false;
        				}
                    	break;
                    case EQUALS:
                        Input<?> input = plugin.getInput(sConditionInputs[i]);
                        //System.err.println("isActivated::input " + input.get().toString() + " expected " + sConditionValues[i]);
                        if (input.get() == null) {
                        	if (!sConditionValues[i].equals("null")) {
                        		return false;
                        	}
                        } else if (!input.get().toString().equals(sConditionValues[i])) {
                            //System.err.println("isActivated::return false");
                            return false;
                        }
                        break;
                    case NOT_EQUALS:
                        Input<?> input2 = plugin.getInput(sConditionInputs[i]);
                        //System.err.println("isActivated::input " + input.get().toString() + " expected " + sConditionValues[i]);
                        if (input2.get() == null) {
                        	if (sConditionValues[i].equals("null")) {
                        		return false;
                        	}
                        } else if (input2.get().toString().equals(sConditionValues[i])) {
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
    	if (sMethodnput.get() != null) {
    		return "call " + sMethodnput.get();
    	}
        return "@" + sSourceID + " -> @" + sTargetID + "/" + sTargetInput;
    }


    public String toString(PartitionContext context) {
    	if (sMethodnput.get() != null) {
    		return toString();
    	}
        return "@" + BeautiDoc.translatePartitionNames(sSourceID, context) + " -> @" + sTargetID + "/" + BeautiDoc.translatePartitionNames(sTargetInput, context);
    }
}
