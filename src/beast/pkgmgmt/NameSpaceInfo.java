package beast.pkgmgmt;


/**
 * The Package Info service allows a BEAST package to claim part of the name space,
 * so other packages will not be able to use them. The PackageManager uses this 
 * information to ensure no other BEAST package can use java package names (or
 * any sub-packages). 
 */
public class NameSpaceInfo {
	
	
	/*
	 * return java package names that cannot be used by other 
	 */
	public String [] namespaces() {
		return new String[] {"beast.pkgmgmt"};
	}

}
