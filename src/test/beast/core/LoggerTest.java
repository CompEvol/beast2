package test.beast.core;

import beast.core.Logger;
import beast.core.parameter.RealParameter;
import org.junit.*;

import java.io.*;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * junit 4
 * http://www.asjava.com/junit/junit-3-vs-junit-4-comparison/
 * http://stackoverflow.com/questions/2469911/how-do-i-assert-my-exception-message-with-junit-test-annotation
 *
 * @author Walter Xie
 */
public class LoggerTest {
    Logger logger;

    @Before
    public void setUp() throws Exception {
        logger = new Logger();
    }

    @Test
    public void isLoggingToStdout() throws Exception {
        logger = new Logger();
        logger.initByName("fileName", null, "log", new RealParameter());
        assertTrue("fileName == null", logger.isLoggingToStdout());

        logger = new Logger();
        logger.initByName("fileName", "", "log", new RealParameter());
        assertTrue("fileName.length() == 0", logger.isLoggingToStdout());

        logger = new Logger();
        logger.initByName("fileName", "beast.log", "log", new RealParameter());
        assertFalse("fileName = \"beast.log\"", logger.isLoggingToStdout());
    }

    @Test
    public void testFileLog() throws Exception {
        logger = new Logger();
        logger.initByName("fileName", "beast.log", "log", new RealParameter(new Double[]{0.3, 0.7}));
        File f_log = new File(logger.fileNameInput.get());
        if (f_log.exists()) {
            boolean log_deleted = f_log.delete();
            System.out.println("Delete log : " + f_log.getAbsolutePath() + " for testFileLog.");
        }

        logger.init();
        assertTrue("beast.log created successfully", f_log.exists());

        // rI >= 0
        int rI = new Random().nextInt(10000000);
        logger.log(-1);
        logger.log(rI);
        logger.close();

        //TODO cannot get "closing" status from PrintStream
//        assertNull("m_out is beast.log after close", logger.getM_out());

        BufferedReader in = new BufferedReader(new FileReader(f_log));
        // column names
        String line = in.readLine();
        // 1st sample
        String sample1 = in.readLine();
        String[] sp = sample1.split("\t", -1);
        assertFalse("check beast.log -1 not logged", sp[0].equals("-1"));
        assertEquals("check beast.log 1st sample", Integer.toString(rI), sp[0]);
    }

    @Test
    public void testScreenLog() throws Exception {
        logger = new Logger();
        logger.initByName("fileName", "", "log", new RealParameter(new Double[]{0.3, 0.7}));

        logger.init();
        assertTrue("m_out is System.out", logger.getM_out() == System.out);

        logger.log(1);
        //TODO cannot extract content

        // close all file, except stdout
        logger.close();
        assertTrue("m_out is still System.out after close", logger.getM_out() == System.out);
    }

}