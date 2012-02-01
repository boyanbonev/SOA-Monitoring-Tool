/**
 *
 * @author bobo
 *
 */
package com.bobo.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Java bean used to hold data for java classes and their methods that are going to be monitored.
 * 
 * @author bobo
 * 
 */
public final class ClassInfo {
	private String name;
	private String pathToClassFile;
	private List<MethodInfo> methods;

	public ClassInfo() {
	}

	public ClassInfo(final String name, final List<MethodInfo> methods, final String pathToClassFile) {
		this.name = name;
		if (methods != null) {
			this.methods = new ArrayList<MethodInfo>(methods);
		} else {
			this.methods = Collections.emptyList();
		}
		this.pathToClassFile = pathToClassFile;
	}

	public ClassInfo(final ClassInfo classInfo) {
		this.name = classInfo.name;
		this.pathToClassFile = classInfo.pathToClassFile;
		this.methods = new ArrayList<MethodInfo>(classInfo.methods);
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 *            the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the methods
	 */
	public List<MethodInfo> getMethods() {
		return methods;
	}

	/**
	 * @param methods
	 *            the methods to set
	 */
	public void setMethods(final List<MethodInfo> methods) {
		this.methods = new ArrayList<MethodInfo>(methods);
	}

	/**
	 * @return the pathToClassFile
	 */
	public String getPathToClassFile() {
		return pathToClassFile;
	}

	/**
	 * @param pathToClassFile
	 *            the pathToClassFile to set
	 */
	public void setPathToClassFile(String pathToClassFile) {
		this.pathToClassFile = pathToClassFile;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder(getName());
		sb.append(":");
		for (MethodInfo method : getMethods()) {
			sb.append("\n").append("  ").append(method.getName()).append(method.getDescriptor());
		}
		sb.append("\n");
		
		return sb.toString();
	}

}
