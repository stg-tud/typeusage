package de.tud.stg.mubench;

import java.util.List;

import de.tud.stg.analysis.DatasetReader;
import de.tud.stg.analysis.ObjectTrace;
import de.tud.stg.analysis.engine.EcoopEngine;

/**
 * @author sven
 */
public class Detector {
	private final List<ObjectTrace> dataset;
	private final EcoopEngine engine;
	private double strangenessThreshold = 0.8;

	public Detector(String datasetFilename) throws Exception {
		this.dataset = new DatasetReader().readObjects(datasetFilename);
		this.engine = new EcoopEngine(dataset);
	}

	public String run() throws Exception {
		StringBuffer output = new StringBuffer();
		int nanalyzed = 0;

		System.out.println("\ncomputing precision and recall...");
		for (ObjectTrace record : dataset) {
			{
				System.out.print("\r" + nanalyzed + "/" + dataset.size());

				engine.query(record);
				double strangeness = record.strangeness();

				if (record.nequals == 0 && record.nalmostequals == 0) {
					// there is no equals and no almost equals
					// we really can not say anything
				} else if (strangeness >= strangenessThreshold) {
					output.append("file: ");
					output.append(record.getLocation());
					output.append("\n");
					output.append("missingcalls:\n");
					for (String missingcall : record.missingcalls.keySet()) {
						output.append("  - ");
						output.append(missingcall);
						output.append("\n");
					}
					output.append("\n---\n");
				}
				nanalyzed++;
			}
		}
		return output.toString();
	}

	public void setEcoopEngineThreshold(double d) throws Exception {
		if (engine.option_filterIsEnabled == false) {
			throw new RuntimeException("option_filterIsEnabled is false");
		}
		engine.option_filter_threshold = d;

	}

	public void enableFiltering() throws Exception {
		engine.option_filterIsEnabled = true;
	}

	protected void disableFiltering() throws Exception {
		engine.option_filterIsEnabled = false;
	}

	public void setOption_k(int option_k) throws Exception {
		engine.setOption_k(option_k);
	}

	public void setOption_nocontext(boolean option_nocontext) {
		throw new RuntimeException();
	}

	public void setOption_notype(boolean option_notype) {
		throw new RuntimeException();
	}

	public void setEcoopDontConsiderType() throws Exception {
		engine.dontConsiderType();
	}

	public void disableContext() throws Exception {
		engine.dontConsiderContext();
	}

	public void setStrangenessThreshold(double strangenessThreshold) {
		this.strangenessThreshold = strangenessThreshold;
	}

}
