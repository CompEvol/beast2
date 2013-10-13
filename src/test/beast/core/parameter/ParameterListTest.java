package test.beast.core.parameter;

import beast.core.parameter.IntegerParameter;
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
public class ParameterListTest {
    
    public ParameterListTest() { }

    /**
     * Tests
     */
    @Test
    public void test1() throws Exception {

        ParameterList<Integer> parameterList = new ParameterList<Integer>();
        
        RealParameter param1 = new RealParameter();
        param1.initByName("value", "2");
        
        RealParameter param2 = new RealParameter();
        param2.initByName("value", "3");
        
        parameterList.initByName(
                "initialParam", param1,
                "initialParam", param2);
        
        Parameter<Integer> param = parameterList.addNewParam();
        
        param.setValue(1);
        assert(parameterList.get(2).getValue() == 1);
    }
    
//    public static void main(String[] args) throws Exception {
//        ParameterListTest.test1();
//    }
}