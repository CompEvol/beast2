package beast.base.inference.parameter;


import java.util.ArrayList;
import java.util.List;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.inference.StateNode;

@Description("Parmeter consisting of 2 or more RealParameters but behaving like a single RealParameter")
// partial implementation
public class CompoundRealParameter extends RealParameter {
	final public Input<List<RealParameter>> parameterListInput = new Input<>("parameter", "parameters making up the compound parameter", new ArrayList<>(), Validate.REQUIRED);

	
	public CompoundRealParameter() {
		// we only want other RealParameters as input
		lowerValueInput.setRule(Validate.FORBIDDEN);
		upperValueInput.setRule(Validate.FORBIDDEN);
		valuesInput.setRule(Validate.FORBIDDEN);
	}
	
	RealParameter [] parameters;
	int dim = 0;
	int [] mapIndexToParameter;
	int [] offset;
	
	
	public void initAndValidate() {
		if (dimensionInput.get() != 1) {
			throw new IllegalArgumentException("dimension should not be specified");
		}
		if (minorDimensionInput.get() != 1) {
			throw new IllegalArgumentException("minor dimension should not be specified");
		}
		if (isEstimatedInput.get() != true) {
			throw new IllegalArgumentException("estimate input should not be specified");
		}
		
		parameters = new RealParameter[parameterListInput.get().size()];
		offset = new int[parameters.length];
		int k = 0;
		for (RealParameter p : parameterListInput.get()) {
			parameters[k++] = p;
			dim += p.getDimension();
		}
		
		mapIndexToParameter = new int[dim];		
		offset = new int[dim];
		k = 0;
		int paramIndex = 0;
		int o = 0;
		for (RealParameter p : parameters) {
			for (int i = 0; i < p.getDimension(); i++) {
				offset[k] = o;
				mapIndexToParameter[k++] = paramIndex;
			}
			paramIndex++;
			o = k;
		}
	}
	
	@Override
	public Double getValue() {
		return parameters[0].getValue();
	}
	
	@Override
	public Double getValue(int i) {
		return parameters[mapIndexToParameter[i]].getValue(i - offset[i]);
	}

	@Override
	public double getArrayValue() {
		return parameters[0].getArrayValue();
	}

	@Override
	public double getArrayValue(int i) {
		return parameters[mapIndexToParameter[i]].getArrayValue(i - offset[i]);
	}

	@Override
	public void setValue(Double value) {
		parameters[0].setValue(value);
	}
	
	@Override
	public void setValue(int i, Double value) {
		parameters[mapIndexToParameter[i]].setValue(i - offset[i], value);
	}
	

	@Override
	public int getDimension() {
		return dim;
	}

	@Override
	public void setDimension(int dim) {
		if (this.dim == dim) {
			return;
		}
		throw new RuntimeException("Cannot be implemented");
	}

	@Override
	public void setLower(Double lower) {
		for (RealParameter p : parameters) {
			p.setLower(lower);
		}
	}
	
	@Override
	public void setUpper(Double upper) {
		for (RealParameter p : parameters) {
			p.setUpper(upper);
		}
	}

	@Override
	public void setBounds(Double lower, Double upper) {
		for (RealParameter p : parameters) {
			p.setBounds(lower, upper);
		}
	}
	
    @Override
    public void assignFromWithoutID(StateNode other) {
    	RealParameter other2 = (RealParameter) other;
    	int k = 0;
		for (RealParameter p : parameters) {			
            Double[] values = new Double[p.getDimension()];
            for (int i = 0; i < p.getDimension(); i++) {
                values[i] = other2.getValue(k++);
            }
            RealParameter r = new RealParameter(values);
            p.assignFrom(r);
		}
    }
    
    @Override
    public StateNode getCurrent() {
    	return this;
    }


    @Override
    protected void store() { 
    	// do nothing
    }
    
    @Override
    public void restore() { 
    	// do nothing
		hasStartedEditing = false;
    }
    
    @Override
    protected boolean requiresRecalculation() {
		for (RealParameter p : parameters) {
			if (p.somethingIsDirty()) {
				hasStartedEditing = true;
				return true;
			}
		}
	    return false;
    }

}
