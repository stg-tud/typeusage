package de.tud.stg.mubench;

import java.io.IOException;

import de.tud.stg.analysis.ObjectTrace;

public class ObjectTraceUtils {

	static String toYaml(ObjectTrace target) throws IOException {
		StringBuffer sb = new StringBuffer();
		sb.append("file: ");
		sb.append(ObjectTraceUtils.getSourceFileName(target));
		sb.append("\n");
		sb.append("missingcalls:\n");
		for (String missingcall : target.missingcalls.keySet()) {
			sb.append("  - ");
			sb.append(missingcall.replaceFirst("call:", ""));
			sb.append("\n");
		}
		return sb.toString();
	}

	static String getSourceFileName(ObjectTrace target) {
		return target.getLocation().split(":")[1].replace('.', '/') + ".java";
	}

}
