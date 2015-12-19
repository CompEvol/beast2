package test.beast.util;

import java.util.HashSet;
import java.util.Set;

import beast.core.BEASTInterface;
import beast.core.Param;
import beast.core.Runnable;

public class JSONTestRunnable extends Runnable {
    
    int param1;
    
    public int getParam1() {
		return param1;
	}

	public void setParam1(int param1) {
		this.param1 = param1;
	}

	public JSONTestRunnable(@Param(description = "test to see whether the JSON parser/producer can handle annotated constructors", name = "param1", defaultValue = "10") Integer param1) {
    	this.param1 = param1;
    }
    
	@Override
	public void initAndValidate() throws Exception {
	}

	@Override
	public String getID() {
		return "JSONTest";
	}

	@Override
	public void setID(String ID) {
		// ignore
	}

	Set<BEASTInterface> outputs = new HashSet<>();
	
	@Override
	public Set getOutputs() {
		return outputs;
	}

	@Override
	public void run() throws Exception {
		System.out.println("We got a " + param1 + ". How's that?");		
	}
}
