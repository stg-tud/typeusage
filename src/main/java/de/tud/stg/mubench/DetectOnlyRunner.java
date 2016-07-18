package de.tud.stg.mubench;

import java.io.File;

import de.tu_darmstadt.stg.mubench.cli.ArgParser;
import de.tu_darmstadt.stg.mubench.cli.DetectorArgs;
import typeusage.miner.FileTypeUsageCollector;

public class DetectOnlyRunner {
	public static void main(String[] args) throws Exception {
		DetectorArgs detectorArgs = ArgParser.parse(args);

		final String misuseClasspath = detectorArgs.getMisuseClassPath();
		final String patternClasspath = detectorArgs.getPatternsClassPath();
		String modelFilename = new File(new File(detectorArgs.getFindingsFile()).getParent(), "model.dat")
				.getAbsolutePath();

		FileTypeUsageCollector collector = new FileTypeUsageCollector(modelFilename) {
			@Override
			protected String[] buildSootArgs() {
				// duplicate pattern classes
				String[] myArgs = { "-soot-classpath", patternClasspath + ":" + patternClasspath + ":" + misuseClasspath,
						"-pp", /* prepend is not required */
						"-process-dir", misuseClasspath, "-process-dir", patternClasspath, "-process-dir", patternClasspath };
				return myArgs;
			}
		};
		Runner.run(detectorArgs, modelFilename, collector,
				// using values from the paper
				/* strangeness threshold = */ 0.01,
				/* maximum number of missing calls = */ Integer.MAX_VALUE);
	}
}
