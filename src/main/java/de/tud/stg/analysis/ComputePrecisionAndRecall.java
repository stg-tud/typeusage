package de.tud.stg.analysis;

import java.io.*;
import java.util.*;

import javax.activation.FileTypeMap;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import de.tud.stg.analysis.engine.EcoopEngine;
import de.tud.stg.analysis.engine.IMissingCallEngine;


/** Computes the performance of the system
 * @author martin
 */
public abstract class ComputePrecisionAndRecall { 

	protected DistanceModule dm = new DistanceModule();
	
		
	private List<ObjectTrace> l;
	
	public List<ObjectTrace> getEvaluationDataSet() {
		if (l == null) throw new RuntimeException();
		return l;
	}


	// default engine
	private EcoopEngine ecoopEngine;
	protected IMissingCallEngine engine ;


	private String suffix = ""; // default value	
	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}

	protected IMissingCallEngine getEngine() throws Exception {
		if (engine != null) return engine;
		if (ecoopEngine == null) {
			ecoopEngine = new EcoopEngine(defaultDataset());
		}
		return ecoopEngine;
	}
	
	public void setEvaluationDataSet(List<ObjectTrace> list) {
		if (list == null) throw new RuntimeException();
		l = list;
	}

	public abstract List<DegradedObjectTrace> createEvaluationData(List<ObjectTrace> input);
		
	boolean display_tmp = false;
	
	public String run() throws Exception {
		
		int nanalyzed=0;
		int nanswered=0;

		
		// everytinh is float in order not to ennoyed by euclidean division
		double nperfect=0;
		double ntoomuch=0;
		double nspecialcase=0;
		double nkilled=0;
		double nfalse=0;
		double precision_score=0;
		double recall_score=0;
		double strangeness_score=0;
		double strangeness_score2=0;
		double strangeness_score3=0;
		double strangeness_score4=0;
		Random random = new Random();
		List<String> overview = new ArrayList<String>();
		
		IMissingCallEngine engine = getEngine();
		
		System.out.println("creating evaluation data...");
		List<DegradedObjectTrace> testCases = createEvaluationData(getEvaluationDataSet());

		Writer output= getOutputWriter();
		
		List<Double> medianlist = new ArrayList<Double>();

		System.out.println("\ncomputing precision and recall...");
		for (DegradedObjectTrace degradedRecord : testCases) {
			{				
				System.out.print("\r"+nanalyzed+"/"+testCases.size());
				engine.simulateQuery(degradedRecord);
								
				double strangeness = degradedRecord.strangeness();
				medianlist.add(strangeness);
				strangeness_score+=strangeness;
				
				if (strangeness<0.1) {
					strangeness_score2++;
				}
				if (strangeness>0.9) {
					strangeness_score3++;
				}
				if (strangeness>0.5) {
					strangeness_score4++;
				}
				
				// there is no equals and no almost equals
				// we really can not say anything
				if (degradedRecord.nequals==0 && degradedRecord.nalmostequals ==0) {
					nspecialcase++;
				}
				
				nanalyzed++;
				output.write(strangeness+" "+degradedRecord.nequals+" "+degradedRecord.nalmostequals+"\n");

				if (strangeness>0.8) {nkilled++;}
//				else {
//					degradedRecord.missingcalls.clear();
//				}

				
				if (degradedRecord.missingcalls.size()>0) {
					nanswered++;
				} else {
					//System.out.println("I am in ");
				}

								
				if (degradedRecord.missingcalls.keySet().contains(degradedRecord.removedMethodCall)) {
					
					//System.out.println(degradedRecord.missingcalls.keySet().size());
					precision_score+=1.0/(degradedRecord.missingcalls.keySet().size());
					recall_score+=1;
					
					if (degradedRecord.missingcalls.keySet().size()==1) {
						nperfect++;							
					} else {
						ntoomuch++;						
					}
				}
				else if (degradedRecord.missingcalls.keySet().size()>0) {
					// answered but false
					// we increase neither the precision nor the recall
					nfalse++;					
				}
				
				overview.clear();
				overview.add("#queries:"+Integer.toString(nanalyzed));
				overview.add(String.format("killed:%2.2f", ((nkilled*1.0)/nanalyzed)));
				//overview.add(String.format("found:%2.2f", (((ntoomuch+nperfect)*1.0)/nanalyzed))
				overview.add(String.format("specialcase:%2.2f", ((nspecialcase*1.0)/testCases.size())));
				overview.add(String.format("answered:%2.2f", ((nanswered*1.0)/nanalyzed)));
				
				// nanswered block
				overview.add(String.format("false:%2.2f", (nfalse/nanswered)));
				overview.add(String.format("correct:%2.2f", ((ntoomuch+nperfect)/nanswered)));
				//overview.add(String.format("found2:%2.2f", (recall_score/nanswered)));
				
				overview.add(String.format("perfect:%2.2f", (nperfect/nanswered)));
				overview.add(String.format("toomuch:%2.2f", (ntoomuch/nanswered)));
				
				overview.add(String.format("precision:%2.2f", (precision_score/(nanswered))));				
				overview.add(String.format("recall:%2.2f", (recall_score/nanalyzed)));
				overview.add(String.format("mean-sscore:%2.2f", ((strangeness_score*1.0)/nanalyzed)));		
				overview.add(String.format("sscore<.1:%2.4f", ((strangeness_score2*1.0)/nanalyzed)));		
				overview.add(String.format("sscore>.9:%2.4f", ((strangeness_score3*1.0)/nanalyzed)));		
				overview.add(String.format("sscore>.5:%2.4f", ((strangeness_score4*1.0)/nanalyzed)));		
				//overview.add(String.format("sscoretmp:%2.2f", strangeness));
				if (display_tmp) System.out.println(StringUtils.join(overview, " "));

			}

		}
		output.close();
		
		// computing and adding the median
		Collections.sort(medianlist);
		if (medianlist.size()>1)  {
			overview.add(String.format("median-sscore:%2.4f", medianlist.get(medianlist.size()/2))); 
		}
		
		IOUtils.copy(new StringBufferInputStream(StringUtils.join(overview, "\n")), new FileOutputStream(getDatasetFileName().replace('/', '-')+getClass().getName()+suffix ));				
		return "\n"+StringUtils.join(overview, "\n");
	}

	protected BufferedWriter getOutputWriter() throws IOException {
		return new BufferedWriter(new StringWriter());
	}
	
	

	public String getExperimentParameters() throws Exception {

		List<String> result = new ArrayList<String>();
		result.add("dataset("+getDatasetFileName()+")");
		result.addAll(getEngine().getParameters());
		return StringUtils.join(result, " ");
	}
	
	private String datasetFileName = null;
	public String getDatasetFileName() {
		if (datasetFileName == null) {
			throw new RuntimeException();
		}
		return datasetFileName;
	}
	public void setDatasetFileName(String s) {
		if (datasetFileName != null) {
			throw new RuntimeException("already set");
		}
		datasetFileName = s;
	}
	
	/** default dataset, noth a getter and a setter */
	protected List<ObjectTrace> defaultDataset() throws Exception {
		List<ObjectTrace> defaultDataSet =new DatasetReader().readObjects(getDatasetFileName());
		setEvaluationDataSet(defaultDataSet);
		return defaultDataSet;
	}

	public void setEcoopEngineThreshold(double d) throws Exception {
		if (((EcoopEngine)getEngine()).option_filterIsEnabled == false) {
			throw new RuntimeException("option_filterIsEnabled is false");
		}
		((EcoopEngine)getEngine()).option_filter_threshold = d;
		
	}

	public void enableFiltering() throws Exception {
		((EcoopEngine)getEngine()).option_filterIsEnabled = true;
	}

	protected void disableFiltering() throws Exception {
		((EcoopEngine)getEngine()).option_filterIsEnabled = false;
	}

	public void setOption_k(int option_k)  throws Exception {
		((EcoopEngine)getEngine()).setOption_k(option_k);
	}

	public void setOption_nocontext(boolean option_nocontext) {
		throw new RuntimeException();
	}

	public void setOption_notype(boolean option_notype) {
		throw new RuntimeException();
	}


	public void setEcoopDontConsiderType() throws Exception  {
		((EcoopEngine)getEngine()).dontConsiderType();		
	}

	public void disableContext()  throws Exception {
		((EcoopEngine)getEngine()).dontConsiderContext();		
	}

	
}
