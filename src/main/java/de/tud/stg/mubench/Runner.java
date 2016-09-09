package de.tud.stg.mubench;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import de.tu_darmstadt.stg.mubench.cli.ArgParser;
import de.tu_darmstadt.stg.mubench.cli.DetectorArgs;
import de.tu_darmstadt.stg.mubench.cli.DetectorFinding;
import de.tu_darmstadt.stg.mubench.cli.DetectorOutput;
import de.tud.stg.analysis.DatasetReader;
import de.tud.stg.analysis.ObjectTrace;
import de.tud.stg.analysis.engine.EcoopEngine;
import typeusage.miner.FileTypeUsageCollector;
import typeusage.miner.TypeUsage;

public class Runner {

	public static void main(String[] args) throws Exception {
		DetectorArgs detectorArgs = ArgParser.parse(args);
		switch (detectorArgs.getDetectorMode()) {
		case DETECT_ONLY:
			detectOnly(detectorArgs);
			break;
		case MINE_AND_DETECT:
			mineAndDetect(detectorArgs);
			break;
		default:
			throw new IllegalArgumentException("Unsupported runmode");
		}
	}

	public static void detectOnly(DetectorArgs detectorArgs) throws FileNotFoundException, Exception, IOException {
		final String targetClassPath = detectorArgs.getTargetClassPath();
		final String trainingClassPath = detectorArgs.getTrainingClassPath();
		final String trainingSrcPath = detectorArgs.getTrainingSrcPath();
		String findingsFile = detectorArgs.getFindingsFile();
		String modelFilename = new File(new File(findingsFile).getParent(), "model.dat").getAbsolutePath();

		final int patternFrequency = 50;
		double minStrangeness = 0.01;
		int maxNumberOfMissingCalls = Integer.MAX_VALUE;
		
		FileTypeUsageCollector collector = new FileTypeUsageCollector(modelFilename) {
			@Override
			protected String[] buildSootArgs() {
				return generateRunArgs(targetClassPath, trainingClassPath, patternFrequency);
			}
			
			@Override
			public void receive(TypeUsage t) {
				int numberOfCopies = isFromPattern(trainingSrcPath, t) ? patternFrequency : 1;
				for (int i = 0; i < numberOfCopies; i++) {
					super.receive(t);
				}
			}
		};
		run(detectorArgs, modelFilename, collector, minStrangeness, maxNumberOfMissingCalls);
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

	public static void mineAndDetect(DetectorArgs detectorArgs) throws FileNotFoundException, Exception, IOException {
		String trainingClassPath = detectorArgs.getTrainingClassPath();
		String modelFilename = new File(new File(detectorArgs.getFindingsFile()).getParent(), "output.dat")
				.getAbsolutePath();
		FileTypeUsageCollector collector = new FileTypeUsageCollector(modelFilename);
		collector.setDirToProcess(trainingClassPath);
		Runner.run(detectorArgs, modelFilename, collector,
				// using values from the paper
				/* strangeness threshold = */ 0.5,
				/* maximum number of missing calls = */ 1);
	}
	
	public static void run(DetectorArgs detectorArgs, String modelFilename, FileTypeUsageCollector usageCollector,
			double minStrangeness, int maxNumberOfMissingCalls) throws Exception, IOException, FileNotFoundException {
		try {
			usageCollector.run();
		} finally {
			usageCollector.close();
		}

		Runner.detect(modelFilename, detectorArgs.getFindingsFile(), minStrangeness, maxNumberOfMissingCalls);
	}

	static void detect(String modelFilename, String outputFile, double minStrangeness, int maxNumberOfMissingCalls)
			throws IOException, Exception {
		DetectorOutput output = new DetectorOutput(outputFile);
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

		output.write();
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

	public static String getType(ObjectTrace target) {
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

	public static Set<String> getPresentCalls(ObjectTrace target) {
		Set<String> presentCalls = new HashSet<String>();
		for (String call : target.calls) {
			presentCalls.add(call.split(":")[1]);
		}
		return presentCalls;
	}

	public static List<String> getMissingCalls(ObjectTrace target) {
		List<String> missingcalls = new LinkedList<String>();
		for (String missingcall : target.missingcalls.keySet()) {
			missingcalls.add(missingcall.split(":")[1]);
		}
		return missingcalls;
	}

	private static String getSourceFileName(ObjectTrace target) {
		return DetectorFinding.convertFQNtoFileName(target.getLocation().split(":")[1]);
	}

	private static String getMethodName(ObjectTrace target) {
		return target.getContext().split(":")[1].replace(",", ", ");
	}

}
