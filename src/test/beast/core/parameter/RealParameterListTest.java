package test.beast.core.parameter;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import beast.core.Operator;
import beast.core.State;
import beast.core.parameter.Parameter;
import beast.core.parameter.RealParameter;
import beast.core.parameter.RealParameterList;

/**
 * Unit tests for ParameterList class.
 *
 * @author Tim Vaughan <tgvaughan@gmail.com>
 */
public class RealParameterListTest extends Operator {
    
    public RealParameterListTest() { }

    @Test
    public void test1() throws Exception {

        RealParameterList parameterList = new RealParameterList();
        
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
        
        assertEquals(parameterList.get(0).getValue(), 20.0, 1e-15);
        assertEquals(parameterList.get(0).getKey(), 0);
        assertEquals(parameterList.get(1).getValue(), 3.0, 1e-15);
        assertEquals(parameterList.get(1).getKey(), 1);
        assertEquals(parameterList.get(2).getValue(), 53.0, 1e-15);
        assertEquals(parameterList.get(2).getKey(), 2);
        assertEquals(parameterList.size(), 3);
        
        parameterList.remove(1);
        
        newParam = parameterList.addNewParam();
        newParam.setValue(42.0);

        assertEquals(parameterList.get(0).getValue(), 20.0, 1e-15);
        assertEquals(parameterList.get(0).getKey(), 0, 1e-15);
        assertEquals(parameterList.get(1).getValue(), 53.0, 1e-15);
        assertEquals(parameterList.get(1).getKey(), 2, 1e-15);
        assertEquals(parameterList.get(2).getValue(), 42.0, 1e-15);
        assertEquals(parameterList.get(2).getKey(), 1);
        assertEquals(parameterList.size(), 3);
        
        // Test state restore
        parameterList.restore();
        
        assertEquals(parameterList.get(0).getValue(),2.0, 1e-15);
        assertEquals(parameterList.get(0).getKey(),0);
        assertEquals(parameterList.get(1).getValue(),3.0, 1e-15);
        assertEquals(parameterList.get(1).getKey(),1);
        assertEquals(parameterList.size(),2);
        
        
        // Test serialization
        String xmlStr = parameterList.toXML();
        assertEquals(xmlStr,"<statenode id='null'>"
                + "Dimension: [1, 1], "
                + "Bounds: [-Infinity,Infinity], "
                + "AvailableKeys: [], NextKey: 2, "
                + "Parameters: [[2.0],[3.0]], "
                + "ParameterKeys: [0,1]"
                + "</statenode>\n");
        
        // Test deserialization
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        Document doc = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xmlStr.getBytes()));
        doc.normalize();
        NodeList nodes = doc.getElementsByTagName("*");
        org.w3c.dom.Node docNode = nodes.item(0);
        
        RealParameterList newParameterList = new RealParameterList();
        newParameterList.initAndValidate();
        newParameterList.fromXML(docNode);
        
        assertEquals(newParameterList.get(0).getValue(),2.0, 1e-15);
        assertEquals(newParameterList.get(0).getKey(),0);
        assertEquals(newParameterList.get(1).getValue(),3.0, 1e-15);
        assertEquals(newParameterList.get(1).getKey(),1);
        assertEquals(newParameterList.size(),2);
        
    }
    
    @Override
    public double proposal() {
        return 0.0;
    }

	@Override
	public void initAndValidate() throws Exception {
		// TODO Auto-generated method stub
		
	}
}