package de.tud.stg.mubench;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import de.tu_darmstadt.stg.mubench.cli.ArgParser;
import de.tu_darmstadt.stg.mubench.cli.DetectorArgs;
import de.tud.stg.analysis.DatasetReader;
import de.tud.stg.analysis.ObjectTrace;
import de.tud.stg.analysis.engine.EcoopEngine;
import typeusage.miner.FileTypeUsageCollector;

public class MineAndDetectRunner {
	private static final double STRANGENESS_THRESHOLD = 0.8;

	public static void main(String[] args) throws Exception {
		DetectorArgs detectorArgs = ArgParser.parse(args);

		String projectClasspath = detectorArgs.projectClassPath;
		File findingsFile = new File(detectorArgs.findingsFile);
		String modelFilename = new File(findingsFile.getParent(), "output.dat").getAbsolutePath();

		FileTypeUsageCollector c = new FileTypeUsageCollector(modelFilename);
		try {
			c.setDirToProcess(projectClasspath);
			c.run();
		} finally {
			c.close();
		}

		detect(modelFilename, findingsFile);
	}

	private static void detect(String modelFilename, File findingsFile) throws IOException, Exception {
		FileWriter writer = new FileWriter(findingsFile);
		BufferedWriter br = null;
		try {
			br = new BufferedWriter(writer);

			List<ObjectTrace> dataset = new DatasetReader().readObjects(modelFilename);
			EcoopEngine engine = new EcoopEngine(dataset);
			engine.dontConsiderContext();

			int nanalyzed = 0;

			System.out.println("\ncomputing precision and recall...");
			for (ObjectTrace record : dataset) {
				{
					System.out.print("\r" + nanalyzed + "/" + dataset.size());

					engine.query(record);
					double strangeness = record.strangeness();
					if (strangeness >= STRANGENESS_THRESHOLD) {
						br.write("file: ");
						br.write(record.getLocation().replaceFirst("location:", "").replace('.', '/'));
						br.write("\n");
						br.write("missingcalls:\n");
						for (String missingcall : record.missingcalls.keySet()) {
							br.write("  - ");
							br.write(missingcall.replaceFirst("call:", ""));
							br.write("\n");
						}
						br.write("\n---\n");
					}
					nanalyzed++;
				}
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
