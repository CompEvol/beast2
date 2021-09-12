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
import beast.base.inference.parameter.BooleanParameter;
import beast.base.inference.parameter.BooleanParameterList;
import beast.base.inference.parameter.Parameter;

/**
 * Unit tests for ParameterList class.
 *
 * @author Tim Vaughan <tgvaughan@gmail.com>
 */
public class BooleanParameterListTest extends Operator {
    
    public BooleanParameterListTest() { }

    @Test
    public void test1() throws Exception {

        BooleanParameterList parameterList = new BooleanParameterList();
        
        // Parameters with which to initialise list
        BooleanParameter param1 = new BooleanParameter();
        param1.initByName("value", "true false true");
        
        BooleanParameter param2 = new BooleanParameter();
        param2.initByName("value", "false true false");
        
        // Initialise parameter list
        parameterList.initByName(
                "dimension", 3,
                "initialParam", param1,
                "initialParam", param2);
        
        // Create dummy state to allow statenode editing
        State state = new State();
        state.initByName("stateNode", parameterList);
        state.initialise();
        
        // Test parameter value modification
        parameterList.get(0).setValue(0, false);
        
        // Test parameter creation and modification
        Parameter<Boolean> newParam = parameterList.addNewParam();
        newParam.setValue(0, true);
        newParam.setValue(1, true);
        newParam.setValue(2, true);
        
        assertTrue(parameterList.get(0).getValue(0)==false);
        assertTrue(parameterList.get(0).getValue(1)==false);
        assertTrue(parameterList.get(0).getValue(2)==true);
        assertTrue(parameterList.get(0).getKey()==0);
        assertTrue(parameterList.get(1).getValue(0)==false);
        assertTrue(parameterList.get(1).getValue(1)==true);
        assertTrue(parameterList.get(1).getValue(2)==false);
        assertTrue(parameterList.get(1).getKey()==1);
        assertTrue(parameterList.get(2).getValue(0)==true);
        assertTrue(parameterList.get(2).getValue(1)==true);
        assertTrue(parameterList.get(2).getValue(2)==true);
        assertTrue(parameterList.get(2).getKey()==2);
        assertTrue(parameterList.size()==3);
        
        parameterList.remove(1);
        
        newParam = parameterList.addNewParam();
        newParam.setValue(0, false);
        newParam.setValue(1, false);
        newParam.setValue(2, false);

        assertTrue(parameterList.get(0).getValue(0)==false);
        assertTrue(parameterList.get(0).getValue(1)==false);
        assertTrue(parameterList.get(0).getValue(2)==true);
        assertTrue(parameterList.get(0).getKey()==0);
        assertTrue(parameterList.get(1).getValue(0)==true);
        assertTrue(parameterList.get(1).getValue(1)==true);
        assertTrue(parameterList.get(1).getValue(2)==true);
        assertTrue(parameterList.get(1).getKey()==2);
        assertTrue(parameterList.get(2).getValue(0)==false);
        assertTrue(parameterList.get(2).getValue(1)==false);
        assertTrue(parameterList.get(2).getValue(2)==false);
        assertTrue(parameterList.get(2).getKey()==1);
        assertTrue(parameterList.size()==3);
        
        // Test state restore
        parameterList.restore();
        
        assertTrue(parameterList.get(0).getValue(0)==true);
        assertTrue(parameterList.get(0).getValue(1)==false);
        assertTrue(parameterList.get(0).getValue(2)==true);
        assertTrue(parameterList.get(0).getKey()==0);
        assertTrue(parameterList.get(1).getValue(0)==false);
        assertTrue(parameterList.get(1).getValue(1)==true);
        assertTrue(parameterList.get(1).getValue(2)==false);
        assertTrue(parameterList.get(1).getKey()==1);
        assertTrue(parameterList.size()==2);
        

        
        // Test serialization
        parameterList.addNewParam(newParam);
        
        String xmlStr = parameterList.toXML();
        assertEquals(xmlStr,"<statenode id='null'>"
                + "Dimension: [3, 1], "
                + "Bounds: [false,true], "
                + "AvailableKeys: [], "
                + "NextKey: 3, "
                + "Parameters: [[true,false,true],[false,true,false],[false,false,false]], "
                + "ParameterKeys: [0,1,2]"
                + "</statenode>\n");
        
        // Test deserialization
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        Document doc = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xmlStr.getBytes()));
        doc.normalize();
        NodeList nodes = doc.getElementsByTagName("*");
        org.w3c.dom.Node docNode = nodes.item(0);
        
        BooleanParameterList newParameterList = new BooleanParameterList();
        newParameterList.initAndValidate();
        newParameterList.fromXML(docNode);
        
        assertTrue(newParameterList.get(0).getValue(0)==true);
        assertTrue(newParameterList.get(0).getValue(1)==false);
        assertTrue(newParameterList.get(0).getValue(2)==true);
        assertTrue(newParameterList.get(0).getKey()==0);
        assertTrue(newParameterList.get(1).getValue(0)==false);
        assertTrue(newParameterList.get(1).getValue(1)==true);
        assertTrue(newParameterList.get(1).getValue(2)==false);
        assertTrue(newParameterList.get(1).getKey()==1);
        assertTrue(newParameterList.get(2).getValue(0)==false);
        assertTrue(newParameterList.get(2).getValue(1)==false);
        assertTrue(newParameterList.get(2).getValue(2)==false);
        assertTrue(newParameterList.get(2).getKey()==2);
        assertTrue(newParameterList.size()==3);
        
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