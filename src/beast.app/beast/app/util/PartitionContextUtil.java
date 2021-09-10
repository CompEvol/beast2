package beast.app.util;

import beast.base.core.BEASTInterface;
import beast.base.evolution.likelihood.GenericTreeLikelihood;
import beast.base.parser.PartitionContext;

public class PartitionContextUtil {
	
	public static PartitionContext newPartitionContext(GenericTreeLikelihood treeLikelihood) {
		PartitionContext p = new PartitionContext();
		String id = treeLikelihood.dataInput.get().getID();
		id = PartitionContext.parsePartition(id);
		p.partition = id;
		if (treeLikelihood.branchRateModelInput.get() != null) {
			id = treeLikelihood.branchRateModelInput.get().getID();
			id = PartitionContext.parsePartition(id);
		}
		p.clockModel = id;
		id = ((BEASTInterface) treeLikelihood.siteModelInput.get()).getID();
		id = PartitionContext.parsePartition(id);
		p.siteModel = id;
		id = treeLikelihood.treeInput.get().getID();
		id = PartitionContext.parsePartition(id);
		p.tree = id;
		return p;
}
}
