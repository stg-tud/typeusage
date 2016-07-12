package de.tud.stg.mubench;

import java.io.IOException;
import java.util.LinkedList;

import de.tu_darmstadt.stg.mubench.cli.DetectorFinding;
import de.tud.stg.analysis.ObjectTrace;

public class ObjectTraceUtils {

	static DetectorFinding toFinding(ObjectTrace target) throws IOException {
		String file = ObjectTraceUtils.getSourceFileName(target);
		String method = ObjectTraceUtils.getMethodName(target);
		DetectorFinding finding = new DetectorFinding(file, method);
		
		
		LinkedList<String> missingcalls = new LinkedList<String>();
		for (String missingcall : target.missingcalls.keySet()) {
			missingcalls.add(missingcall.split(":")[1]);
		}
		
		finding.put("missingcalls", missingcalls);
		return finding;
	}

	static String getSourceFileName(ObjectTrace target) {
		return target.getLocation().split(":")[1].replace('.', '/') + ".java";
	}

	private static String getMethodName(ObjectTrace target) {
		String context = target.getContext().split(":")[1];
		return context.substring(0, context.indexOf('('));
	}
}
