package de.tud.stg.mubench;

import java.io.File;
import java.io.FileWriter;

import de.tu_darmstadt.stg.mubench.cli.ArgParser;
import de.tu_darmstadt.stg.mubench.cli.DetectorArgs;
import typeusage.miner.FileTypeUsageCollector;

public class Runner {

	public static void main(String[] args) throws Exception {
		DetectorArgs detectorArgs = ArgParser.parse(args);
		
		String projectClasspath = detectorArgs.projectClassPath;
		File findingsFile = new File(detectorArgs.findingsFile);
		String modelFilename = new File(findingsFile.getParent(), "output.dat").getAbsolutePath();

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
