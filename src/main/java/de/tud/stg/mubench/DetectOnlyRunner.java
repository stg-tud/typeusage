package de.tud.stg.mubench;

import java.io.File;

import de.tu_darmstadt.stg.mubench.cli.ArgParser;
import de.tu_darmstadt.stg.mubench.cli.DetectorArgs;
import typeusage.miner.FileTypeUsageCollector;

public class DetectOnlyRunner {
	public static void main(String[] args) throws Exception {
		DetectorArgs detectorArgs = ArgParser.parse(args);
		final String misuseClasses = detectorArgs.getMisuseClassPath();
		final String patternClasses = detectorArgs.getPatternsClassPath();
		String findingsFile = detectorArgs.getFindingsFile();
		String modelFilename = new File(new File(findingsFile).getParent(), "model.dat").getAbsolutePath();

		final int patternFrequency = 50;
		double minStrangeness = 0.75;
		int maxNumberOfMissingCalls = Integer.MAX_VALUE;
		
		FileTypeUsageCollector collector = new FileTypeUsageCollector(modelFilename) {
			@Override
			protected String[] buildSootArgs() {
				return generateRunArgs(misuseClasses, patternClasses, patternFrequency);
			}
		};
		Runner.run(detectorArgs, modelFilename, collector, minStrangeness, maxNumberOfMissingCalls);
	}

	public static String[] generateRunArgs(String misuseClasspath, String patternClasspath, int patternFrequency) {
		String input = misuseClasspath;
		for (int i = 0; i < patternFrequency; i++) {
			input += ":" + patternClasspath;
		}

		int numberOfBaseArgs = 5;
		String[] runArgs = new String[numberOfBaseArgs + (2 * patternFrequency)];
		runArgs[0] = "-soot-classpath";
		runArgs[1] = input;
		runArgs[2] = "-pp"; // prepend is not required
		runArgs[3] = "-process-dir";
		runArgs[4] = misuseClasspath;
		for (int i = 0; i < patternFrequency; i++) {
			runArgs[numberOfBaseArgs + (2 * i)] = "-process-dir";
			runArgs[numberOfBaseArgs + (2 * i) + 1] = patternClasspath;
		}
		return runArgs;
	}
}
