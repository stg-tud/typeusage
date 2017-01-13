package de.tud.stg.mubench;

import de.tu_darmstadt.stg.mubench.cli.CodePath;
import de.tu_darmstadt.stg.mubench.cli.DetectorFinding;
import de.tu_darmstadt.stg.mubench.cli.DetectorOutput;
import de.tu_darmstadt.stg.mubench.cli.MuBenchRunner;
import de.tud.stg.analysis.DatasetReader;
import de.tud.stg.analysis.ObjectTrace;
import de.tud.stg.analysis.engine.EcoopEngine;
import typeusage.miner.FileTypeUsageCollector;
import typeusage.miner.TypeUsage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DMMCRunner extends MuBenchRunner {

	public static void main(String[] args) throws Exception {
		new DMMCRunner().run(args);
	}

	protected void detectOnly(CodePath patternPath, CodePath targetPath, DetectorOutput output) throws Exception {
		String modelFilename = Files.createTempFile("model", ".dat").toString();
		final String targetClassPath = targetPath.classPath;
		final String trainingClassPath = patternPath.classPath;
		final String trainingSrcPath = patternPath.srcPath;

		final int patternFrequency = 50;
		double minStrangeness = 0.01;
		int maxNumberOfMissingCalls = Integer.MAX_VALUE;

		FileTypeUsageCollector collector = new FileTypeUsageCollector(modelFilename) {
			@Override
			protected String[] buildSootArgs() {
				return generateRunArgs(targetClassPath, trainingClassPath);
			}

			@Override
			public void receive(TypeUsage t) {
				int numberOfCopies = isFromPattern(trainingSrcPath, t) ? patternFrequency : 1;
				for (int i = 0; i < numberOfCopies; i++) {
					super.receive(t);
				}
			}
		};
		run(output, modelFilename, collector, minStrangeness, maxNumberOfMissingCalls);
	}

	private static String[] generateRunArgs(String misuseClasspath, String patternClasspath) {
		return new String[] { "-soot-classpath", misuseClasspath + ":" + patternClasspath,
				"-pp", /* prepend is not required */
				"-process-dir", misuseClasspath, "-process-dir", patternClasspath };
	}

	private static boolean isFromPattern(String patternsSrcPath, TypeUsage t) {
		String location = t.getLocation().split(":")[0];
		String fileName = DetectorFinding.convertFQNtoFileName(location);
		return new File(patternsSrcPath, fileName).exists();
	}

	protected void mineAndDetect(CodePath trainingAndTargetPath, DetectorOutput output) throws Exception {
		String modelFilename = Files.createTempFile("output", ".dat").toString();
		FileTypeUsageCollector collector = new FileTypeUsageCollector(modelFilename);
		collector.setDirToProcess(trainingAndTargetPath.classPath);
		run(output, modelFilename, collector,
				// using values from the paper
				/* strangeness threshold = */ 0.5,
				/* maximum number of missing calls = */ 1);
	}

	private static void run(DetectorOutput output, String modelFilename, FileTypeUsageCollector usageCollector,
							double minStrangeness, int maxNumberOfMissingCalls) throws Exception {
		try {
			usageCollector.run();
		} finally {
			usageCollector.close();
		}

		detect(modelFilename, output, minStrangeness, maxNumberOfMissingCalls);
	}

	private static void detect(String modelFilename, DetectorOutput output, double minStrangeness, int maxNumberOfMissingCalls)
			throws Exception {
		List<ObjectTrace> dataset = new DatasetReader().readObjects(modelFilename);
		EcoopEngine engine = new EcoopEngine(dataset);
		engine.dontConsiderContext();
		engine.setOption_k(maxNumberOfMissingCalls);

		int nanalyzed = 0;

		System.out.println("finding usages with a strangeness of more than " + minStrangeness + "...");
		for (ObjectTrace record : dataset) {
			System.out.print(nanalyzed + "/" + dataset.size());

			engine.query(record);
			double strangeness = record.strangeness();
			if (strangeness >= minStrangeness) {
				System.out.print(" -> violation!");
				addFinding(output, record);
			}
			System.out.println();
			nanalyzed++;
		}
	}

	private static void addFinding(DetectorOutput output, ObjectTrace target) throws IOException {
		String file = getSourceFileName(target);
		String method = getMethodName(target);
		DetectorFinding finding = output.add(file, method);

		finding.put("type", getType(target));
		finding.put("firstcallline", getFirstCallLine(target));
		finding.put("presentcalls", getPresentCalls(target));
		finding.put("missingcalls", getMissingCalls(target));
		finding.put("strangeness", Double.toString(target.strangeness()));
	}

	private static String getType(ObjectTrace target) {
		return target.getType().split(":")[1];
	}

	private static String getFirstCallLine(ObjectTrace target) {
		String[] locationInfo = target.getLocation().split(":");
		if (locationInfo.length > 2) {
			return locationInfo[2];
		} else {
			return "unknown";
		}
	}

	private static Set<String> getPresentCalls(ObjectTrace target) {
		return target.calls.stream().map(call -> call.split(":")[1]).collect(Collectors.toSet());
	}

	private static List<String> getMissingCalls(ObjectTrace target) {
		return target.missingcalls.keySet().stream()
				.map(missingcall -> missingcall.split(":")[1])
				.collect(Collectors.toCollection(LinkedList::new));
	}

	private static String getSourceFileName(ObjectTrace target) {
		return DetectorFinding.convertFQNtoFileName(target.getLocation().split(":")[1]);
	}

	private static String getMethodName(ObjectTrace target) {
		return target.getContext().split(":")[1].replace(",", ", ");
	}

}
