package test.beast.core.parameter;

import beast.core.Operator;
import beast.core.State;
import beast.core.parameter.Parameter;
import beast.core.parameter.ParameterList;
import beast.core.parameter.RealParameter;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for ParameterList class.
 *
 * @author Tim Vaughan <tgvaughan@gmail.com>
 */
public class ParameterListTest extends Operator {
    
    public ParameterListTest() { }

    /**
     * Tests
     */
    @Test
    public void test1() throws Exception {

        ParameterList<Double> parameterList = new ParameterList<Double>();
        
        // Parameters with which to initialise list
        RealParameter param1 = new RealParameter();
        param1.initByName("value", "2");
        
        RealParameter param2 = new RealParameter();
        param2.initByName("value", "3");
        
        // Initialise parameter list
        parameterList.initByName(
                "initialParam", param1,
                "initialParam", param2);
        
        // Create dummy state to allow statenode editing
        State state = new State();
        state.initByName("stateNode", parameterList);
        state.initialise();
        
        // Test parameter value modification
        parameterList.get(0).setValue(20.0);
        
        // Test parameter creation and modification
        Parameter<Double> newParam = parameterList.addNewParam();
        newParam.setValue(53.0);
        
        assert(parameterList.get(0).getValue() == 20.0);
        assert(parameterList.get(1).getValue() == 3.0);
        assert(parameterList.get(2).getValue() == 53.0);
        assert(parameterList.size()==3);

        // Test state restore
        parameterList.restore();
        
        assert(parameterList.get(0).getValue() == 2.0);
        assert(parameterList.get(1).getValue() == 3.0);
        assert(parameterList.size()==2);
        
    }

    @Override
    public double proposal() {
        return 0.0;
    }
}