package test.beast.core;


import org.junit.Test;

import beast.core.OperatorSchedule;
import beast.core.parameter.RealParameter;
import beast.evolution.operators.DeltaExchangeOperator;
import beast.evolution.operators.ScaleOperator;
import junit.framework.TestCase;

public class OperatorScheduleTest extends TestCase {
	
	@Test
	public void testOperatorSchedule() {
		double [] probs;
		DeltaExchangeOperator operator1 = new DeltaExchangeOperator();
		operator1.setID("deltaOperator");
		RealParameter parameter = new RealParameter(new Double[] {1., 1., 1., 1.});
		operator1.initByName("parameter", parameter, "weight", 1.0);

		ScaleOperator operator2 = new ScaleOperator(); 
		operator2.setID("scaleOperator");
		operator2.initByName("parameter", parameter, "weight", 3.0);

		OperatorSchedule schedule = new OperatorSchedule();
		schedule.initAndValidate();
		schedule.addOperator(operator1);
		schedule.addOperator(operator2);
		// selectOperator() causes reweighting
		schedule.selectOperator();

		
		// weights have not changed
		probs = schedule.getCummulativeProbs();
		assertEquals(1.0/4.0, probs[0], 1e-15);
		assertEquals(4.0/4.0, probs[1], 1e-15);
		
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
		assertEquals(1.0/5.0, probs[0], 1e-15);
		assertEquals(4.0/5.0, probs[1], 1e-15);
		assertEquals(5.0/5.0, probs[2], 1e-15);
		

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
		assertEquals(1.0/5.0, probs[0], 1e-15);
		assertEquals(4.0/5.0, probs[1], 1e-15);
		assertEquals(4.5/5.0, probs[2], 1e-15);
		assertEquals(5.0/5.0, probs[3], 1e-15);
	}

}
