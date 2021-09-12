package test.beast.app;

import org.junit.Test;

import beast.app.inputeditor.BeautiConnector;
import beast.app.inputeditor.BeautiSubTemplate;
import beast.base.inference.parameter.RealParameter;
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
		String xml = t.xMLInput.get().replaceAll("\\s+", " ");
		assertEquals("<![CDATA[ <parameter id=\"kappa\" value=\"1.0\"/> ]]>", xml);

		// minimal template + connector
		t = new BeautiSubTemplate();
		t.initByName("class",RealParameter.class.getName(),
				"mainid", "kappa",
				"value", "<state idref='thestate'><parameter id='kappa' value='1.0'/></state>"
				);
		assertEquals(1, t.connectorsInput.get().size());
		BeautiConnector c = t.connectorsInput.get().get(0);
		assertEquals("kappa", c.sourceIDInput.get());
		assertEquals("thestate", c.targetIDInput.get());
		assertEquals("parameter", c.inputNameInput.get());
		assertEquals(null, c.conditionInput.get());
		xml = t.xMLInput.get().replaceAll("\\s+", " ");
		assertEquals("<![CDATA[ <parameter id=\"kappa\" value=\"1.0\"/> ]]>", xml);

		// minimal template + connector + name
		t = new BeautiSubTemplate();
		t.initByName("class",RealParameter.class.getName(),
				"mainid", "kappa",
				"value", "<state idref='thestate'><parameter id='kappa' name='stateNode' value='1.0'/></state>"
				);
		assertEquals(1, t.connectorsInput.get().size());
		c = t.connectorsInput.get().get(0);
		assertEquals("kappa", c.sourceIDInput.get());
		assertEquals("thestate", c.targetIDInput.get());
		assertEquals("stateNode", c.inputNameInput.get());
		assertEquals(null, c.conditionInput.get());
		xml = t.xMLInput.get().replaceAll("\\s+", " ");
		assertEquals("<![CDATA[ <parameter id=\"kappa\" name=\"stateNode\" value=\"1.0\"/> ]]>", xml);

		// minimal template + connector + name + condition
		t = new BeautiSubTemplate();
		t.initByName("class",RealParameter.class.getName(),
				"mainid", "kappa",
				"value", "<state idref='thestate'><parameter id='kappa' name='stateNode' value='1.0' beauti:if='kappa/estimate=true'/></state>"
				);
		assertEquals(1, t.connectorsInput.get().size());
		c = t.connectorsInput.get().get(0);
		assertEquals("kappa", c.sourceIDInput.get());
		assertEquals("thestate", c.targetIDInput.get());
		assertEquals("stateNode", c.inputNameInput.get());
		assertEquals("kappa/estimate=true", c.conditionInput.get());
		xml = t.xMLInput.get().replaceAll("\\s+", " ");
		assertEquals("<![CDATA[ <parameter id=\"kappa\" name=\"stateNode\" value=\"1.0\"/> ]]>", xml);
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
		assertEquals("kappa", c.sourceIDInput.get());
		assertEquals("thestate", c.targetIDInput.get());
		assertEquals("stateNode", c.inputNameInput.get());
		assertEquals("kappa/estimate=true", c.conditionInput.get());
		String xml = t.xMLInput.get();
		assertEquals("<![CDATA[\n<parameter id=\"kappa\" name=\"stateNode\" value=\"1.0\"/>\n]]>", xml);
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
		assertEquals("kappa", c.sourceIDInput.get());
		assertEquals("thestate", c.targetIDInput.get());
		assertEquals("stateNode", c.inputNameInput.get());
		assertEquals("kappa/estimate=true", c.conditionInput.get());
		c = t.connectorsInput.get().get(1);
		assertEquals("gamma", c.sourceIDInput.get());
		assertEquals("thestate", c.targetIDInput.get());
		assertEquals("parameter", c.inputNameInput.get());
		assertEquals("kappa/estimate=true", c.conditionInput.get());
		String xml = t.xMLInput.get();
		assertEquals("<![CDATA[\n"+
"<parameter id=\"kappa\" name=\"stateNode\" value=\"1.0\"/>\n"+
"<parameter id=\"gamma\" value=\"3.0\"/>\n"+
"]]>", xml);

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
		assertEquals("kappa", c.sourceIDInput.get());
		assertEquals("thestate", c.targetIDInput.get());
		assertEquals("stateNode", c.inputNameInput.get());
		assertEquals("kappa/estimate=true", c.conditionInput.get());
		c = t.connectorsInput.get().get(1);
		assertEquals("gamma", c.sourceIDInput.get());
		assertEquals("thestate", c.targetIDInput.get());
		assertEquals("parameter", c.inputNameInput.get());
		assertEquals("kappa/estimate=true", c.conditionInput.get());
		xml = t.xMLInput.get();
		assertEquals("<![CDATA[\n"+
"<parameter id=\"kappa\" name=\"stateNode\" value=\"1.0\"/>\n"+
"]]>", xml);

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
		assertEquals("kappa", c.sourceIDInput.get());
		assertEquals("thestate", c.targetIDInput.get());
		assertEquals("stateNode", c.inputNameInput.get());
		assertEquals("kappa/estimate=true", c.conditionInput.get());
		c = t.connectorsInput.get().get(0);
		assertEquals("gamma", c.sourceIDInput.get());
		assertEquals("thestate", c.targetIDInput.get());
		assertEquals("parameter", c.inputNameInput.get());
		assertEquals("kappa/estimate=true", c.conditionInput.get());
		xml = t.xMLInput.get();
		assertEquals("<![CDATA[\n"+
"<parameter id=\"kappa\" name=\"stateNode\" value=\"1.0\"/>\n"+
"]]>", xml);
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
		assertEquals("kappa", c.sourceIDInput.get());
		assertEquals("thestate", c.targetIDInput.get());
		assertEquals("stateNode", c.inputNameInput.get());
		assertEquals("kappa/estimate=true", c.conditionInput.get());
		c = t.connectorsInput.get().get(1);
		assertEquals("gamma", c.sourceIDInput.get());
		assertEquals("thestate", c.targetIDInput.get());
		assertEquals("parameter", c.inputNameInput.get());
		assertEquals("kappa/estimate=true", c.conditionInput.get());
		c = t.connectorsInput.get().get(2);
		assertEquals("kappa", c.sourceIDInput.get());
		assertEquals("tracer", c.targetIDInput.get());
		assertEquals("log", c.inputNameInput.get());
		assertEquals("gamma/estimate=true", c.conditionInput.get());
		c = t.connectorsInput.get().get(3);
		assertEquals("gamma", c.sourceIDInput.get());
		assertEquals("tracer", c.targetIDInput.get());
		assertEquals("log", c.inputNameInput.get());
		assertEquals("gamma/estimate=true", c.conditionInput.get());
		String xml = t.xMLInput.get();
		assertEquals("<![CDATA[      <parameter id=\"kappa\" name=\"stateNode\" value=\"1.0\"/>    <parameter id=\"gamma\" value=\"3.0\"/>              ]]>"
, xml);
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
		assertEquals("kappa", c.sourceIDInput.get());
		assertEquals("thestate", c.targetIDInput.get());
		assertEquals("stateNode", c.inputNameInput.get());
		assertEquals("kappa/estimate=true", c.conditionInput.get());
		c = t.connectorsInput.get().get(1);
		assertEquals("gamma", c.sourceIDInput.get());
		assertEquals("thestate", c.targetIDInput.get());
		assertEquals("parameter", c.inputNameInput.get());
		assertEquals("gamma/estimate=true", c.conditionInput.get());
		c = t.connectorsInput.get().get(2);
		assertEquals("kappa", c.sourceIDInput.get());
		assertEquals("tracer", c.targetIDInput.get());
		assertEquals("log", c.inputNameInput.get());
		assertEquals("kappa/estimate=true", c.conditionInput.get());
		c = t.connectorsInput.get().get(3);
		assertEquals("gamma", c.sourceIDInput.get());
		assertEquals("tracer", c.targetIDInput.get());
		assertEquals("log", c.inputNameInput.get());
		assertEquals("gamma/estimate=true", c.conditionInput.get());
		String xml = t.xMLInput.get().replaceAll("\\s+", " ");
		assertEquals("<![CDATA[ <parameter id=\"kappa\" name=\"stateNode\" value=\"1.0\"/> <parameter id=\"gamma\" value=\"3.0\"/> ]]>"
, xml);
	}
}
