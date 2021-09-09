package beast.evolution.operator.kernel;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import beast.base.Description;
import beast.base.Input;
import beast.base.Log;
import beast.evolution.operator.ScaleOperator;
import beast.evolution.operator.SubtreeSlide;
import beast.evolution.operator.TipDatesRandomWalker;
import beast.evolution.operator.Uniform;
import beast.inference.Operator;
import beast.inference.OperatorSchedule;
import beast.inference.operator.DeltaExchangeOperator;
import beast.inference.operator.RealRandomWalkOperator;
import beast.inference.operator.UniformOperator;
import beast.inference.operator.UpDownOperator;
import beast.inference.operator.kernel.BactrianDeltaExchangeOperator;
import beast.inference.operator.kernel.BactrianIntervalOperator;
import beast.inference.operator.kernel.BactrianRandomWalkOperator;
import beast.inference.operator.kernel.BactrianUpDownOperator;

@Description("Operator schedule that replaces operators with Bactrian operators")
public class BactrianOperatorSchedule extends OperatorSchedule {

	public BactrianOperatorSchedule() {
		super();
	}
	
	@Override
	public void addOperator(Operator p) {
		if (p.getClass() == ScaleOperator.class) {
			Operator bp = new BactrianScaleOperator();
			p = initialiseOperator(p, bp);
		} else if (p.getClass() == RealRandomWalkOperator.class) {
			Operator bp = new BactrianRandomWalkOperator();
			p = initialiseOperator(p, bp);
		} else if (p.getClass() == Uniform.class) {
			Operator bp = new BactrianNodeOperator();
			p = initialiseOperator(p, bp);
		} else if (p.getClass() == UniformOperator.class) {
			Operator bp = new BactrianIntervalOperator();
			p = initialiseOperator(p, bp);
		} else if (p.getClass() == DeltaExchangeOperator.class) {
			Operator bp = new BactrianDeltaExchangeOperator();
			p = initialiseOperator(p, bp);
		} else if (p.getClass() ==  TipDatesRandomWalker.class) {
			Operator bp = new BactrianTipDatesRandomWalker();
			p = initialiseOperator(p, bp);
		} else if (p.getClass() == UpDownOperator.class) {
			Operator bp = new BactrianUpDownOperator();
			p = initialiseOperator(p, bp);
		} else if (p.getClass() == SubtreeSlide.class) {
			Operator bp = new BactrianSubtreeSlide();
			p = initialiseOperator(p, bp);
		}
		super.addOperator(p);
	}

	private Operator initialiseOperator(Operator p, Operator bp) {
		Log.warning("replacing " + p.getID() + " with " + bp.getClass().getSimpleName());

		List<Object> os = new ArrayList<>();
		Set<String> inputNames = new LinkedHashSet<>();
		for (Input<?> input : p.listInputs()) {
			inputNames.add(input.getName());
		}
		
		for (Input<?> input : bp.listInputs()) {
			if (inputNames.contains(input.getName())) {
				Object value = p.getInputValue(input.getName());
				if (value != null && !(value instanceof List && ((List)value).size() == 0)) {
				    os.add(input.getName());
				    os.add(value);
				}	
			}
		}
		bp.initByName(os.toArray());
		bp.setID(p.getID());
		return bp;
	}

}
