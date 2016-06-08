package de.tud.stg.analysis;

import java.util.*;

import org.apache.commons.lang.StringUtils;

public abstract class TypeUsage {
	private String context;
	private String type;
	private String location;
	public String getLocation() {
		return location;
	}
	public void setLocation(String location) {
		this.location = location;
	}
	public Set<String> calls = new HashSet<String>();
	public String getType() {
		return type;
	}
	public String getContext() {
		return context;
	}
	public TypeUsage setType(String _type) {
		type=_type;
		return this;
	}
	public TypeUsage setContext(String _context) {
		context=_context;
		return this;
	}
	public String toString() {
		return context+" "+type+" "+StringUtils.join(calls," ");
	}
}
