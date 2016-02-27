package test.beast.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import beast.core.BEASTInterface;
import beast.core.Description;
import beast.core.Param;
import beast.core.Runnable;
import beast.evolution.alignment.Taxon;

@Description("Used for testing purposed only")
public class AnnotatedRunnableTestClass extends Runnable {
    
    int param1;
    List<Taxon> taxa;
    List<Double> array;
    
	public List<Double> getArray() {
		return array;
	}

	public void setArray(List<Double> array) {
		this.array = array;
	}

	public void setArray(Double value) {
		if (this.array == null) {
			this.array = new ArrayList<>();
		}
		this.array.add(value);
	}

	public Integer getParam1() {
		return param1;
	}

	public void setParam1(Integer param1) {
		this.param1 = param1;
	}

	/** default constructor, should not be used **/
	public AnnotatedRunnableTestClass() {
    	this.param1 = 0;
    	this.taxa = new ArrayList<>();
	}

	// note that if there are different constructors and an argument does not appear in the other constructor it has to be optional
	public AnnotatedRunnableTestClass(
			@Param(description = "test to see whether the JSON/XML parser/producer can handle annotated constructors", name = "param1", optional=true, defaultValue = "10") Integer param1,
			@Param(description = "test to see whether the JSON/XML parser/producer can handle annotated List", name = "taxon", optional=true) List<Taxon> taxa) {
    	this.param1 = param1;
    	this.taxa = new ArrayList<>();
    	this.taxa.addAll(taxa);
    }
	
	public AnnotatedRunnableTestClass(
			@Param(description = "test to see whether multiple constructors are handled, and list of Doubles", name = "array", optional=true) List<Double> array) {
		this.array = array;
	}
    
	@Override
	public void initAndValidate() {
	}

	@Override
	public String getID() {
		return "JSONTest";
	}

	@Override
	public void setID(String ID) {
		// ignore
	}

    public List<Taxon> getTaxon() {
		return taxa;
	}
	
	public void setTaxon(Taxon taxon) {
		this.taxa.add(taxon);
	}
	
	Set<BEASTInterface> outputs = new HashSet<>();
	
	@Override
	public Set<BEASTInterface> getOutputs() {
		return outputs;
	}

	@Override
	public void run() throws Exception {
		System.out.println("We got a " + param1 + ". How's that?");		
	}
}
