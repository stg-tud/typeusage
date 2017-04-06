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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
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
				int numberOfCopies = isFromPattern(trainingSrcPath, t.getLocation()) ? patternFrequency : 1;
				for (int i = 0; i < numberOfCopies; i++) {
					super.receive(t);
				}
			}
		};
		run(output, modelFilename, collector, minStrangeness, maxNumberOfMissingCalls,
				usage -> !isFromPattern(trainingSrcPath, usage.getLocation()), false);
	}

	private static String[] generateRunArgs(String misuseClasspath, String patternClasspath) {
		return new String[] { "-soot-classpath", misuseClasspath + ":" + patternClasspath,
				"-pp", /* prepend is not required */
				"-process-dir", misuseClasspath, "-process-dir", patternClasspath };
	}

	private static boolean isFromPattern(String patternsSrcPath, String location) {
		String typeName = convertLocationToFQN(location);
		String fileName = DetectorFinding.convertFQNtoFileName(typeName);
		return new File(patternsSrcPath, fileName).exists();
	}

	private static String convertLocationToFQN(String location) {
		return location.split(":")[location.startsWith("location:") ? 1 : 0];
	}

	protected void mineAndDetect(CodePath trainingAndTargetPath, DetectorOutput output) throws Exception {
		String modelFilename = Files.createTempFile("output", ".dat").toString();
		FileTypeUsageCollector collector = new FileTypeUsageCollector(modelFilename);
		collector.setDirToProcess(trainingAndTargetPath.classPath);
		run(output, modelFilename, collector,
				// using values from the paper
				/* strangeness threshold = */ 0.5,
				/* maximum number of missing calls = */ 1,
				usage -> true, true);
	}

	private static void run(DetectorOutput output, String modelFilename, FileTypeUsageCollector usageCollector,
							double minStrangeness, int maxNumberOfMissingCalls,
							Predicate<ObjectTrace> targetUsage, boolean filterMissingMethodsWithSmallSupport) throws Exception {
		try {
			usageCollector.run();
		} finally {
			usageCollector.close();
		}

		detect(modelFilename, output, minStrangeness, maxNumberOfMissingCalls, targetUsage, filterMissingMethodsWithSmallSupport);
	}

	private static void detect(String modelFilename, DetectorOutput output,
							   double minStrangeness, int maxNumberOfMissingCalls,
							   Predicate<ObjectTrace> targetUsage,
							   boolean filterMissingMethodsWithSmallSupport)
			throws Exception {
		List<ObjectTrace> dataset = new DatasetReader().readObjects(modelFilename);
		EcoopEngine engine = new EcoopEngine(dataset);
		engine.option_filterIsEnabled = filterMissingMethodsWithSmallSupport;
		engine.dontConsiderContext();
		engine.setOption_k(maxNumberOfMissingCalls);

		int nanalyzed = 0;

		System.out.println("finding usages with a strangeness of more than " + minStrangeness + "...");
        List<ObjectTrace> findings = new ArrayList<>();
		for (ObjectTrace record : dataset) {
			System.out.print(nanalyzed + "/" + dataset.size());

			if (targetUsage.test(record)) {
				engine.query(record);
				double strangeness = record.strangeness();
				if (strangeness >= minStrangeness) {
					System.out.print(" -> violation!");
					findings.add(record);
				}
			}
			System.out.println();
			nanalyzed++;
		}

		findings.sort((f1, f2) -> Double.compare(f2.strangeness(), f1.strangeness()));

        for (ObjectTrace finding : findings) {
            addFinding(output, finding);
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
		return DetectorFinding.convertFQNtoFileName(convertLocationToFQN(target.getLocation()));
	}

	private static String getMethodName(ObjectTrace target) {
		String methodName = target.getContext().split(":")[1].replace(",", ", ");
		if (methodName.startsWith("<init>")) {
            String typeName = convertLocationToFQN(target.getLocation());
			typeName = typeName.substring(typeName.lastIndexOf(".") + 1);
			typeName = typeName.substring(typeName.lastIndexOf("$") + 1);
            methodName = typeName + methodName.substring("<init>".length());
        }
		return methodName;
	}

}
