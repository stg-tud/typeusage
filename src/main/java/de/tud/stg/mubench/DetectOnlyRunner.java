package de.tud.stg.mubench;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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

		String projectClasspath = detectorArgs.projectClassPath;
		String patternClasspath = detectorArgs.patternsClassPath;
		File findingsFile = new File(detectorArgs.findingsFile);
		String targetFilename = new File(findingsFile.getParent(), "target.dat").getAbsolutePath();
		String modelFilename = new File(findingsFile.getParent(), "model.dat").getAbsolutePath();

		List<ObjectTrace> targets = collectTypeUsages(projectClasspath, targetFilename);
		List<ObjectTrace> model = collectTypeUsages(patternClasspath, modelFilename);

		detect(model, targets, findingsFile);
	}

	private static List<ObjectTrace> collectTypeUsages(String classpath, String tmpFilename) throws Exception {
		FileTypeUsageCollector c = new FileTypeUsageCollector(tmpFilename);
		try {
			c.setDirToProcess(classpath);
			c.run();
			return new DatasetReader().readObjects(tmpFilename);
		} finally {
			c.close();
		}
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
				System.out.print("\r" + nanalyzed + "/" + targets.size());
				for (ObjectTrace pattern : model) {
					if (dm.almostEquals(target, pattern)) {
						br.write("file: ");
						br.write(target.getLocation().replaceFirst("location:", "").replace('.', '/'));
						br.write("\n");
						br.write("missingcalls:\n");
						for (String missingcall : target.missingcalls.keySet()) {
							br.write("  - ");
							br.write(missingcall.replaceFirst("call:", ""));
							br.write("\n");
						}
						br.write("\n---\n");
					}
				}
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
