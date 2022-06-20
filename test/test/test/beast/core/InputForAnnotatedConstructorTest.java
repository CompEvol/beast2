package test.beast.core;

import java.util.*;

import org.json.JSONException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beast.base.parser.JSONParser;
import beast.base.parser.JSONParserException;
import beast.base.parser.JSONProducer;
import beast.base.parser.XMLParser;
import beast.base.parser.XMLParserException;
import beast.base.parser.XMLProducer;
import beast.pkgmgmt.BEASTClassLoader;

import test.beast.core.PrimitiveBeastObject.Enumeration;
import test.beast.core.PrimitiveBeastObject.InnerClass;
import test.beast.core.PrimitiveInterface.InterfaceInnerClass;

public class InputForAnnotatedConstructorTest  {

	
	@Test
	public void testPrimitiveInput() {
		PrimitiveBeastObject o = new PrimitiveBeastObject(1, Enumeration.one);
		
		// test type
		List<Input<?>> inputs = o.listInputs();
		assertEquals(7, inputs.size());
		Input<?> input = null;
		for (Input i : inputs) {
			if (i.getName().equals("i")) {
				input = i;
				break;
			}
		}
		assertEquals("int", input.getType().toGenericString());
		
		// test value
		assertEquals(1, input.get());
		
		// test setting a value
		input.set(2);
		assertEquals(2, input.get());
		
		// test setting a value by primitive
		int i = 3;
		input.set(i);
		assertEquals(3, input.get());
		
		// test setting a value by object
		Integer j = 4;
		input.set(j);
		assertEquals(4, input.get());

		Short k = 5;
		// fails -- don't know how to convert from Short to int
//		input.set(k);
//		assertEquals(5, input.get());
	}
	
	@Test
	public void testEnumerationInput() {
		PrimitiveBeastObject o = new PrimitiveBeastObject(1, Enumeration.one);
		assertEquals("one", o.getE().toString());
		
		o.setE(Enumeration.two);
		assertEquals("two", o.getE().toString());

		PrimitiveBeastObject o2 = new PrimitiveBeastObject(0, Enumeration.two);
		assertEquals("two", o2.getE().toString());
	}
	
	@Test
	public void testArrayInput() throws JSONParserException, JSONException {
		double [] a = new double[] {1.0, 3.0};
		PrimitiveBeastObject o3 = new PrimitiveBeastObject(a);
		assertEquals(1.0, o3.getA()[0]);
		assertEquals(3.0, o3.getA()[1]);
		a[0] = 5; 
		a[1] = 9;
		assertEquals(1.0, o3.getA()[0]);
		assertEquals(3.0, o3.getA()[1]);
		o3.setA(a);
		assertEquals(5.0, o3.getA()[0]);
		assertEquals(9.0, o3.getA()[1]);
	}
	
	
	@Test
	public void testInnerClass() throws JSONParserException, JSONException {
		double [] a = new double[] {1.0, 3.0};
		InnerClass inner = new PrimitiveBeastObject().new InnerClass(a);
		assertEquals(1.0, inner.getA()[0]);
		assertEquals(3.0, inner.getA()[1]);	
	}
	
	@Test
	public void testInterfaceInnerClass() throws JSONException, XMLParserException {
		InterfaceInnerClass inner = new PrimitiveInterface.InterfaceInnerClass(3);
		assertEquals(3, inner.getI());

	}

	
	@Test
	public void testStringArrayInput() throws JSONException, XMLParserException {
		String [] s = new String[] {"John", "Peter"};
		PrimitiveBeastObject o3 = new PrimitiveBeastObject(s);
		assertEquals("John", o3.getS()[0]);
		assertEquals("Peter", o3.getS()[1]);

		
		String json = "{id: testObject, spec: test.beast.core.PrimitiveBeastObject, s: [John, Peter]}";
		JSONParser parser = new JSONParser();
		List<Object> o = parser.parseBareFragment(json, false);
		PrimitiveBeastObject po = (PrimitiveBeastObject) o.get(0);
		assertEquals("John", po.getS()[0]);
		assertEquals("Peter", po.getS()[1]);

		JSONProducer producer = new JSONProducer();
        String json2 = producer.toJSON(po);
		json2 = json2.substring(json2.indexOf('[') + 1, json2.lastIndexOf(']')).trim();
		assertEqualJSON("{id: \"testObject\", spec: \"test.beast.core.PrimitiveBeastObject\", i: \"0\", e: \"none\", s: [John, Peter] }", json2);

	}
	
