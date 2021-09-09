package beast.app.inputeditor;

public interface BeautiDocProvider {
	
	public BeautiDoc getDoc();
	
	public BeautiPanelConfig getConfig();

	public int getPartitionIndex();
}
