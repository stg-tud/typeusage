package de.tud.stg.analysis;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;

import org.junit.Test;

import de.tud.stg.analysis.engine.EcoopEngine;
import de.tud.stg.analysis.engine.IMissingCallEngine;

public class Main  {

	public static void main(String[] args) throws Exception {

		ComputePrecisionAndRecall analysis;
		analysis = new AnalysisDegraded();
		analysis.setDatasetFileName("eclipse-soot-swt-v5.dat");
		analysis.setOption_k(1);
		System.out.println(analysis.run());

	}
}

/*








*/