	@Test
	public void testBEASTObjectInput() throws JSONException, XMLParserException {
		double [] a = new double[] {1.0, 3.0};
		PrimitiveBeastObject inner0 = new PrimitiveBeastObject(a);
		a = new double[] {3.0, 4.0};
		PrimitiveBeastObject inner1 = new PrimitiveBeastObject(a);
		PrimitiveBeastObject [] array = new PrimitiveBeastObject[]{inner0, inner1};

		// can we create the object?
		PrimitiveBeastObject pi3 = new PrimitiveBeastObject(array);
		
		// can we produce String json?
		String json = "{spec: \"test.beast.core.PrimitiveBeastObject\", i: \"0\", e: \"none\",\n" + 
		"	 p: [\n" + 
		"		 {spec: \"test.beast.core.PrimitiveBeastObject\", i: \"0\", e: \"none\", a: [1.0, 3.0] }, \n" +  
		"		 {spec: \"test.beast.core.PrimitiveBeastObject\", i: \"0\", e: \"none\", a: [3.0, 4.0] }\n" + 
		"	    ]\n" + 
		"	}";
		JSONProducer producer = new JSONProducer();
        String json2 = producer.toJSON(pi3);
		json2 = json2.substring(json2.indexOf('[') + 1, json2.lastIndexOf(']')).trim();
        System.out.println(json2);
        assertEqualJSON(json, json2);

		
		String json3 = "{spec: \"test.beast.core.PrimitiveBeastObject\",\n" + 
		"	 p: [\n" + 
		"		 {spec: \"test.beast.core.PrimitiveBeastObject\", a: [1.0, 3.0] }, \n" +  
		"		 {spec: \"test.beast.core.PrimitiveBeastObject\", a: [3.0, 4.0] }\n" + 
		"	    ]\n" + 
		"	}";
        // can we parse String json?
		JSONParser parser = new JSONParser();
		List<Object> o = parser.parseBareFragment(json3, false);
		pi3 = (PrimitiveBeastObject) o.get(0);		
        json2 = producer.toJSON(pi3);
		json2 = json2.substring(json2.indexOf('[') + 1, json2.lastIndexOf(']')).trim();
		assertEqualJSON(json, json2);
	}

	
	@Test
	public void testXML() throws XMLParserException {
		// register test classes
		BEASTClassLoader.addService(BEASTInterface.class.getName(), PrimitiveBeastObject.class.getName(), "BEAST.base");
		BEASTClassLoader.addService(BEASTInterface.class.getName(), PrimitiveBeastObject.InnerClass.class.getName(), "BEAST.base");
		BEASTClassLoader.addService(BEASTInterface.class.getName(), PrimitiveInterface.InterfaceInnerClass.class.getName(), "BEAST.base");
		
		
		// test enum & int c'tor
		String xml = "<input id='testObject' spec='test.beast.core.PrimitiveBeastObject' e='two' i='3'/>";
		String  xml2;
		XMLParser parser = new XMLParser();
		XMLProducer producer = new XMLProducer();
		Object o;
		PrimitiveBeastObject po;
		
		o = parser.parseBareFragment(xml, false);
		po = (PrimitiveBeastObject) o;

		assertEquals(3, po.getI());
		assertEquals(Enumeration.two, po.getE());
		
		xml2 = producer.toRawXML(po).trim();
		assertEqualXML(xml, xml2);

		// test int c'tor and default value
		xml = "<input id='testObject' spec='test.beast.core.PrimitiveBeastObject' i='2'/>";
		o = parser.parseBareFragment(xml, false);
		po = (PrimitiveBeastObject) o;
		assertEquals(2, po.getI());
		assertEquals(Enumeration.one, po.getE());
		
		po.setE(Enumeration.one);
		xml2 = producer.toRawXML(po).trim();
		assertEquals(xml, xml2);

		// test primitive array c'tor
		xml = "<input id='testObject' spec='test.beast.core.PrimitiveBeastObject' a='1.0 15.0 17.0'/>";
		o = parser.parseBareFragment(xml, false);
		po = (PrimitiveBeastObject) o;
		assertEquals(3, po.getA().length);
		assertEquals(1.0, po.getA()[0]);
		assertEquals(15.0, po.getA()[1]);
		assertEquals(17.0, po.getA()[2]);

		xml2 = producer.toRawXML(po).trim();
		xml = "<input id='testObject' spec='test.beast.core.PrimitiveBeastObject' a=\"1.0 15.0 17.0\" e='none' i='0'/>";
		assertEqualXML(xml, xml2);


		// test object array c'tor
		xml = "<input id='testObject' spec='test.beast.core.PrimitiveBeastObject' b='1.0 15.0 17.0'/>";
		o = parser.parseBareFragment(xml, false);
		po = (PrimitiveBeastObject) o;
		assertEquals(3, po.getB().length);
		assertEquals(1.0, po.getB()[0]);
		assertEquals(15.0, po.getB()[1]);
		assertEquals(17.0, po.getB()[2]);

		xml2 = producer.toRawXML(po).trim();
		xml = "<input id='testObject' spec='test.beast.core.PrimitiveBeastObject' b=\"1.0 15.0 17.0\" e='none' i='0'/>";
		assertEqualXML(xml, xml2);
		
		// test inner class inside base class
		xml = "<input id='testObject' spec='test.beast.core.PrimitiveBeastObject$InnerClass' a='2.0 5.0 7.0'/>";
		o = parser.parseBareFragment(xml, false);
		po = (PrimitiveBeastObject) o;
		assertEquals(3, po.getA().length);
		assertEquals(2.0, po.getA()[0]);
		assertEquals(5.0, po.getA()[1]);
		assertEquals(7.0, po.getA()[2]);

		xml2 = producer.toRawXML(po).trim();
		xml = "<input id='testObject' spec='test.beast.core.PrimitiveBeastObject$InnerClass' a=\"2.0 5.0 7.0\" e='none' i='0'/>";
		assertEqualXML(xml, xml2);

		// test inner class inside interface 
		xml = "<input id='testObject' spec='test.beast.core.PrimitiveInterface$InterfaceInnerClass' i='7'/>";
		o = parser.parseBareFragment(xml, false);
		InterfaceInnerClass iio = (InterfaceInnerClass) o;
		assertEquals(7, iio.getI());

		xml2 = producer.toRawXML(iio).trim();
		xml = "<input id='testObject' spec='test.beast.core.PrimitiveInterface$InterfaceInnerClass' i='7'/>";
		assertEqualXML(xml, xml2);
	}	

