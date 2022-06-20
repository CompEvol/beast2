package test.beast.integration;

import java.io.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import beast.base.util.FileUtils;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 
 * Test for java package dependencies
 * 
 */
public class DependencyTest  {

	
	private static class StreamGobbler implements Runnable {
	    private InputStream inputStream;
	    private Consumer<String> consumer;

	    public StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
	        this.inputStream = inputStream;
	        this.consumer = consumer;
	    }

	    @Override
	    public void run() {
	        new BufferedReader(new InputStreamReader(inputStream)).lines()
	          .forEach(consumer);
	    }
	}
	
	@Test
	public void testMutualDependencyOfPackageDependencies() throws Exception {
		
		// obtain package dependencies in dot file by executing
		// jdeps -dotoutput /tmp/jdeps/ beast
		
		String jdepsDir = "/tmp/jdeps";
		
		System.err.println("Running jdeps...");
		if (!new File(jdepsDir).exists()) {
			new File(jdepsDir).mkdirs();
		}
		ProcessBuilder builder = new ProcessBuilder();
		builder.command("jdeps", "-dotoutput", jdepsDir+"/", "beast");
		String dir = System.getProperty("user.dir");
		if (dir.indexOf("/src/") > 0) {			
			dir = dir.substring(0, dir.indexOf("/src/"));
		}
		builder.directory(new File(dir + "/build/"));
		Process process = builder.start();
		StreamGobbler streamGobbler = 
		  new StreamGobbler(process.getInputStream(), System.out::println);
		Executors.newSingleThreadExecutor().submit(streamGobbler);
		int exitCode = process.waitFor();
		assert exitCode == 0;

		// parse /tmp/jdeps/beast.dot for dependencies
		System.err.println("Processing dependencies");
		String dotFile = FileUtils.load(new File(jdepsDir + "/beast.dot"));
		Set<String> edges = new HashSet<>();	
		for (String str : dotFile.split("\n")) {
			if (str.contains("->")) {
				String [] strs = str.split("->");
				String from = strs[0].trim().replaceAll("\"","");
				String to = strs[1].replaceAll("\"","");
				if (to.indexOf("(") > 0) {
					to = to.substring(0, to.indexOf("(")).trim();
				}
				edges.add(from + "->" + to);
			}
		}

		
		Set<String> cycles = new HashSet<>();
		
		for (String edge : edges) {
			String [] str = edge.split("->");
			String from = str[0];
			String to = str[1];			
			String d = to + "->" + from;
			if (edges.contains(d)) {
				if (from.compareTo(to)> 0) {
					cycles.add(from + " <-> " + to);
				} else {
					cycles.add(to + " <-> " + from);
				}
			}	
		}
		
		if (cycles.size() > 0) {
			System.err.println("Cycles found:");
			for (String cycle : cycles) {
				System.err.println(cycle);
			}
		} else {			
			System.err.println("Bravo! No cycles of length 2 found");
		}
		assertEquals(cycles.size(), 0);
		
		// test for cycles longer than 2
		Map<String, Set<String>> neighbours = new HashMap<>();
		for (String edge : edges) {
			String [] str = edge.split("->");
			String from = str[0];
			String to = str[1];			
			if (from.startsWith("beast")) {
				if (!neighbours.containsKey(from)) {
					neighbours.put(from, new HashSet<>());
				}
				if (to.startsWith("beast")) {
					neighbours.get(from).add(to);
				}
			}
		}
		
		// create partial order
		List<String> order = new ArrayList<>();
		Set<String> done = new HashSet<>();
		boolean progress = true;
		while (progress && neighbours.size() > 0) {
			progress = false;
			for (String n : neighbours.keySet()) {
				Set<String> neighbors = neighbours.get(n);
				boolean canOrder = true;
				for (String n2 : neighbors) {
					if (!done.contains(n2)) {
						canOrder = false;
						break;
					}
				}
				if (canOrder) {
					order.add(n);
					done.add(n);
					neighbours.remove(n);
					progress = true;
					break;
				}
			}
		}
		
		
		// report if the ordering cannot be completed
		if (!progress) {
			System.err.println("Cyclic dependency larger than 2 detected");
			for (String str : order) {
				System.err.println(str);
			}
			System.err.println("Fine so far, but cannot add the following:");
			for (String n : neighbours.keySet()) {
				System.err.print(n);
				System.err.print(" depends on ");
				for (String n2 : neighbours.get(n)) {
					if (!done.contains(n2)) {
						System.err.print(n2 + ", ");
					}									
				}
				System.err.print(" Done: ");
				for (String n2 : neighbours.get(n)) {
					if (done.contains(n2)) {
						System.err.print(n2 + ", ");
					}									
				}
				System.err.println();
			}						
		}
		
		assertEquals(true, progress);
		System.err.println("Bravo! No cycles of length >2 found");
		System.err.print("Done");		
	}
}
