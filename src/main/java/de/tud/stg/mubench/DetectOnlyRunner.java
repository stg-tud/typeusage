package de.tud.stg.mubench;

import java.io.File;

import de.tu_darmstadt.stg.mubench.cli.ArgParser;
import de.tu_darmstadt.stg.mubench.cli.DetectorArgs;
import de.tu_darmstadt.stg.mubench.cli.DetectorFinding;
import typeusage.miner.FileTypeUsageCollector;
import typeusage.miner.TypeUsage;

public class DetectOnlyRunner {
	public static void main(String[] args) throws Exception {
		DetectorArgs detectorArgs = ArgParser.parse(args);
		final String misuseClasses = detectorArgs.getMisuseClassPath();
		final String patternClasses = detectorArgs.getPatternsClassPath();
		final String patternsSrcPath = detectorArgs.getPatternsSrcPath();
		String findingsFile = detectorArgs.getFindingsFile();
		String modelFilename = new File(new File(findingsFile).getParent(), "model.dat").getAbsolutePath();

		final int patternFrequency = 50;
		double minStrangeness = 0.01;
		int maxNumberOfMissingCalls = Integer.MAX_VALUE;
		
		FileTypeUsageCollector collector = new FileTypeUsageCollector(modelFilename) {
			@Override
			protected String[] buildSootArgs() {
				return generateRunArgs(misuseClasses, patternClasses, patternFrequency);
			}
			
			@Override
			public void receive(TypeUsage t) {
				int numberOfCopies = isFromPattern(patternsSrcPath, t) ? patternFrequency : 1;
				for (int i = 0; i < numberOfCopies; i++) {
					super.receive(t);
				}
			}
		};
		Runner.run(detectorArgs, modelFilename, collector, minStrangeness, maxNumberOfMissingCalls);
	}

	private static String[] generateRunArgs(String misuseClasspath, String patternClasspath, int patternFrequency) {
		return new String[] { "-soot-classpath", misuseClasspath + ":" + patternClasspath,
				"-pp", /* prepend is not required */
				"-process-dir", misuseClasspath, "-process-dir", patternClasspath
		};
	}
	
	private static boolean isFromPattern(String patternsSrcPath, TypeUsage t) {
		String location = t.getLocation().split(":")[0];
		String fileName = DetectorFinding.convertFQNtoFileName(location);
		return new File(patternsSrcPath, fileName).exists();
	}
}
