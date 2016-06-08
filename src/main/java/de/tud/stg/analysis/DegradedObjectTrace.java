package de.tud.stg.analysis;

public class DegradedObjectTrace extends ObjectTrace {
	public ObjectTrace original;
	String removedMethodCall;
	public void remove(String removedId) {
		removedMethodCall=removedId;	
		calls.remove(removedId);
	}
}
