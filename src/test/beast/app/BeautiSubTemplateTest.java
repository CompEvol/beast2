package test.beast.app;

import org.junit.Test;

import beast.app.beauti.BeautiConnector;
import beast.app.beauti.BeautiSubTemplate;
import beast.core.parameter.RealParameter;
import junit.framework.TestCase;

public class BeautiSubTemplateTest extends TestCase {
	
	@Test
	public void testBeautiSubTemplate() throws Exception {
		// minimal template
		BeautiSubTemplate t = new BeautiSubTemplate();
		t.initByName("class",RealParameter.class.getName(),
				"mainid", "kappa",
				"value", "<parameter id='kappa' value='1.0'/>"
				);
		assertEquals(0, t.connectorsInput.get().size());
		String sXML = t.sXMLInput.get().replaceAll("\\s+", " ");
		assertEquals("<![CDATA[ <parameter id=\"kappa\" value=\"1.0\"/> ]]>", sXML);

		// minimal template + connector
		t = new BeautiSubTemplate();
		t.initByName("class",RealParameter.class.getName(),
				"mainid", "kappa",
				"value", "<state idref='thestate'><parameter id='kappa' value='1.0'/></state>"
				);
		assertEquals(1, t.connectorsInput.get().size());
		BeautiConnector c = t.connectorsInput.get().get(0);
		assertEquals("kappa", c.sSourceIDInput.get());
		assertEquals("thestate", c.sTargetIDInput.get());
		assertEquals("parameter", c.sInputNameInput.get());
		assertEquals(null, c.sConditionInput.get());
		sXML = t.sXMLInput.get().replaceAll("\\s+", " ");
		assertEquals("<![CDATA[ <parameter id=\"kappa\" value=\"1.0\"/> ]]>", sXML);

		// minimal template + connector + name
		t = new BeautiSubTemplate();
		t.initByName("class",RealParameter.class.getName(),
				"mainid", "kappa",
				"value", "<state idref='thestate'><parameter id='kappa' name='stateNode' value='1.0'/></state>"
				);
		assertEquals(1, t.connectorsInput.get().size());
		c = t.connectorsInput.get().get(0);
		assertEquals("kappa", c.sSourceIDInput.get());
		assertEquals("thestate", c.sTargetIDInput.get());
		assertEquals("stateNode", c.sInputNameInput.get());
		assertEquals(null, c.sConditionInput.get());
		sXML = t.sXMLInput.get().replaceAll("\\s+", " ");
		assertEquals("<![CDATA[ <parameter id=\"kappa\" name=\"stateNode\" value=\"1.0\"/> ]]>", sXML);

		// minimal template + connector + name + condition
		t = new BeautiSubTemplate();
		t.initByName("class",RealParameter.class.getName(),
				"mainid", "kappa",
				"value", "<state idref='thestate'><parameter id='kappa' name='stateNode' value='1.0' beauti:if='kappa/estimate=true'/></state>"
				);
		assertEquals(1, t.connectorsInput.get().size());
		c = t.connectorsInput.get().get(0);
		assertEquals("kappa", c.sSourceIDInput.get());
		assertEquals("thestate", c.sTargetIDInput.get());
		assertEquals("stateNode", c.sInputNameInput.get());
		assertEquals("kappa/estimate=true", c.sConditionInput.get());
		sXML = t.sXMLInput.get().replaceAll("\\s+", " ");
		assertEquals("<![CDATA[ <parameter id=\"kappa\" name=\"stateNode\" value=\"1.0\"/> ]]>", sXML);
	}

	@Test
	public void testBeautiSubTemplateIfElement() throws Exception {
		// minimal template
		BeautiSubTemplate t = new BeautiSubTemplate();

		// minimal template + connector + name + condition
		t.initByName("class",RealParameter.class.getName(),
				"mainid", "kappa",
				"value", "<state idref='thestate'><if cond='kappa/estimate=true'><parameter id='kappa' name='stateNode' value='1.0'/></if></state>"
				);
		assertEquals(1, t.connectorsInput.get().size());
		BeautiConnector c = t.connectorsInput.get().get(0);
		assertEquals("kappa", c.sSourceIDInput.get());
		assertEquals("thestate", c.sTargetIDInput.get());
		assertEquals("stateNode", c.sInputNameInput.get());
		assertEquals("kappa/estimate=true", c.sConditionInput.get());
		String sXML = t.sXMLInput.get();
		assertEquals("<![CDATA[\n<parameter id=\"kappa\" name=\"stateNode\" value=\"1.0\"/>\n]]>", sXML);
	}
	
