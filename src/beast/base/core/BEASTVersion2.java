package beast.base.core;

import beast.pkgmgmt.BEASTVersion;

/**
 * This is a duplicate of BEASTVersion, however the BEASTVersion class should be used by BEASTLauncher 
 * and derivatives, but this class should be used in any other place
 * */
public class BEASTVersion2 extends BEASTVersion {

    /**
     * Version string: assumed to be in format x.x.x
     */
    private static final String VERSION = "2.7.1";

    private static final String DATE_STRING = "2002-2022";

    private static final boolean IS_PRERELEASE = true;
//
//    private static final String BEAST2_WEBPAGE = "http://beast2.org/";
//    
//    private static final String BEAST2_SOURCE = "http://github.com/CompEvol/beast2";
//
    @Override
	public String getVersion() {
        return VERSION;
    }

    @Override
	public String getVersionString() {
        return "v" + VERSION + (IS_PRERELEASE ? " Prerelease" : "");
    }

    @Override
	public String getDateString() {
        return DATE_STRING;
    }
}
