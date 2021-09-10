module beast.base {
	
	// module depends on beast.pkgmgmt
	requires beast.pkgmgmt;
	
	// standard module dependencies
	requires java.base;
	requires java.desktop;

	
	// external libraries from lib folder
	requires beagle;
	requires antlr.runtime;
	requires colt;
	// libraries customised for BEAST 2 from build/dist folder
	requires json;
	requires commons.math;

	exports beast.base.core;
	exports beast.base.util;
	exports beast.base.parser;
	exports beast.base.math.matrixalgebra;
	exports beast.base.math;
	// exports beast.base.evolution.tree.treeparser;
	exports beast.base.evolution.tree.coalescent;
	exports beast.base.evolution.tree;
	exports beast.base.evolution.likelihood;
	exports beast.base.evolution.sitemodel;
	exports beast.base.evolution.distance;
	exports beast.base.evolution.datatype;
	exports beast.base.evolution.alignment;
	exports beast.base.evolution.branchratemodel;
	exports beast.base.evolution.speciation;
	exports beast.base.evolution.operator.kernel;
	exports beast.base.evolution.operator;
	exports beast.base.evolution.substitutionmodel;
	exports beast.base.evolution;
	exports beast.base.inference.util;
	exports beast.base.inference.distribution;
	exports beast.base.inference.parameter;
	exports beast.base.inference.operator.kernel;
	exports beast.base.inference.operator;
	exports beast.base.inference;
}