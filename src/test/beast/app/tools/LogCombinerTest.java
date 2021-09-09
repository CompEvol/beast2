package test.beast.app.tools;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.junit.Test;

import beast.app.inputeditor.BeautiDoc;
import beast.app.tools.LogAnalyser;
import beast.app.tools.LogCombiner;
import beast.evolution.tree.Tree;
import beast.parser.NexusParser;
import beast.util.Randomizer;
import junit.framework.TestCase;

public class LogCombinerTest extends TestCase {

	static void creatLogFiles(int filesCount, int sampleInterval, int sampleCount) {
		for (int i = 0; i < filesCount; i++) {
			StringBuilder b = new StringBuilder();
			b.append("Sample\tRandom\n");
			for (int j = 0; j < sampleCount; j++) {
				b.append(j * sampleInterval);
				b.append('\t');
				b.append(Randomizer.nextDouble());
				b.append('\n');
			}
			try {
				FileWriter outfile = new FileWriter(new File("tmp_in" + i + ".log"));
				outfile.write(b.toString());
				outfile.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	public void testLogCombiner() throws IOException {
		creatLogFiles(3, 1000, 101);
		LogAnalyser analyser = new LogAnalyser("tmp_in0.log", 0);
		assertEquals(101, analyser.getTrace(0).length);

		// combine 3 log files (default burn in 10%)
		LogCombiner.main(new String[] { "-log", "tmp_in0.log", "tmp_in1.log", "tmp_in2.log", "-o", "tmp_out.log" });
		analyser = new LogAnalyser("tmp_out.log", 0);
		Double[] trace = analyser.getTrace(0);
		// ensure length of combined trace = 300 - 10% burn in
		assertEquals(273, trace.length);
		// ensure last sample nr = 272 x 1000 = 26900
		assertEquals(272000, trace[trace.length - 1], 1e-10);

		// combine 3 log files with burnin 0
		LogCombiner.main(new String[] { "-log", "tmp_in0.log", "tmp_in1.log", "tmp_in2.log", "-o", "tmp_out.log",
				"-burnin", "0" });
		analyser = new LogAnalyser("tmp_out.log", 0);
		assertEquals(303, analyser.getTrace(0).length);

		// test resampling single file
		LogCombiner.main(
				new String[] { "-log", "tmp_in0.log", "-o", "tmp_out.log", "-burnin", "0", "-resample", "10000" });
		analyser = new LogAnalyser("tmp_out.log", 0);
		trace = analyser.getTrace(0);
		// ensure length of combined trace = 10 burn in
		assertEquals(11, trace.length);
		// ensure last sample nr = 100000
		assertEquals(100000, trace[trace.length - 1], 1e-10);

		// test resampling three files
		LogCombiner.main(new String[] { "-log", "tmp_in0.log", "tmp_in1.log", "tmp_in2.log", "-o", "tmp_out.log",
				"-burnin", "0", "-resample", "10000" });
		analyser = new LogAnalyser("tmp_out.log", 0);
		trace = analyser.getTrace(0);
		// ensure length of combined trace = 10 burn in
		assertEquals(33, trace.length);
		// ensure last sample nr = 100000
		assertEquals(320000, trace[trace.length - 1], 1e-10);
		// new File("tmp_out.log").delete();

		// test renumbering single file
		LogCombiner.main(new String[] { "-log", "tmp_in0.log", "-o", "tmp_out.log", "-burnin", "0", "-renumber" });
		analyser = new LogAnalyser("tmp_out.log", 0);
		trace = analyser.getTrace(0);
		// ensure length of combined trace = 300 - 10% burn in
		assertEquals(101, trace.length);
		// ensure last sample nr = 101
		assertEquals(100, trace[trace.length - 1], 1e-10);

		// test renumbering 3 log files
		LogCombiner.main(new String[] { "-log", "tmp_in0.log", "tmp_in1.log", "tmp_in2.log", "-o", "tmp_out.log",
				"-burnin", "0", "-renumber" });
		analyser = new LogAnalyser("tmp_out.log", 0);
		trace = analyser.getTrace(0);
		// ensure length of combined trace = 303
		assertEquals(303, trace.length);
		// ensure last sample nr = 303
		assertEquals(302, trace[trace.length - 1], 1e-10);
	}

	static void creatTreeLogFiles(int filesCount, int sampleInterval, int sampleCount) {
		for (int i = 0; i < filesCount; i++) {
			StringBuilder b = new StringBuilder();
			b.append("#NEXUS\n\n");
			b.append("Begin taxa;\n");
			b.append("Dimensions ntax=2;\n");
			b.append("	                Taxlabels\n");
			b.append("	                        bonobo \n");
			b.append("	                        siamang \n");
			b.append(";\n");
			b.append("End;\n");
			b.append("Begin trees;\n");
			b.append("	        Translate\n");
			b.append("	                   1 bonobo,\n");
			b.append("	                   2 siamang\n");
			b.append(";\n");

			for (int j = 0; j < sampleCount; j++) {
				b.append("tree STATE_" + j * sampleInterval + " = ");
				double h = Randomizer.nextDouble();
				b.append("(1:" + h + ",2:" + h +"):0.0;\n");
			}
			b.append("End;\n");
			try {
				FileWriter outfile = new FileWriter(new File("tmp_in" + i + ".trees"));
				outfile.write(b.toString());
				outfile.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	public void testTreeLogCombiner() throws IOException {
		creatTreeLogFiles(3, 1000, 101);
		assertEquals(101, treeCount("tmp_in0.trees"));
		assertEquals(100000, lastTreeStateNr("tmp_in0.trees"));
		
		// combine 3 log files (default burn in 10%)
		LogCombiner.main(new String[] { "-log", "tmp_in0.trees", "tmp_in1.trees", "tmp_in2.trees", "-o", "tmp_out.trees" });
		// ensure length of combined trace = 300 - 10% burn in
		assertEquals(273, treeCount("tmp_out.trees"));
		// ensure last sample nr = 272 x 1000 = 26900
		assertEquals(272000, lastTreeStateNr("tmp_out.trees"));

		// combine 3 log files with burnin 0
		LogCombiner.main(new String[] { "-log", "tmp_in0.trees", "tmp_in1.trees", "tmp_in2.trees", "-o", "tmp_out.trees",
				"-burnin", "0" });
		assertEquals(303, treeCount("tmp_out.trees"));

		// test resampling single file
		LogCombiner.main(
				new String[] { "-log", "tmp_in0.trees", "-o", "tmp_out.trees", "-burnin", "0", "-resample", "10000" });
		assertEquals(11, treeCount("tmp_out.trees"));
		// ensure last sample nr = 100000
		assertEquals(100000, lastTreeStateNr("tmp_out.trees"));

		// test resampling three files
		LogCombiner.main(new String[] { "-log", "tmp_in0.trees", "tmp_in1.trees", "tmp_in2.trees", "-o", "tmp_out.trees",
				"-burnin", "0", "-resample", "10000" });
		assertEquals(33, treeCount("tmp_out.trees"));
		// ensure last sample nr = 100000
		assertEquals(320000, lastTreeStateNr("tmp_out.trees"));
		// new File("tmp_out.trees").delete();

		// test renumbering single file
		LogCombiner.main(new String[] { "-log", "tmp_in0.trees", "-o", "tmp_out.trees", "-burnin", "0", "-renumber" });
		assertEquals(101, treeCount("tmp_out.trees"));
		// ensure last sample nr = 101
		assertEquals(100, lastTreeStateNr("tmp_out.trees"));

		// test renumbering 3 log files
		LogCombiner.main(new String[] { "-log", "tmp_in0.trees", "tmp_in1.trees", "tmp_in2.trees", "-o", "tmp_out.trees",
				"-burnin", "0", "-renumber" });
		assertEquals(303, treeCount("tmp_out.trees"));
		// ensure last sample nr = 303
		assertEquals(302, lastTreeStateNr("tmp_out.trees"));
	}

	private int treeCount(String file) throws IOException {
		NexusParser analyser = new NexusParser();
		analyser.parseFile(new File(file));
		return analyser.trees.size();
	}

	private long lastTreeStateNr(String file) throws IOException {
		String str = BeautiDoc.load(file);
		String [] strs = str.split("\n");
		str = strs[strs.length - 2];
        String str2 = str.substring(11, str.indexOf("=")).trim();
        str2 = str2.split("\\s")[0];
        long logState = Long.parseLong(str2);
		return logState;
	}
}
