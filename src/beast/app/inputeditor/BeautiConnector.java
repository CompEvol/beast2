package beast.app.inputeditor;


import java.lang.reflect.Method;
import java.util.List;

import beast.base.BEASTInterface;
import beast.base.BEASTObject;
import beast.base.Description;
import beast.base.Input;
import beast.base.Input.Validate;
import beast.inference.MCMC;
import beast.inference.Operator;
import beast.parser.PartitionContext;
import beast.pkgmgmt.BEASTClassLoader;



@Description("Specifies which part of the template get connected to the main network")
public class BeautiConnector extends BEASTObject {
    final public Input<String> methodnput = new Input<>("method", "name of static method that should be called with BeautiDoc as " +
    		"argument. For example beast.app.beauti.SiteModelInputEditor.custmoConnector");

    final public Input<String> sourceIDInput = new Input<>("srcID", "ID of the beastObject to be connected", Validate.XOR, methodnput);
    final public Input<String> targetIDInput = new Input<>("targetID", "ID of beastObject to connect to", Validate.XOR, methodnput);
    final public Input<String> inputNameInput = new Input<>("inputName", "name of the input of the beastObject to connect to", Validate.XOR, methodnput);
    final public Input<String> tipText = new Input<>("value", "associate some tip text with the srcID beastObject, useful for displaying prior and operator specific information");

    final public Input<String> conditionInput = new Input<>("if", "condition under which this connector should be executed." +
            "These should be of the form " +
            "inposterior(id) or id/input=value, e.g. inposterior(kappa), kappa/estimate=true. " +
            "inlikelihood(id) to check there is a beastObject with suplied id that is predecessor of likelihood. " +
            "nooperator(id) to check there is no operator with suplied id. " +
            "isInitialising to execute only when subtemplate is first instantiated. " +
            "For partition specific ids, use $(n), e.g. e.g. kappa.$(n)/estimate=true. " +
            "For multiple conditions, separate by 'and', e.g. inposterior(kappa.$(n)) and kappa.$(n)/estimate=true");
//	public enum ConnectCondition {always, ifunlinked};
//	public Input<ConnectCondition> connectCondition = new Input<>("connect","condition when to connect. Default is 'always'. " +
//			"With ifunlinked, the connector is only activated if the link does not already exists. " +
//			"Possible values: " + ConnectCondition.values(),
//			ConnectCondition.always, ConnectCondition.values());


    enum Operation {EQUALS, NOT_EQUALS, IS_IN_POSTERIOR, IS_IN_LIKELIHOOD, IS_NOT_AN_OPERTOR, AT_INITIALISATION_ONLY}
//	final static String IS_IN_POSTERIOR = "x";
//	final static String AT_INITIALISATION_ONLY = "y";

    String sourceID;
    String targetID;
    String targetInput;

    String[] conditionIDs;
    String[] conditionInputs;
    Operation[] conditionOperations;
    String[] conditionValues;
    
    boolean isRegularConnector = true;
    
    Method method = null;

    public BeautiConnector() {}

    public BeautiConnector(String sourceID, String targetID, String inputName, String condition) {
		initByName("srcID", sourceID, "targetID", targetID, "inputName", inputName, 
				"if", condition);
    }


