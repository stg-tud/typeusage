package de.tud.stg.analysis;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AnalysisDegraded extends ComputePrecisionAndRecall {

	@Override
	public List<DegradedObjectTrace> createEvaluationData(List<ObjectTrace> input) {
		// we don't use input
		List<ObjectTrace> filtered= new ArrayList<ObjectTrace>(); 
		int i=0;
		System.out.println("filtering special cases (keeping only redundant type usages)...");
		for (ObjectTrace o1 : input) {
			boolean unique = true;
			System.out.print("\r"+(i++)+"/"+input.size());
			for (ObjectTrace o2 : input) {
				// we need to have at least on redundancy
				// and is better from a methodocological viewpoint
				if (o1!=o2 && dm.weakEquals(o1, o2)) {
  				  unique = false;
				  break;
				}
			}
			if (!unique) {
				filtered.add(o1);
			}
		}
		System.out.print("\nfound "+filtered.size()+" redundant type-usages");

		
		List<DegradedObjectTrace> output = new ArrayList<DegradedObjectTrace>();
		
		int j=0;
		System.out.println("\nremoving method calls one by one ...");
		for (ObjectTrace o1 : filtered) {
			System.out.print("\r"+(j++)+"/"+filtered.size());

			// to avoid a java.lang.IndexOutOfBoundsException: Index: 0, Size: 0
			// since we wil remove one method call
			// we select only records with at least 2 calls
			// Sep. 24 remove it makes sens to predict also with a single method call
			//if (option_filter_one_call_record  == true) {
			//  if (o1.calls.size()<2) continue;
			//}
			
			// filter
			//if (!o1.getContext().matches("context:create.*")) continue;

			for (String removedId : o1.calls)
			//int removedId = random.nextInt(o1.calls.size()); 
			// by doing so we don't give too much (artificial) importance
			// too records with a large number of method calls
			{
				// then we degrade the object
				// and recompute the values
				DegradedObjectTrace degradedRecord = dm.clone(o1);
				degradedRecord.remove(removedId);
				output.add(degradedRecord);
				//System.out.println("generated "+output.size());
			}
		}
		System.out.print("\ncreated "+output.size()+" test queries (articificial faulty type-usages)");
		return output;
	}


	@Override
	protected BufferedWriter getOutputWriter() throws IOException {
		return new BufferedWriter(new FileWriter(getDatasetFileName().replace('/','-')+"-degraded.dat"));
	}

}