	@Test
	public void testJSON() throws JSONParserException, JSONException {
		List<Object> o;
		PrimitiveBeastObject po = null;
		
		// test c'tor with primitive int and enum
		String json = "{id: testObject, spec: test.beast.core.PrimitiveBeastObject, i: 3, e: two }";
		JSONParser parser = new JSONParser();
		o = parser.parseBareFragment(json, false);
		po = (PrimitiveBeastObject) o.get(0);
		assertEquals(3, po.getI());
		assertEquals(Enumeration.two, po.getE());
		
		JSONProducer producer = new JSONProducer();
		String json2 = producer.toJSON(po);
		json2 = json2.substring(json2.indexOf('[') + 1, json2.lastIndexOf(']')).trim();
		assertEqualJSON("{id: \"testObject\", spec: \"test.beast.core.PrimitiveBeastObject\", i: \"3\", e: \"two\" }", json2);
		
		// test int c'tor and default value
		json = "{id: testObject, spec: test.beast.core.PrimitiveBeastObject, i: 2}";
		parser = new JSONParser();
		o = parser.parseBareFragment(json, false);
		po = (PrimitiveBeastObject) o.get(0);
		assertEquals(2, po.getI());
		assertEquals(Enumeration.one, po.getE());
		
		json2 = producer.toJSON(po);
		json2 = json2.substring(json2.indexOf('[') + 1, json2.lastIndexOf(']')).trim();
		assertEqualJSON("{id: \"testObject\", spec: \"test.beast.core.PrimitiveBeastObject\", i: \"2\" }", json2);

	
		// test array of primitive values
		json = "{id: testObject, spec: test.beast.core.PrimitiveBeastObject, a: [1.0, 15.0, 17.0]}";
		parser = new JSONParser();
		o = parser.parseBareFragment(json, false);
		po = (PrimitiveBeastObject) o.get(0);
		assertEquals(3, po.getA().length);
		assertEquals(1.0, po.getA()[0]);
		assertEquals(15.0, po.getA()[1]);
		assertEquals(17.0, po.getA()[2]);

		json2 = producer.toJSON(po);
		json2 = json2.substring(json2.indexOf('[') + 1, json2.lastIndexOf(']')).trim();
		assertEqualJSON("{id: \"testObject\", spec: \"test.beast.core.PrimitiveBeastObject\", i: \"0\", e: \"none\", a: [1.0, 15.0, 17.0] }", json2);

		// test array of object values
		json = "{id: testObject, spec: test.beast.core.PrimitiveBeastObject, b: [1.0, 15.0, 17.0]}";
		parser = new JSONParser();
		o = parser.parseBareFragment(json, false);
		po = (PrimitiveBeastObject) o.get(0);
		assertEquals(3, po.getB().length);
		assertEquals(1.0, po.getB()[0]);
		assertEquals(15.0, po.getB()[1]);
		assertEquals(17.0, po.getB()[2]);

		json2 = producer.toJSON(po);
		json2 = json2.substring(json2.indexOf('[') + 1, json2.lastIndexOf(']')).trim();
		assertEqualJSON("{id: \"testObject\", spec: \"test.beast.core.PrimitiveBeastObject\", i: \"0\", e: \"none\", b: [1.0, 15.0, 17.0] }", json2);
		
		// test inner class inside base class
		InnerClass io = null;
		json = "{spec: \"test.beast.core.PrimitiveBeastObject$InnerClass\", a: [1.0, 3.0] }";
		parser = new JSONParser();
		o = parser.parseBareFragment(json, false);
		io = (InnerClass) o.get(0);
		assertEquals(1.0, io.getA()[0]);
		assertEquals(3.0, io.getA()[1]);	
		json2 = producer.toJSON(io);
		json2 = json2.substring(json2.indexOf('[') + 1, json2.lastIndexOf(']')).trim();
		assertEqualJSON("{spec: \"test.beast.core.PrimitiveBeastObject$InnerClass\", e: \"none\", i: \"0\", a: [1.0, 3.0] }", json2);

		// test inner class inside interface
		InterfaceInnerClass iio = null;
		json = "{spec: \"test.beast.core.PrimitiveInterface$InterfaceInnerClass\", i: 5 }";
		parser = new JSONParser();
		o = parser.parseBareFragment(json, false);
		iio = (InterfaceInnerClass) o.get(0);
		assertEquals(5, iio.getI());
		json2 = producer.toJSON(iio);
		json2 = json2.substring(json2.indexOf('[') + 1, json2.lastIndexOf(']')).trim();
		assertEqualJSON("{spec: \"test.beast.core.PrimitiveInterface$InterfaceInnerClass\", i: \"5\" }", json2);
	}	
	
