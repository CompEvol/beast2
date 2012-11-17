package beast.app.beauti;


import beast.core.Plugin;
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
		String sID = treeLikelihood.m_data.get().getID();
		sID = BeautiDoc.parsePartition(sID);
		this.partition = sID;
		if (treeLikelihood.m_pBranchRateModel.get() != null) {
			sID = treeLikelihood.m_pBranchRateModel.get().getID();
			sID = BeautiDoc.parsePartition(sID);
		}
		this.clockModel = sID;
		sID = ((Plugin) treeLikelihood.m_pSiteModel.get()).getID();
		sID = BeautiDoc.parsePartition(sID);
		this.siteModel = sID;
		sID = treeLikelihood.m_tree.get().getID();
		sID = BeautiDoc.parsePartition(sID);
		this.tree = sID;
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
