package test.beast.core;


import beast.base.BEASTObject;
import beast.base.Description;
import beast.base.Param;

@Description("PrimitiveInterface is used for testing inner class inside interface")
public interface PrimitiveInterface {


	@Description("InterfaceInnerClass is used for testing inner class inside interface")
	public class InterfaceInnerClass extends BEASTObject implements PrimitiveInterface {
		private int i;
		
		public InterfaceInnerClass(@Param(name="i", description="input of primitive type") int i) {
			this.i = i;
		}
		
		public InterfaceInnerClass() {}
		
		@Override
		public void initAndValidate() {
		}

		public int getI() {
			return i;
		}
		public void setI(int i) {
			this.i = i;
		}

	}
}
