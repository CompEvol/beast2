module beast.app {
	
	// module depends on beast.pkgmgmt and beast.base
	requires beast.pkgmgmt;
	requires beast.base;
	
	// standard module dependencies
	requires java.base;
	requires java.desktop;
	requires java.logging;

	
	// external libraries from lib folder
	requires beagle;
	requires antlr.runtime;
	requires colt;
	requires jam;
	
	// libraries customised for BEAST 2 from build/dist folder
	requires json;
	requires commons.math;

}