package beast.parser;

import beast.base.BEASTInterface;
import beast.inference.util.ModelLogger;


/** Hack to get around dependency of beast.inferece.Logger to XMLProducer **/
public class XMLModelLogger extends ModelLogger {

	
	@Override
	protected int canHandleObject(Object o) {		
		if (o instanceof BEASTInterface) {
			return 1;
		}
		return -1;
	}
	
	
	@Override
	public String modelToStringImp(Object o) {
		return new XMLProducer().modelToXML((BEASTInterface) o); 
	}
}
