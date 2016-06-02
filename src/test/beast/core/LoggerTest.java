package test.beast.core;

import beast.core.Logger;
import beast.core.parameter.RealParameter;
import org.junit.*;

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

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void initAndValidate() throws Exception {

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
    public void init() throws Exception {

    }

    @Test
    public void openLogFile() throws Exception {

    }

    @Test
    public void log() throws Exception {
        logger.log(-1);
//        assertEquals("", , );

    }

    @Test
    public void close() throws Exception {

    }

    @Test
    public void getSampleOffset() throws Exception {

    }

}