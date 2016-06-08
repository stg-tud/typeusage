package de.tud.stg.analysis;

import java.util.*;

import org.apache.commons.lang.StringUtils;
/**
 * 		//Note that since the relationship holds for the identity, i.e. xEx is always-
		//valid, E(x) always contains x itself, and |E(x)|  1.

		// well, this reuce the average S-score significantly
		// we remove the 1 again


 * @author martin
 *
 */
public class ObjectTrace extends TypeUsage {
	public int rowNumber; // to get back the source line number
	public HashMap<String, Integer> missingcalls = new HashMap<String, Integer>();
	public int nequals;
	public int nalmostequals;
	public static final double conservative_coef = 2;
	public void reset() {
		nequals=0;
		nalmostequals=0;
	}
	
	public double strangeness() {
		if ((nequals+nalmostequals)==0) {
			return 0;
		}
		return ((double)nalmostequals)/(nequals+nalmostequals); 
	}		
	public double strangeness2() {
		return strangeness2(conservative_coef);
	}
	public double strangeness2(double coef) {
		return (nalmostequals)/((double)(Math.pow(nequals, coef)+nalmostequals));			
	}
	public double strangeness3() {
		return ((double)nalmostequals)/(1+ nequals +nalmostequals);			
	}

}
