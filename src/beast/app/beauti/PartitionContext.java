package beast.app.beauti;


import beast.core.BEASTInterface;
import beast.evolution.likelihood.GenericTreeLikelihood;

public class PartitionContext {
	public String partition;
	public String siteModel;
	public String clockModel;
	public String tree;

	public PartitionContext() {}
	
	public PartitionContext(String partition) {
		this.partition = partition;
		siteModel = partition;
		clockModel = partition;
		tree = partition;
	}
	
	public PartitionContext(String partition,
			String siteModel,
			String clockModel,
			String tree
			) {
		this.partition = partition;
		this.siteModel = siteModel;
		this.clockModel = clockModel;
		this.tree = tree;
	}
	
	public PartitionContext(GenericTreeLikelihood treeLikelihood) {
		String id = treeLikelihood.dataInput.get().getID();
		id = BeautiDoc.parsePartition(id);
		this.partition = id;
		if (treeLikelihood.branchRateModelInput.get() != null) {
			id = treeLikelihood.branchRateModelInput.get().getID();
			id = BeautiDoc.parsePartition(id);
		}
		this.clockModel = id;
		id = ((BEASTInterface) treeLikelihood.siteModelInput.get()).getID();
		id = BeautiDoc.parsePartition(id);
		this.siteModel = id;
		id = treeLikelihood.treeInput.get().getID();
		id = BeautiDoc.parsePartition(id);
		this.tree = id;
	}
	
	@Override
	public String toString() {
		return partition + "," + siteModel + "," + clockModel + "," + tree;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj instanceof PartitionContext) {
			PartitionContext other = (PartitionContext) obj;
			return other.partition.equals(partition) &&
				other.siteModel.equals(siteModel) &&
				other.clockModel.equals(clockModel) &&
				other.tree.equals(tree);
		}
		return false;
	}
}
