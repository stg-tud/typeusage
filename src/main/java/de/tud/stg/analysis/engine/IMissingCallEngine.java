package de.tud.stg.analysis.engine;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import de.tud.stg.analysis.DegradedObjectTrace;
import de.tud.stg.analysis.ObjectTrace;

public interface IMissingCallEngine {
	/** returns a list of method calls */
	HashMap<String, Integer> query(ObjectTrace o);

	/** returns a list of method calls */
	HashMap<String, Integer> simulateQuery(DegradedObjectTrace degraded);

	List<String> getParameters();
}
