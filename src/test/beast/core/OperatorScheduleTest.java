package test.beast.core;


import org.junit.Test;

import beast.core.OperatorSchedule;
import beast.core.parameter.RealParameter;
import beast.evolution.operators.DeltaExchangeOperator;
import beast.evolution.operators.ScaleOperator;
import junit.framework.TestCase;

public class OperatorScheduleTest extends TestCase {
	
	RealParameter parameter = new RealParameter(new Double[] {1., 1., 1., 1.});
	
	OperatorSchedule setUpSchedule() {
		DeltaExchangeOperator operator1 = new DeltaExchangeOperator();
		operator1.setID("deltaOperator");
		operator1.initByName("parameter", parameter, "weight", 1.0);

		ScaleOperator operator2 = new ScaleOperator(); 
		operator2.setID("scaleOperator");
		operator2.initByName("parameter", parameter, "weight", 3.0);

		OperatorSchedule schedule = new OperatorSchedule();
		schedule.initAndValidate();
		schedule.addOperator(operator1);
		schedule.addOperator(operator2);
		return schedule;
	}
	
	
	/** test single schedule with 2 operators **/
	@Test
	public void testOperatorSchedulePlain() {
		double [] probs;
		OperatorSchedule schedule = setUpSchedule();
		// selectOperator() causes reweighting
		schedule.selectOperator();
		
		// weights have not changed
		probs = schedule.getCummulativeProbs();
		assertEquals(2, probs.length);
		assertEquals(2,  schedule.operators.size());
		assertEquals(1.0/4.0, probs[0], 1e-15);
		assertEquals(4.0/4.0, probs[1], 1e-15);
	}
	
	/** as testOperatorSchedulePlain() but with 1 nested schedule **/
	@Test
	public void testOperatorScheduleNestedByPercentage() {
		double [] probs;
		OperatorSchedule schedule = setUpSchedule();
		ScaleOperator operator3 = new ScaleOperator(); 
		operator3.setID("scaleOperator.Species");
		operator3.initByName("parameter", parameter, "weight", 20.0);
		OperatorSchedule subSchedule = new OperatorSchedule();
		subSchedule.initByName("operator", operator3, "weight", 20.0, "weightIsPercentage", true, "operatorPattern", "^.*\\.Species$");
		schedule.subschedulesInput.setValue(subSchedule, schedule);
		schedule.initAndValidate();
		schedule.addOperator(operator3);
		// selectOperator() causes reweighting
		schedule.selectOperator();

		probs = schedule.getCummulativeProbs();
		assertEquals(3, probs.length);
		assertEquals(3,  schedule.operators.size());
		assertEquals(1.0/5.0, probs[0], 1e-15);
		assertEquals(4.0/5.0, probs[1], 1e-15);
		assertEquals(5.0/5.0, probs[2], 1e-15);
	}
	
	/** as testOperatorScheduleNestedByPercentage() but with 2 nested operators **/
	@Test
	public void testOperatorScheduleNestedByPercentage2() {
		double [] probs;
		OperatorSchedule schedule = setUpSchedule();

		ScaleOperator operator3 = new ScaleOperator(); 
		operator3.setID("scaleOperator.Species");
		operator3.initByName("parameter", parameter, "weight", 20.0);
		OperatorSchedule subSchedule = new OperatorSchedule();
		subSchedule = new OperatorSchedule();
		ScaleOperator operator4 = new ScaleOperator(); 
		operator4.setID("scaleOperator2.Species");
		operator4.initByName("parameter", parameter, "weight", 20.0);
		subSchedule.initByName("operator", operator3, "operator", operator4, "weight", 20.0, "weightIsPercentage", true, "operatorPattern", "^.*\\.Species$");
		schedule.subschedulesInput.get().clear();
		schedule.subschedulesInput.setValue(subSchedule, schedule);
		schedule.initAndValidate();
		schedule.addOperator(operator3);
		schedule.addOperator(operator4);
		// selectOperator() causes reweighting
		schedule.selectOperator();

		probs = schedule.getCummulativeProbs();
		assertEquals(4, probs.length);
		assertEquals(4,  schedule.operators.size());
		assertEquals(1.0/5.0, probs[0], 1e-15);
		assertEquals(4.0/5.0, probs[1], 1e-15);
		assertEquals(4.5/5.0, probs[2], 1e-15);
		assertEquals(5.0/5.0, probs[3], 1e-15);
	}
	
