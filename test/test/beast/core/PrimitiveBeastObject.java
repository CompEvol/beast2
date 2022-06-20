package test.beast.core;


import beast.base.core.BEASTObject;
import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Param;
import beast.base.inference.*;

@Description("Class to test the behaviour of primitive inputs")
public class PrimitiveBeastObject extends BEASTObject {
	public Input<Boolean> input = new Input<>("plaininput", "classic BEAST2 style input for testing listing inputs", true);
	
	public enum Enumeration {none, one, two};
	
	private int i;
	private Enumeration e = Enumeration.none;
	double [] a = null;
	Double [] b = null;
	String [] s = null;
	PrimitiveBeastObject [] p = null;
	
	public PrimitiveBeastObject() {
	}
	
	// add un-annotated c'tor to confuse JSON/XML parsers
	public PrimitiveBeastObject(int i, Enumeration e, double [] a, Double [] b, String [] s) {
		setI(i);
		setE(e);
		setA(a);
		setB(b);
		setS(s);
	}

	
	public PrimitiveBeastObject(@Param(name="i", description="input of primitive type") int i,
			@Param(name="e", description="input of primitive type", optional=true, defaultValue="one") Enumeration e) {
		this.i = i;
		this.e = e;
	}

	public PrimitiveBeastObject(@Param(name="i", description="input of primitive type") int i,
			@Param(name="e", description="input of primitive type", optional=true, defaultValue="one") Enumeration e,
			@Param(name="a", description="input of array of primitive type") double [] a) {
		this.i = i;
		this.e = e;
		setA(a);
	}

	public PrimitiveBeastObject(@Param(name="e", description="input of primitive type", optional=true, defaultValue="one") Enumeration e) {
		this(0, e);
	}

	public PrimitiveBeastObject(@Param(name="a", description="input of array of primitive type") double [] a) {
		setA(a);
	}

	public PrimitiveBeastObject(@Param(name="b", description="input of array of objects") Double [] b) {
		setB(b);
	}

	public PrimitiveBeastObject(@Param(name="s", description="input of array of string objects") String [] s) {
		setS(s);
	}

	public PrimitiveBeastObject(@Param(name="p", description="input of array of BEAST objects") PrimitiveBeastObject [] p) {
		setP(p);
	}

	@Override
	public void initAndValidate() {
	}

	public int getI() {
		return i;
	}
	public void setI(int i) {
		this.i = i;
	}

	public Enumeration getE() {
		return e;
	}
	public void setE(Enumeration e) {
		this.e = e;
	}
	
	public PrimitiveBeastObject [] getP() {
		if (p == null) {
			return null;
		}
		return p.clone();
	}
	
	public void setP(PrimitiveBeastObject [] p) {
		this.p = p.clone();
	}

	public double[] getA() {
		if (a == null) {
			return null;
		}
		return a.clone();
	}

	public void setA(double[] a) {
		this.a = a.clone();
	}

	public Double[] getB() {
		if (b == null) {
			return null;
		}
		return b.clone();
	}

	public void setS(String[] s) {
		this.s = s.clone();
	}
	
	public String[] getS() {
		if (s == null) {
			return null;
		}
		return s.clone();
	}

	public void setB(Double[] b) {
		this.b = b.clone();
	}

	public class InnerClass extends PrimitiveBeastObject {

		public InnerClass() {}
		
		public InnerClass(@Param(name="i", description="input of primitive type") int i,
				@Param(name="e", description="input of primitive type", optional=true, defaultValue="one") Enumeration e) {
			super(i, e);
		}

		public InnerClass(@Param(name="e", description="input of primitive type", optional=true, defaultValue="one") Enumeration e) {
			this(0, e);
		}

		public InnerClass(@Param(name="a", description="input of primitive type") double[] a) {
			super(a);
		}
	}
}
