package beast.app.draw;

import java.io.File;
import java.io.FilenameFilter;

import javax.swing.filechooser.FileFilter;

public class ExtensionFileFilter extends FileFilter implements FilenameFilter {

    /**
     * The text description of the types of files accepted
     */
    protected String m_Description;

    /**
     * The filename extensions of accepted files
     */
    protected String[] m_Extension;

    /**
     * Creates the ExtensionFileFilter
     *
     * @param extension   the extension of accepted files.
     * @param description a text description of accepted files.
     */
    public ExtensionFileFilter(String extension, String description) {
        m_Extension = new String[1];
        m_Extension[0] = extension;
        m_Description = description;
    }

    /**
     * Creates an ExtensionFileFilter that accepts files that have any of the
     * extensions contained in the supplied array.
     *
     * @param extensions  an array of acceptable file extensions (as Strings).
     * @param description a text description of accepted files.
     */
    public ExtensionFileFilter(String[] extensions, String description) {
        m_Extension = extensions;
        m_Description = description;
    }

    /**
     * Gets the description of accepted files.
     *
     * @return the description.
     */
    public String getDescription() {

        return m_Description;
    }

    /**
     * Returns a copy of the acceptable extensions.
     *
     * @return the accepted extensions
     */
    public String[] getExtensions() {
        return (String[]) m_Extension.clone();
    }

    /**
     * Returns true if the supplied file should be accepted (i.e.: if it has the
     * required extension or is a directory).
     *
     * @param file the file of interest.
     * @return true if the file is accepted by the filter.
     */
    public boolean accept(File file) {

        String name = file.getName().toLowerCase();
        if (file.isDirectory()) {
            return true;
        }
        for (int i = 0; i < m_Extension.length; i++) {
            if (name.endsWith(m_Extension[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the file in the given directory with the given name
     * should be accepted.
     *
     * @param dir  the directory where the file resides.
     * @param name the name of the file.
     * @return true if the file is accepted.
     */
    public boolean accept(File dir, String name) {
        return accept(new File(dir, name));
    }
}
