package beast.app.beauti;

import java.io.File;
import java.util.List;

import beast.core.BEASTInterface;


/** Interface for importing alignments from file that are recognisable 
 * from its file extension. BeautiAlignmentProvider will find implementations 
 * through introspection, and these are used when a default implementation
 * is not available for the file extension.
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
	
}
