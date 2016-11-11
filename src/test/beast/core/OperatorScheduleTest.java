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
		schedule.reweightSpeciesPartitionOperators();

		// weights have not changed
		assertEquals(1.0, operator1.getWeight());
		assertEquals(3.0, operator2.getWeight());
		
		ScaleOperator operator3 = new ScaleOperator(); 
		operator3.setID("scaleOperator.Species");
		operator3.initByName("parameter", parameter, "weight", 20.0);
		schedule.addOperator(operator3);
		schedule.reweightSpeciesPartitionOperators();

		assertEquals(1.0, operator1.getWeight());
		assertEquals(3.0, operator2.getWeight());
		// operator3 gets 20% of total weight
		assertEquals(1.0, operator3.getWeight());

		ScaleOperator operator4 = new ScaleOperator(); 
		operator4.setID("scaleOperator2.Species");
		operator4.initByName("parameter", parameter, "weight", 20.0);
		schedule.addOperator(operator4);
		schedule.reweightSpeciesPartitionOperators();
		assertEquals(1.0, operator1.getWeight());
		assertEquals(3.0, operator2.getWeight());
		// operator3 & 4 get 20% of weight in proportions 1:20
		assertEquals(1.0/21.0, operator3.getWeight());
		assertEquals(20.0/21.0, operator4.getWeight());
	}

}
