package beast.app.inputeditor;

import java.io.File;
import java.util.List;

import beast.base.core.BEASTInterface;


/** Interface for importing alignments from file that are recognisable 
 * from for example its file extension (see canHandleFile()). BeautiAlignmentProvider 
 * will find implementations in packages through introspection, and these are used 
 * when a implementation is not available in the BEAST core.
 **/
public interface AlignmentImporter {

	/** return list of file extensions 
	 * that are supported by this importer
	 * @return
	 */
	public String [] getFileExtensions();
	
	/** process single file
	 * @param file
	 * @return list of Alignments found in file, and calibrations 
	 */
	public List<BEASTInterface> loadFile(File file);
	
	/** check whether the file can be processed by this particular importer.
	 * Often, the first line of a file contains information about the nature 
	 * of the file (e.g. #NEXUS in nexus files, <beast version="2.0"... in BEAST 2 
	 * files) that can tell a bit more than just the file extension. 
	 * 
	 * By default, it only checks whether the file extension matches any of the 
	 * ones listed in getFileExtension().
	 * 
	 * return true if file can be processed by this importer.
	 */
	default public boolean canHandleFile(File file) {
		String name = file.getName();
		if (name.lastIndexOf('.') == -1) {
			// this file has no file extension
			return false;
		}
		
		String extension = name.substring(name.lastIndexOf('.') + 1);
		for (String s : getFileExtensions()) {
			if (s.equals(extension)) {
				return true;
			}
		}
		
		return false;
	}
}