	/** as testOperatorSchedulePlain() but with 1 nested schedule weighted by relative weight
	 * Also, make sure the operatorPattern matching works **/
	@Test
	public void testOperatorScheduleNestedByRelativeWeight() {
		double [] probs;
		OperatorSchedule schedule = setUpSchedule();
		ScaleOperator operator3 = new ScaleOperator(); 
		operator3.setID("scaleOperator.Species");
		operator3.initByName("parameter", parameter, "weight", 20.0);
		OperatorSchedule subSchedule = new OperatorSchedule();
		subSchedule.initByName(/*"operator", operator3,*/ "weight", 4.0, "operatorPattern", "^.*\\.Species$");
		schedule.subschedulesInput.setValue(subSchedule, schedule);
		schedule.initAndValidate();
		schedule.addOperator(operator3);
		// selectOperator() causes reweighting
		schedule.selectOperator();

		probs = schedule.getCummulativeProbs();
		assertEquals(3, probs.length);
		assertEquals(3,  schedule.operators.size());
		assertEquals(1.0/8.0, probs[0], 1e-15);
		assertEquals(4.0/8.0, probs[1], 1e-15);
		assertEquals(8.0/8.0, probs[2], 1e-15);
	}

	/** as testOperatorScheduleNestedByRelativeWeight() but with 2 nested operators **/
	@Test
	public void testOperatorScheduleNestedByRelativeWeight2() {
		double [] probs;
		OperatorSchedule schedule = setUpSchedule();

		ScaleOperator operator3 = new ScaleOperator(); 
		operator3.setID("scaleOperator.Species");
		operator3.initByName("parameter", parameter, "weight", 20.0);
		OperatorSchedule subSchedule = new OperatorSchedule();
		subSchedule = new OperatorSchedule();
		ScaleOperator operator4 = new ScaleOperator(); 
		operator4.setID("scaleOperator2.Species");
		operator4.initByName("parameter", parameter, "weight", 20.0);
		subSchedule.initByName("operator", operator3, "operator", operator4, "weight", 4.0, "operatorPattern", "^.*\\.Species$");
		schedule.subschedulesInput.get().clear();
		schedule.subschedulesInput.setValue(subSchedule, schedule);
		schedule.initAndValidate();
		schedule.addOperator(operator3);
		schedule.addOperator(operator4);
		// selectOperator() causes reweighting
		schedule.selectOperator();

		probs = schedule.getCummulativeProbs();
		assertEquals(4, probs.length);
		assertEquals(4,  schedule.operators.size());
		assertEquals(1.0/8.0, probs[0], 1e-15);
		assertEquals(4.0/8.0, probs[1], 1e-15);
		assertEquals(6.0/8.0, probs[2], 1e-15);
		assertEquals(8.0/8.0, probs[3], 1e-15);
	}
	

	/** as testOperatorSchedulePlain() but with 2 nested schedules weighted by relative weight **/
	@Test
	public void testOperatorScheduleMultiNestedByRelativeWeight() {
		double [] probs;
		OperatorSchedule schedule = setUpSchedule();
		
		ScaleOperator operator3 = new ScaleOperator(); 
		operator3.setID("scaleOperator.Species");
		operator3.initByName("parameter", parameter, "weight", 20.0);
		OperatorSchedule subSchedule = new OperatorSchedule();
		subSchedule.initByName("operator", operator3, "weight", 4.0, "operatorPattern", "^.*\\.Species$");
		
		ScaleOperator operator4 = new ScaleOperator(); 
		operator4.setID("scaleOperator.Species2");
		operator4.initByName("parameter", parameter, "weight", 20.0);
		OperatorSchedule subSchedule2 = new OperatorSchedule();
		subSchedule2.initByName("operator", operator4, "weight", 2.0, "operatorPattern", "^.*\\.Species2$");
		
		schedule.subschedulesInput.setValue(subSchedule, schedule);
		schedule.subschedulesInput.setValue(subSchedule2, schedule);
		schedule.initAndValidate();
		schedule.addOperator(operator3);
		schedule.addOperator(operator4);
		
		// selectOperator() causes reweighting
		schedule.selectOperator();

		probs = schedule.getCummulativeProbs();
		assertEquals(4, probs.length);
		assertEquals(4,  schedule.operators.size());
		assertEquals(1.0/10.0, probs[0], 1e-15);
		assertEquals(4.0/10.0, probs[1], 1e-15);
		assertEquals(8.0/10.0, probs[2], 1e-15);
		assertEquals(10.0/10.0, probs[3], 1e-15);
	}