	@Test
	public void testBeautiSubTemplateIfMultipleElement() throws Exception {
		BeautiSubTemplate t = new BeautiSubTemplate();
		// minimal template + connector + name + condition for 2 entries
		t.initByName("class",RealParameter.class.getName(),
				"mainid", "kappa",
				"value", "<state idref='thestate'>"
						+ "<if cond='kappa/estimate=true'>"
						+ "<parameter id='kappa' name='stateNode' value='1.0'/>" 
						+ "<parameter id='gamma' value='3.0'/>" 
						+ "</if></state>"
				);
		assertEquals(2, t.connectorsInput.get().size());
		BeautiConnector c = t.connectorsInput.get().get(0);
		assertEquals("kappa", c.sSourceIDInput.get());
		assertEquals("thestate", c.sTargetIDInput.get());
		assertEquals("stateNode", c.sInputNameInput.get());
		assertEquals("kappa/estimate=true", c.sConditionInput.get());
		c = t.connectorsInput.get().get(1);
		assertEquals("gamma", c.sSourceIDInput.get());
		assertEquals("thestate", c.sTargetIDInput.get());
		assertEquals("parameter", c.sInputNameInput.get());
		assertEquals("kappa/estimate=true", c.sConditionInput.get());
		String sXML = t.sXMLInput.get();
		assertEquals("<![CDATA[\n"+
"<parameter id=\"kappa\" name=\"stateNode\" value=\"1.0\"/>\n"+
"<parameter id=\"gamma\" value=\"3.0\"/>\n"+
"]]>", sXML);

		t = new BeautiSubTemplate();
		// minimal template + connector + name + condition for 2 entries
		t.initByName("class",RealParameter.class.getName(),
				"mainid", "kappa",
				"value", "<state idref='thestate'>"
						+ "<if cond='kappa/estimate=true'>"
						+ "<parameter id='kappa' name='stateNode' value='1.0'/>" 
						+ "<parameter idref='gamma'/>" 
						+ "</if></state>"
				);
		assertEquals(2, t.connectorsInput.get().size());
		c = t.connectorsInput.get().get(0);
		assertEquals("kappa", c.sSourceIDInput.get());
		assertEquals("thestate", c.sTargetIDInput.get());
		assertEquals("stateNode", c.sInputNameInput.get());
		assertEquals("kappa/estimate=true", c.sConditionInput.get());
		c = t.connectorsInput.get().get(1);
		assertEquals("gamma", c.sSourceIDInput.get());
		assertEquals("thestate", c.sTargetIDInput.get());
		assertEquals("parameter", c.sInputNameInput.get());
		assertEquals("kappa/estimate=true", c.sConditionInput.get());
		sXML = t.sXMLInput.get();
		assertEquals("<![CDATA[\n"+
"<parameter id=\"kappa\" name=\"stateNode\" value=\"1.0\"/>\n"+
"]]>", sXML);

		t = new BeautiSubTemplate();
		// minimal template + connector + name + condition for 2 entries
		t.initByName("class",RealParameter.class.getName(),
				"mainid", "kappa",
				"value", "<state idref='thestate'>"
						+ "<if cond='kappa/estimate=true'>"
						+ "<parameter idref='gamma'/>" 
						+ "<parameter id='kappa' name='stateNode' value='1.0'/>" 
						+ "</if></state>"
				);
		assertEquals(2, t.connectorsInput.get().size());
		c = t.connectorsInput.get().get(1);
		assertEquals("kappa", c.sSourceIDInput.get());
		assertEquals("thestate", c.sTargetIDInput.get());
		assertEquals("stateNode", c.sInputNameInput.get());
		assertEquals("kappa/estimate=true", c.sConditionInput.get());
		c = t.connectorsInput.get().get(0);
		assertEquals("gamma", c.sSourceIDInput.get());
		assertEquals("thestate", c.sTargetIDInput.get());
		assertEquals("parameter", c.sInputNameInput.get());
		assertEquals("kappa/estimate=true", c.sConditionInput.get());
		sXML = t.sXMLInput.get();
		assertEquals("<![CDATA[\n"+
"<parameter id=\"kappa\" name=\"stateNode\" value=\"1.0\"/>\n"+
"]]>", sXML);
	}
	
