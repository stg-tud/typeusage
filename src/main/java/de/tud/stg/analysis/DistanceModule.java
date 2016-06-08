package de.tud.stg.analysis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import org.junit.Test;



/** 
 * Computes the number of equals and almost equals 
 *
 * @author martin
 */
public final class DistanceModule {

	private boolean option_nocontext = false;
	private int option_k = 1;
	private boolean option_notype = false;
	
	private boolean isOption_nocontext() {
		return option_nocontext;
	}

	public void setOption_nocontext(boolean option_nocontext) {
		this.option_nocontext = option_nocontext;
	}

	private boolean isOption_notype() {
		return option_notype;
	}

	public void setOption_notype(boolean option_notype) {
		this.option_notype = option_notype;
	}

	private int getOption_k() {
		return option_k;
	}

	public void setOption_k(int option_k) {
		this.option_k = option_k;
	}

	public List<String> getParameters() {
		ArrayList<String> l = new ArrayList<String>() ;
		l.add("option_notype = "+option_notype);
		l.add("option_k = "+option_k);
		return l;
	}


	public void printMissingCalls(ObjectTrace o1, Writer w) throws IOException {		
		w.write("\n\n\n -------------------- \n");
		w.write("Strange: "+o1.strangeness()+"\n");
		w.write("Strangev2: "+o1.strangeness2()+"\n");
		w.write("Equals: "+o1.nequals+"\n");
		w.write("Almost: "+o1.nalmostequals+"\n");
		w.write("\tlocation: "+o1.getLocation()+"\n");
		w.write("\tlocation: "+o1.rowNumber+"\n");
		w.write("\tcontext: "+o1.getContext()+"\n");
		w.write("\ttype: "+o1.getType()+"\n");
		//System.out.println(" "+o1.calls.get(0).getLineNumber());
		w.write("\t\tpresent"+o1.calls+"\n");
		w.write("\t\tmissing"+o1.missingcalls+"\n");
		
	}

	public boolean almostEquals(ObjectTrace this_, TypeUsage ob)  {

		if (this_.calls.size() >= ob.calls.size()) return false;
		if (!contextIsValid(this_, ob)) return false;
		if (!typeIsValid(this_, ob)) return false;

		Set<String> missing= getMissingTypeUsage(this_, ob);
		if ((missing.size()>=1) && (missing.size()<=getOption_k())){

			for (String missing_call: missing) {
				int n=(!this_.missingcalls.keySet().contains(missing_call))?0:this_.missingcalls.get(missing_call);
				n++;
				this_.missingcalls.put(missing_call,n);
			}
			return true;

		}
		return false;
	}

	/** returns the calls that are missing in this w.r.t to ob */
	public Set<String> getMissingTypeUsage(TypeUsage this_, TypeUsage ob)  {
		Set<String> calls_this = new HashSet<String>(this_.calls);
		Set<String> missing = new HashSet<String>(ob.calls);
		missing.removeAll(calls_this);
		return missing;
	}
	
	/** a is almost equals to b */
	public boolean almostEqualsTypeUsage(TypeUsage a, TypeUsage b)  {
		if (!contextIsValid(a, b)) return false;
		if (!typeIsValid(a, b)) return false;
		Set<String> missing = new HashSet<String>(b.calls);
		missing.removeAll(new HashSet<String>(a.calls));
		return missing.size()>0 && missing.size()<=getOption_k();
	}

	
	public DegradedObjectTrace clone(ObjectTrace model) {
		DegradedObjectTrace clone = new DegradedObjectTrace();
		clone.original = model;
		cloneInternal(model, clone);
		return clone;
	}


	private void cloneInternal(ObjectTrace model, TypeUsage clone) {
		clone.setContext(model.getContext());
		clone.setType(model.getType());
		clone.calls.addAll(model.calls);
	}
	
	public boolean weakEquals(ObjectTrace this_, ObjectTrace ob)  {
		if (!contextIsValid(this_, ob)) return false;
		if (!typeIsValid(this_, ob)) return false;
		return true;
	}
	
	public boolean equals(ObjectTrace this_, TypeUsage ob)  {

		if (!contextIsValid(this_, ob)) return false;
		if (!typeIsValid(this_, ob)) return false;
		
		if (this_.calls.size() != ob.calls.size()) return false;

		// old implementation
//		// not robust to having several calls in the same record
//		String[] calls_this = this_.calls.toArray(new String[0]);
//		Arrays.sort(calls_this);
//		String[] calls_ob = ob.calls.toArray(new String[0]);
//		Arrays.sort(calls_ob);
//		return Arrays.equals(calls_this, calls_ob);
		
		// new implementation
		// robust to several calls in the same record
		return new HashSet<String>(this_.calls).equals(new HashSet<String>(ob.calls));
	}

	public boolean contextIsValid(TypeUsage this_, TypeUsage ob) {
		if (isOption_nocontext() == true) {
			return true;
		}		
		return this_.getContext().equals(ob.getContext());
	}

	public boolean typeIsValid(TypeUsage this_, TypeUsage ob) {
		if (isOption_notype() == true) {
			return true;
		}		
		return this_.getType().equals(ob.getType());
	}



}
