package beast.core;

import java.util.Set;

public interface BEASTInterface {
	String getID();
	void setID(String ID);
	Set getOutputs();

    /**
     * @throws Exception when plugin does not implement this method
     */
    //abstract public void initAndValidate() throws Exception;
    public void initAndValidate() throws Exception;
        // TODO: AR - Why is this not an abstract method? Does Plugin need to be concrete?
        // RRB: can be abstract, but this breaks some of the DocMaker stuff.
        // It only produces pages for Plugins that are not abstract.
        // This means the MCMC page does not point to Operator page any more since the latter does not exist.
        // As a result, there is no place that lists all Operators, which is a bit of a shame.
        // Perhaps DocMaker can be fixed to work around this, otherwise I see no issues making this abstract.

//        throw new Exception("BEASTobject.initAndValidate(): Every BEAST object should implement this method to" +
//                " assure the class behaves, even when inputs are not specified");
}