	@Override
    public void initAndValidate() {
        sourceID = sourceIDInput.get();
        targetID = targetIDInput.get();
        targetInput = inputNameInput.get();

        if (conditionInput.get() != null) {
            String[] conditions = conditionInput.get().split("\\s+and\\s+");
            conditionIDs = new String[conditions.length];
            conditionInputs = new String[conditions.length];
            conditionValues = new String[conditions.length];
            conditionOperations = new Operation[conditions.length];
            for (int i = 0; i < conditions.length; i++) {
                String s = conditions[i];
                if (s.startsWith("inposterior(")) {
                    conditionIDs[i] = s.substring(s.indexOf("(") + 1, s.lastIndexOf(")"));
                    conditionInputs[i] = null;
                    conditionOperations[i] = Operation.IS_IN_POSTERIOR;
                    conditionValues[i] = null;
                } else if (s.startsWith("inlikelihood(")) {
                    conditionIDs[i] = s.substring(s.indexOf("(") + 1, s.lastIndexOf(")"));
                    conditionInputs[i] = null;
                    conditionOperations[i] = Operation.IS_IN_LIKELIHOOD;
                    conditionValues[i] = null;
                } else if (s.startsWith("nooperator")) {
                    conditionIDs[i] = s.substring(s.indexOf("(") + 1, s.lastIndexOf(")"));
                    conditionOperations[i] = Operation.IS_NOT_AN_OPERTOR;
                    conditionInputs[i] = null;
                    conditionValues[i] = null;
                } else if (s.startsWith("isInitializing")) {
                    conditionIDs[i] = null;
                    conditionOperations[i] = Operation.AT_INITIALISATION_ONLY;
                    conditionInputs[i] = null;
                    conditionValues[i] = null;
                } else {
                    conditionIDs[i] = s.substring(0, s.indexOf("/"));
                    conditionInputs[i] = s.substring(s.indexOf("/") + 1, s.indexOf("="));
                    conditionValues[i] = s.substring(s.indexOf("=") + 1);
                    conditionOperations[i] = Operation.EQUALS;
                    if (conditionInputs[i].endsWith("!")) {
                        conditionInputs[i] = conditionInputs[i].substring(0, conditionInputs[i].length() - 1);
                        conditionOperations[i] = Operation.NOT_EQUALS;
                    }
                }
            }
        } else {
            conditionIDs = new String[0];
            conditionInputs = new String[0];
            conditionOperations = new Operation[0];
            conditionValues = new String[0];
        }
        if (methodnput.get() != null) {
        	String fullMethod = methodnput.get();
        	String className = fullMethod.substring(0, fullMethod.lastIndexOf('.'));
        	String methodName = fullMethod.substring(fullMethod.lastIndexOf('.') + 1);
        	Class<?> class_;
			try {
				class_ = BEASTClassLoader.forName(className);
	        	method = class_.getMethod(methodName, BeautiDoc.class);
			} catch (ClassNotFoundException | NoSuchMethodException | SecurityException e) {
				throw new IllegalArgumentException(e.getMessage());
			}
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
        if (methodnput.get() != null) {
//        if (method != null) {
	    	try {
            	String fullMethod = methodnput.get();
            	String className = fullMethod.substring(0, fullMethod.lastIndexOf('.'));
            	String methodName = fullMethod.substring(fullMethod.lastIndexOf('.') + 1);
            	Class<?> class_ = BEASTClassLoader.forName(className);
            	method = class_.getMethod(methodName, BeautiDoc.class);
        		method.invoke(null, doc);
        	} catch (Exception e) {
        		// ignore
        	}
        }

        boolean isActive = true;
        for (int i = 0; i < conditionIDs.length; i++) {
        	//String id = conditionIDs[i].replaceAll("\\$\\(n\\)", partition);
        	String id = BeautiDoc.translatePartitionNames(conditionIDs[i], partitionContext);
            BEASTInterface beastObject = doc.pluginmap.get(id);
            if (beastObject == null) {
            	if (conditionOperations[i] != Operation.IS_NOT_AN_OPERTOR) {
                    return false;
            		
            	}
                //System.err.println("isActivated::no beastObject found");
            }
            //System.err.println("isActivated::found " + id);
            try {
                switch (conditionOperations[i]) {
                    case IS_IN_POSTERIOR:
                        if (!posteriorPredecessors.contains(beastObject)) {
                            //System.err.println(posteriorPredecessors);
                            //System.err.println("isActivated::is not in posterior, return false");
                            return false;
                        }
                        break;
                    case IS_IN_LIKELIHOOD:
                        if (!likelihoodPredecessors.contains(beastObject)) {
                            //System.err.println(posteriorPredecessors);
                            //System.err.println("isActivated::is not in posterior, return false");
                            return false;
                        }
                        break;
                    //System.err.println("isActivated::is in posterior");
                    case IS_NOT_AN_OPERTOR:
        				List<Operator> operators = ((MCMC) doc.mcmc.get()).operatorsInput.get();
        				if (operators.contains(beastObject)) {
        					return false;
        				}
                    	break;
                    case EQUALS:
                        final Input<?> input = beastObject.getInput(conditionInputs[i]);
                        //System.err.println("isActivated::input " + input.get().toString() + " expected " + conditionValues[i]);
                        if (input.get() == null) {
                        	if (!conditionValues[i].equals("null")) {
                        		return false;
                        	}
                        } else if (!input.get().toString().equals(conditionValues[i])) {
                            //System.err.println("isActivated::return false");
                            return false;
                        }
                        break;
                    case NOT_EQUALS:
                        final Input<?> input2 = beastObject.getInput(conditionInputs[i]);
                        //System.err.println("isActivated::input " + input.get().toString() + " expected " + conditionValues[i]);
                        if (input2.get() == null) {
                        	if (conditionValues[i].equals("null")) {
                        		return false;
                        	}
                        } else if (input2.get().toString().equals(conditionValues[i])) {
                            //System.err.println("isActivated::return false");
                            return false;
                        }
                        break;
                    default:
                        throw new IllegalArgumentException("Unexpected operation: " + conditionOperations[i]);
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        //if (conditionIDs.length > 0) {
        //	System.err.println("isActivated::return true");
        //}
        return isActive;
    }

    public String getTipText() {
        return tipText.get();
    }

    @Override
	public String toString() {
    	if (methodnput.get() != null) {
    		return "call " + methodnput.get();
    	}
        return "@" + sourceID + " -> @" + targetID + "/" + targetInput;
    }


    public String toString(PartitionContext context) {
    	if (methodnput.get() != null) {
    		return toString();
    	}
        return "@" + BeautiDoc.translatePartitionNames(sourceID, context) + " -> @" + targetID + "/" + BeautiDoc.translatePartitionNames(targetInput, context);
    }
}
