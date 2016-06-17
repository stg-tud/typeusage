package de.tud.stg.mubench;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.tu_darmstadt.stg.mubench.cli.ArgParser;
import de.tu_darmstadt.stg.mubench.cli.DetectorArgs;
import de.tud.stg.analysis.DatasetReader;
import de.tud.stg.analysis.DistanceModule;
import de.tud.stg.analysis.ObjectTrace;
import typeusage.miner.FileTypeUsageCollector;

public class DetectOnlyRunner {
	// values from the paper
	private static final int K = 1;

	public static void main(String[] args) throws Exception {
		DetectorArgs detectorArgs = ArgParser.parse(args);

		String projectSourcePath = detectorArgs.projectSrcPath;
		String projectClasspath = detectorArgs.projectClassPath;
		String patternClasspath = detectorArgs.patternsClassPath;
		File findingsFile = new File(detectorArgs.findingsFile);
		String modelFilename = new File(findingsFile.getParent(), "model.dat").getAbsolutePath();

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

		detect(model, targets, findingsFile);
	}

	private static boolean isFromProject(ObjectTrace objectTrace, String projectSourcePath) {
		String fileName = ObjectTraceUtils.getSourceFileName(objectTrace);
		return new File(projectSourcePath, fileName).exists();
	}

	private static List<ObjectTrace> collectTypeUsages(final String patternClasspath, final String projectClasspath, String modelFilename) throws Exception {
		FileTypeUsageCollector c = new FileTypeUsageCollector(modelFilename){
			@Override
			protected String[] buildSootArgs() {
				String[] myArgs = {
				        "-soot-classpath", patternClasspath + ":" + projectClasspath,
				        "-pp",// prepend is not required
				        "-process-dir", projectClasspath,
				        "-process-dir", patternClasspath
				    };
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

	private static void detect(List<ObjectTrace> model, List<ObjectTrace> targets, File findingsFile)
			throws IOException, Exception {
		FileWriter writer = new FileWriter(findingsFile);
		BufferedWriter br = null;
		try {
			br = new BufferedWriter(writer);
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
						br.write(ObjectTraceUtils.toYaml(target));
						br.write("\n---\n");
					}
				}
				System.out.println();
				nanalyzed++;
			}
		} finally {
			if (br != null) {
				br.close();
			} else {
				writer.close();
			}
		}
	}
}
