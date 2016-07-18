package de.tud.stg.mubench;

import java.io.File;

import de.tu_darmstadt.stg.mubench.cli.ArgParser;
import de.tu_darmstadt.stg.mubench.cli.DetectorArgs;
import typeusage.miner.FileTypeUsageCollector;

public class MineAndDetectRunner {

	public static void main(String[] args) throws Exception {
		DetectorArgs detectorArgs = ArgParser.parse(args);
		String projectClasspath = detectorArgs.getProjectClassPath();
		String modelFilename = new File(new File(detectorArgs.getFindingsFile()).getParent(), "output.dat")
				.getAbsolutePath();
		FileTypeUsageCollector collector = new FileTypeUsageCollector(modelFilename);
		collector.setDirToProcess(projectClasspath);
		Runner.run(detectorArgs, modelFilename, collector,
				// using values from the paper
				/* strangeness threshold = */ 0.5,
				/* maximum number of missing calls = */ 1);
	}
}
