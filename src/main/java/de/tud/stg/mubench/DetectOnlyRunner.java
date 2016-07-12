package de.tud.stg.mubench;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.tu_darmstadt.stg.mubench.cli.ArgParser;
import de.tu_darmstadt.stg.mubench.cli.DetectorArgs;
import de.tu_darmstadt.stg.mubench.cli.DetectorOutput;
import de.tud.stg.analysis.DatasetReader;
import de.tud.stg.analysis.DistanceModule;
import de.tud.stg.analysis.ObjectTrace;
import typeusage.miner.FileTypeUsageCollector;

public class DetectOnlyRunner {
	// values from the paper
	private static final int K = 1;

	public static void main(String[] args) throws Exception {
		DetectorArgs detectorArgs = ArgParser.parse(args);
		DetectorOutput output = new DetectorOutput(detectorArgs);

		String projectSourcePath = detectorArgs.getPatternsSrcPath();
		String projectClasspath = detectorArgs.getProjectClassPath();
		String patternClasspath = detectorArgs.getPatternsClassPath();
		String modelFilename = new File(new File(detectorArgs.getFindingsFile()).getParent(), "model.dat")
				.getAbsolutePath();

		List<ObjectTrace> all = collectTypeUsages(patternClasspath, projectClasspath, modelFilename);
		List<ObjectTrace> model = new ArrayList<ObjectTrace>();
		List<ObjectTrace> targets = new ArrayList<ObjectTrace>();
		System.out.println("Usages: " + all.size());
		for (ObjectTrace objectTrace : all) {
			if (isFromProject(objectTrace, projectSourcePath)) {
				System.out.println("Target: " + ObjectTraceUtils.getSourceFileName(objectTrace));
				targets.add(objectTrace);
			} else {
				System.out.println("Pattern: " + ObjectTraceUtils.getSourceFileName(objectTrace));
				model.add(objectTrace);
			}
		}

		detect(model, targets, output);
	}

	private static boolean isFromProject(ObjectTrace objectTrace, String projectSourcePath) {
		String fileName = ObjectTraceUtils.getSourceFileName(objectTrace);
		return new File(projectSourcePath, fileName).exists();
	}

	private static List<ObjectTrace> collectTypeUsages(final String patternClasspath, final String projectClasspath,
			String modelFilename) throws Exception {
		FileTypeUsageCollector c = new FileTypeUsageCollector(modelFilename) {
			@Override
			protected String[] buildSootArgs() {
				String[] myArgs = { "-soot-classpath", patternClasspath + ":" + projectClasspath,
						"-pp", /* prepend is not required */
						"-process-dir", projectClasspath, "-process-dir", patternClasspath };
				return myArgs;
			}
		};
		try {
			c.run();
		} finally {
			c.close();
		}
		return new DatasetReader().readObjects(modelFilename);
	}

	private static void detect(List<ObjectTrace> model, List<ObjectTrace> targets, DetectorOutput output)
			throws IOException, Exception {
		DistanceModule dm = new DistanceModule();
		dm.setOption_nocontext(true);
		dm.setOption_k(K);

		int nanalyzed = 0;

		System.out.println("finding targets that are almost-equal to patterns...");
		for (ObjectTrace target : targets) {
			System.out.print(nanalyzed + "/" + targets.size());
			for (ObjectTrace pattern : model) {
				if (dm.almostEquals(target, pattern)) {
					System.out.print(" -> violation!");
					output.add(ObjectTraceUtils.toFinding(target));
				}
			}
			System.out.println();
			nanalyzed++;
		}
		
		output.write();
	}
}