	void assertEqualJSON(String expected, String obtained) {
		if (expected.equals(obtained)) {
			return;
		}
		Set<String> eSet = new LinkedHashSet<>();
		for (String str : expected.split(",")) {
			eSet.add(str.replaceAll("[{}]","").trim());
		}
		Set<String> oSet = new LinkedHashSet<>();
		for (String str : obtained.split(",")) {
			oSet.add(str.replaceAll("[{}]","").trim());
		}
		if (eSet.size() == oSet.size()) {
			eSet.removeAll(oSet);
			if (eSet.isEmpty()) {
				return;
			}
		}
		assertEquals(expected, obtained);
	}
	
	void assertEqualXML(String expected, String obtained) {
		if (expected.equals(obtained)) {
			return;
		}
		// check element names match
		String prefix1 = expected.substring(0, expected.indexOf(' '));
		String prefix2 = obtained.substring(0, expected.indexOf(' '));
		if (prefix1.equals(prefix2)) {
			expected = expected.substring(prefix1.length(), expected.length() - 2).trim();
			obtained = obtained.substring(prefix1.length(), obtained.length() - 2).trim();
			// check attributes match
			String [] strs = expected.split("=");
			Set<String> eSet = new LinkedHashSet<>();
			for (int i = 0; i < strs.length - 1; i++) {
				String name = i == 0 ? strs[i] : strs[i].substring(strs[i].lastIndexOf(' ')+1); 
				String value = i == strs.length - 2 ? strs[i+1] : strs[i+1].substring(0, strs[i+1].lastIndexOf(' ') - 1);
				eSet.add(name + " = " + value);
			}
			strs = expected.split("=");
			Set<String> oSet = new LinkedHashSet<>();
			for (int i = 0; i < strs.length - 1; i++) {
				String name = i == 0 ? strs[i] : strs[i].substring(strs[i].lastIndexOf(' ')+1); 
				String value = i == strs.length - 2 ? strs[i+1] : strs[i+1].substring(0, strs[i+1].lastIndexOf(' ') - 1);
				oSet.add(name + " = " + value);
			}
			if (eSet.size() == oSet.size()) {
				eSet.removeAll(oSet);
				if (eSet.isEmpty()) {
					return;
				}
			}
		}
		assertEquals(expected, obtained);
	}
}
