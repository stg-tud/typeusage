package de.tud.stg.mubench;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import de.tu_darmstadt.stg.mubench.cli.DetectorArgs;
import de.tu_darmstadt.stg.mubench.cli.DetectorFinding;
import de.tu_darmstadt.stg.mubench.cli.DetectorOutput;
import de.tud.stg.analysis.DatasetReader;
import de.tud.stg.analysis.ObjectTrace;
import de.tud.stg.analysis.engine.EcoopEngine;
import typeusage.miner.FileTypeUsageCollector;

public class Runner {

	public static void run(DetectorArgs detectorArgs, String modelFilename,
			FileTypeUsageCollector usageCollector, double minStrangeness, int maxNumberOfMissingCalls) throws Exception, IOException, FileNotFoundException {
		try {
			usageCollector.run();
		} finally {
			usageCollector.close();
		}
	
		Runner.detect(modelFilename, detectorArgs.getFindingsFile(), minStrangeness, maxNumberOfMissingCalls);
	}

	static void detect(String modelFilename, String outputFile, double minStrangeness, int maxNumberOfMissingCalls) throws IOException, Exception {
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
		
		LinkedList<String> missingcalls = new LinkedList<String>();
		for (String missingcall : target.missingcalls.keySet()) {
			missingcalls.add(missingcall.split(":")[1]);
		}
		
		finding.put("missingcalls", missingcalls);
		finding.put("strangeness", Double.toString(target.strangeness()));
	}

	private static String getSourceFileName(ObjectTrace target) {
		return target.getLocation().split(":")[1].replace('.', '/') + ".java";
	}

	private static String getMethodName(ObjectTrace target) {
		String context = target.getContext().split(":")[1];
		return context.substring(0, context.indexOf('('));
	}

}