	@Test
	public void testBeautiSubTemplateCombined() throws Exception {
		BeautiSubTemplate t = new BeautiSubTemplate();
		// minimal template + connector + name + condition for 2 entries
		t.initByName("class",RealParameter.class.getName(),
				"mainid", "kappa",
				"value", "<state idref='thestate'>"
						+ "  <if cond='kappa/estimate=true'>"
						+ "    <parameter id='kappa' name='stateNode' value='1.0'/>" 
						+ "    <parameter id='gamma' value='3.0'/>" 
						+ "  </if>"
						+ "</state>"
						+ "<logger idref='tracer'>"
						+ "  <if cond='gamma/estimate=true'>"
						+ "    <log idref='kappa'/>"
						+ "    <log idref='gamma'/>"
						+ "  </if>"
						+ "</logger>"
				);
		assertEquals(4, t.connectorsInput.get().size());
		BeautiConnector c = t.connectorsInput.get().get(0);
		assertEquals("kappa", c.sSourceIDInput.get());
		assertEquals("thestate", c.sTargetIDInput.get());
		assertEquals("stateNode", c.sInputNameInput.get());
		assertEquals("kappa/estimate=true", c.sConditionInput.get());
		c = t.connectorsInput.get().get(1);
		assertEquals("gamma", c.sSourceIDInput.get());
		assertEquals("thestate", c.sTargetIDInput.get());
		assertEquals("parameter", c.sInputNameInput.get());
		assertEquals("kappa/estimate=true", c.sConditionInput.get());
		c = t.connectorsInput.get().get(2);
		assertEquals("kappa", c.sSourceIDInput.get());
		assertEquals("tracer", c.sTargetIDInput.get());
		assertEquals("log", c.sInputNameInput.get());
		assertEquals("gamma/estimate=true", c.sConditionInput.get());
		c = t.connectorsInput.get().get(3);
		assertEquals("gamma", c.sSourceIDInput.get());
		assertEquals("tracer", c.sTargetIDInput.get());
		assertEquals("log", c.sInputNameInput.get());
		assertEquals("gamma/estimate=true", c.sConditionInput.get());
		String sXML = t.sXMLInput.get();
		assertEquals("<![CDATA[      <parameter id=\"kappa\" name=\"stateNode\" value=\"1.0\"/>    <parameter id=\"gamma\" value=\"3.0\"/>              ]]>"
, sXML);
	}

	@Test
	public void testBeautiSubTemplateCombined2() throws Exception {
		BeautiSubTemplate t = new BeautiSubTemplate();
		// minimal template + connector + name + condition for 2 entries
		t.initByName("class",RealParameter.class.getName(),
				"mainid", "kappa",
				"value", "<state idref='thestate'>"
						+ "    <parameter id='kappa' name='stateNode' value='1.0' beauti:if='kappa/estimate=true'/>" 
						+ "    <parameter id='gamma' value='3.0'  beauti:if='gamma/estimate=true'/>" 
						+ "</state>"
						+ "<logger idref='tracer'>"
						+ "    <log idref='kappa' beauti:if='kappa/estimate=true'/>"
						+ "    <log idref='gamma' beauti:if='gamma/estimate=true'/>"
						+ "</logger>"
				);
		assertEquals(4, t.connectorsInput.get().size());
		BeautiConnector c = t.connectorsInput.get().get(0);
		assertEquals("kappa", c.sSourceIDInput.get());
		assertEquals("thestate", c.sTargetIDInput.get());
		assertEquals("stateNode", c.sInputNameInput.get());
		assertEquals("kappa/estimate=true", c.sConditionInput.get());
		c = t.connectorsInput.get().get(1);
		assertEquals("gamma", c.sSourceIDInput.get());
		assertEquals("thestate", c.sTargetIDInput.get());
		assertEquals("parameter", c.sInputNameInput.get());
		assertEquals("gamma/estimate=true", c.sConditionInput.get());
		c = t.connectorsInput.get().get(2);
		assertEquals("kappa", c.sSourceIDInput.get());
		assertEquals("tracer", c.sTargetIDInput.get());
		assertEquals("log", c.sInputNameInput.get());
		assertEquals("kappa/estimate=true", c.sConditionInput.get());
		c = t.connectorsInput.get().get(3);
		assertEquals("gamma", c.sSourceIDInput.get());
		assertEquals("tracer", c.sTargetIDInput.get());
		assertEquals("log", c.sInputNameInput.get());
		assertEquals("gamma/estimate=true", c.sConditionInput.get());
		String sXML = t.sXMLInput.get().replaceAll("\\s+", " ");
		assertEquals("<![CDATA[ <parameter id=\"kappa\" name=\"stateNode\" value=\"1.0\"/> <parameter id=\"gamma\" value=\"3.0\"/> ]]>"
, sXML);
	}
}
