module test.beast {
	
	// module depends on beast.pkgmgmt and beast.base
	requires beast.pkgmgmt;
	requires beast.base;
	requires beast.app;
	
	// standard module dependencies
	requires java.base;
	requires java.desktop;
	requires java.logging;

	
	// external libraries from lib folder
	requires beagle;
	requires antlr.runtime;
	requires colt;
	requires jam;
	requires junit;
	
	// libraries customised for BEAST 2 from build/dist folder
	requires json;
	requires commons.math;
	
	// exports required to run tests inside Ecplipse
	exports test.beast.core;	
	exports test.beast.util;	
	exports test.beast.evolution.operator;

}