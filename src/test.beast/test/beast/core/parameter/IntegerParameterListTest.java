package test.beast.core.parameter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import beast.base.inference.Operator;
import beast.base.inference.State;
import beast.base.inference.parameter.IntegerParameter;
import beast.base.inference.parameter.IntegerParameterList;
import beast.base.inference.parameter.Parameter;

/**
 * Unit tests for ParameterList class.
 *
 * @author Tim Vaughan <tgvaughan@gmail.com>
 */
public class IntegerParameterListTest extends Operator {
    
    public IntegerParameterListTest() { }

    @Test
    public void test1() throws Exception {

        IntegerParameterList parameterList = new IntegerParameterList();
        
        // Parameters with which to initialise list
        IntegerParameter param1 = new IntegerParameter();
        param1.initByName("value", "2");
        
        IntegerParameter param2 = new IntegerParameter();
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
        parameterList.get(0).setValue(20);
        
        // Test parameter creation and modification
        Parameter<Integer> newParam = parameterList.addNewParam();
        newParam.setValue(53);
        
        assertTrue(parameterList.get(0).getValue()==20);
        assertTrue(parameterList.get(0).getKey()==0);
        assertTrue(parameterList.get(1).getValue()==3);
        assertTrue(parameterList.get(1).getKey()==1);
        assertTrue(parameterList.get(2).getValue()==53);
        assertTrue(parameterList.get(2).getKey()==2);
        assertTrue(parameterList.size()==3);
        
        parameterList.remove(1);
        
        newParam = parameterList.addNewParam();
        newParam.setValue(42);

        assertTrue(parameterList.get(0).getValue()==20);
        assertTrue(parameterList.get(0).getKey()==0);
        assertTrue(parameterList.get(1).getValue()==53);
        assertTrue(parameterList.get(1).getKey()==2);
        assertTrue(parameterList.get(2).getValue()==42);
        assertTrue(parameterList.get(2).getKey()==1);
        assertTrue(parameterList.size()==3);
        
        // Test state restore
        parameterList.restore();
        
        assertTrue(parameterList.get(0).getValue()==2);
        assertTrue(parameterList.get(0).getKey()==0);
        assertTrue(parameterList.get(1).getValue()==3);
        assertTrue(parameterList.get(1).getKey()==1);
        assertTrue(parameterList.size()==2);
        
        
        // Test serialization
        String xmlStr = parameterList.toXML();
        assertEquals(xmlStr,"<statenode id='null'>"
                + "Dimension: [1, 1], "
                + "Bounds: [-2147483647,2147483646], "
                + "AvailableKeys: [], "
                + "NextKey: 2, "
                + "Parameters: [[2],[3]], "
                + "ParameterKeys: [0,1]"
                + "</statenode>\n");
        
        // Test deserialization
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        Document doc = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xmlStr.getBytes()));
        doc.normalize();
        NodeList nodes = doc.getElementsByTagName("*");
        org.w3c.dom.Node docNode = nodes.item(0);
        
        IntegerParameterList newParameterList = new IntegerParameterList();
        newParameterList.initAndValidate();
        newParameterList.fromXML(docNode);
        
        assertTrue(newParameterList.get(0).getValue()==2);
        assertTrue(newParameterList.get(0).getKey()==0);
        assertTrue(newParameterList.get(1).getValue()==3);
        assertTrue(newParameterList.get(1).getKey()==1);
        assertTrue(newParameterList.size()==2);
        
    }
    
    @Override
    public double proposal() {
        return 0.0;
    }

	@Override
	public void initAndValidate() {
		// TODO Auto-generated method stub
		
	}
}