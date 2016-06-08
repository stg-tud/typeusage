package de.tud.stg.analysis.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import de.tud.stg.analysis.DegradedObjectTrace;
import de.tud.stg.analysis.DistanceModule;
import de.tud.stg.analysis.ObjectTrace;

public class EcoopEngine implements IMissingCallEngine {

	private DistanceModule dm = new DistanceModule();
	List<ObjectTrace> dataset;

	public boolean option_filterIsEnabled = true;
	public double option_filter_threshold = 0.9;

	public EcoopEngine(List<ObjectTrace> l) {
		dataset = new ArrayList<ObjectTrace>(l);
	}
	
	@Override
	public HashMap<String, Integer> query(ObjectTrace query) {
		// reset
		query.reset();
		
		for (ObjectTrace o2:dataset) {
			if (dm.equals(query,o2)) query.nequals++;
			if (dm.almostEquals(query,o2)) {
				query.nalmostequals++;					
			}
		}
						
		// in this case, there could not be almost equals almost by construction
		// which mechanically degrades the results
		// so we discard them
		
		// however, this does give a better overview of the results thanks to 
		// the if (o2.equals(o1)) continue; above
		// mechanically improve 
		//       * the precision (.84-> .86)
		//       * and especially the answered/recall .78 -> 1
		// so it's better to actually consider the precision
		//  beacause we don't consider hard cases
		// or in other terms it removes all queries that could not be answered directly
//		if (option_norarecase == true && probe==0) {
//			nspecialcase++;
//			continue;
//		}
		
		/*if (nalmostequals2 == 0) {
		  System.out.println(nequals2+" "+nalmostequals2+" "+probe+" "+degradedRecord);
		}*/
		// possible filter on the missing calls with a second threshold
		//if (true){
		if (option_filterIsEnabled){
			List<String> callsToBeFiltered = new ArrayList<String>();
			int nmissing = query.missingcalls.size();
			for(String cs :query.missingcalls.keySet()) {
				if (  (query.missingcalls.get(cs)*1.0)/nmissing  <  option_filter_threshold ) {
					callsToBeFiltered.add(cs); }
			}
			for (String cs:callsToBeFiltered) {
				query.missingcalls.remove(cs);
			}
		}
		
		return query.missingcalls;
	}

	@Override
	public HashMap<String, Integer> simulateQuery(DegradedObjectTrace degraded) {
		
		if (degraded.original != null) {
		  if (!dataset.remove(degraded.original)) throw new RuntimeException();
		}
		HashMap<String, Integer> result = query(degraded);
		if (degraded.original != null) {
			dataset.add(degraded.original);
		}
		return result;
	}

	@Override
	public List<String> getParameters() {
		List<String> l = dm.getParameters() ;
		l.add("option_nofilter = "+option_filterIsEnabled);
		l.add("option_filter_threshold = "+option_filter_threshold);
		return l;
	}

	public void setOption_nocontext(boolean option_nocontext) {
		throw new RuntimeException();
	}
	
	public void setOption_notype(boolean option_notype) {
		throw new RuntimeException();
	}

	public void setOption_k(int option_k) {
		dm.setOption_k(option_k);
	}

	public void dontConsiderType() {
		dm.setOption_notype(true);		
	}

	public void dontConsiderContext() {
		dm.setOption_nocontext(true);		
	}

}
