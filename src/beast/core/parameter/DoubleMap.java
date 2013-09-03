package beast.core.parameter;

import beast.core.Description;

@Description("Maps names to double values, e.g. taxon names to sample date")
public class DoubleMap extends Map<Double> {
	protected Class<?> mapType() {return Double.class;}

}