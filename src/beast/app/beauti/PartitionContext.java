package beast.app.beauti;

import beast.evolution.likelihood.TreeLikelihood;

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
	
	public PartitionContext(TreeLikelihood treeLikelihoods) {
		String sID = treeLikelihoods.m_data.get().getID();
		sID = BeautiDoc.parsePartition(sID);
		this.partition = sID;
		sID = treeLikelihoods.m_pSiteModel.get().getID();
		sID = BeautiDoc.parsePartition(sID);
		this.siteModel = sID;
		sID = treeLikelihoods.m_pBranchRateModel.get().getID();
		sID = BeautiDoc.parsePartition(sID);
		this.clockModel = sID;
		sID = treeLikelihoods.m_tree.get().getID();
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
