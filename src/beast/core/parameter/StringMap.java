package beast.core.parameter;

import beast.core.Description;

@Description("Maps names to string, e.g. taxon names to sequences")
public class StringMap extends Map<String> {
	protected Class<?> mapType() {return String.class;}

}
