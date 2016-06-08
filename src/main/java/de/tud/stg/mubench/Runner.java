package de.tud.stg.mubench;

import java.io.FileWriter;

import typeusage.miner.FileTypeUsageCollector;

public class Runner {

	public static void main(String[] args) throws Exception {
		String projectClasspath = "target/classes/";
		String modelFilename = "output.dat";
		String findingsFile = "findings.yml";

		FileTypeUsageCollector c = new FileTypeUsageCollector(modelFilename);
		try {
			c.setDirToProcess(projectClasspath);
			c.run();
		} finally {
			c.close();
		}

		FileWriter writer = new FileWriter(findingsFile);
		try {
			Detector detector = new Detector(modelFilename);
			detector.disableContext();
			writer.write(detector.run());
		} finally {
			writer.close();
		}
	}
}
