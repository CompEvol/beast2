package beast.base.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class FileUtils {

	
    static public String load(String fileName) throws IOException {
        return load(new File(fileName));
    } // load

    static public String load(File file) throws IOException {
        BufferedReader fin = new BufferedReader(new FileReader(file));
        StringBuffer buf = new StringBuffer();
        String str = null;
        while (fin.ready()) {
            str = fin.readLine();
            buf.append(str);
            buf.append('\n');
        }
        fin.close();
        return buf.toString();
    } // load
    
    
    public void save(String fileName, String text) throws IOException  {
        save(new File(fileName), text);
    } // save

    public void save(File file, String text) throws IOException  {
        FileWriter outfile = new FileWriter(file);
        outfile.write(text);
        outfile.close();
    } // save
}