	/** as testOperatorSchedulePlain() but with 2 nested schedules weighted by percentage
	 * Also, reuse operator, so it is in 2 sub schedules 
	 **/
	@Test
	public void testOperatorScheduleMultiNestedByPercentage() {
		double [] probs;
		OperatorSchedule schedule = setUpSchedule();
		
		ScaleOperator operator3 = new ScaleOperator(); 
		operator3.setID("scaleOperator.Species");
		operator3.initByName("parameter", parameter, "weight", 20.0);
		OperatorSchedule subSchedule = new OperatorSchedule();
		subSchedule.initByName("operator", operator3, "weight", 20.0, "weightIsPercentage", true, "operatorPattern", "^.*\\.Species$");
		
		OperatorSchedule subSchedule2 = new OperatorSchedule();
		subSchedule2.initByName("operator", operator3, "weight", 30.0, "weightIsPercentage", true, "operatorPattern", "^.*\\.Species$");
		
		schedule.subschedulesInput.setValue(subSchedule, schedule);
		schedule.subschedulesInput.setValue(subSchedule2, schedule);
		schedule.initAndValidate();
		schedule.addOperator(operator3);
		
		// selectOperator() causes reweighting
		schedule.selectOperator();

		probs = schedule.getCummulativeProbs();
		assertEquals(4, probs.length);
		assertEquals(4,  schedule.operators.size());
		assertEquals(1.25/10.0, probs[0], 1e-15);
		assertEquals(5.0/10.0, probs[1], 1e-15);
		assertEquals(7.0/10.0, probs[2], 1e-15);
		assertEquals(10.0/10.0, probs[3], 1e-15);
	}

	/** as testOperatorSchedulePlain() but with 2 nested schedules, one weighted by percentage, one weighted by relative weight
	 * Also, reuse operator, so it is in 2 sub schedules 
	 **/
	@Test
	public void testOperatorScheduleMultiNestedByPercentageAndRelativeWeight() {
		double [] probs;
		OperatorSchedule schedule = setUpSchedule();
		
		ScaleOperator operator3 = new ScaleOperator(); 
		operator3.setID("scaleOperator.Species");
		operator3.initByName("parameter", parameter, "weight", 20.0);
		OperatorSchedule subSchedule = new OperatorSchedule();
		subSchedule.initByName("operator", operator3, "weight", 4.0, "weightIsPercentage", false, "operatorPattern", "^.*\\.Species$");
		
		OperatorSchedule subSchedule2 = new OperatorSchedule();
		subSchedule2.initByName(/*"operator", operator3, */"weight", 20.0, "weightIsPercentage", true, "operatorPattern", "^.*\\.Species$");
		
		schedule.subschedulesInput.setValue(subSchedule, schedule);
		schedule.subschedulesInput.setValue(subSchedule2, schedule);
		schedule.initAndValidate();
		//schedule.addOperator(operator3);
		
		// selectOperator() causes reweighting
		schedule.selectOperator();

		probs = schedule.getCummulativeProbs();
		assertEquals(4, probs.length);
		assertEquals(4,  schedule.operators.size());
		assertEquals(1.0/10.0, probs[0], 1e-15);
		assertEquals(4.0/10.0, probs[1], 1e-15);
		assertEquals(8.0/10.0, probs[2], 1e-15);
		assertEquals(10.0/10.0, probs[3], 1e-15);
	}
}
