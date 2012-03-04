package com.bobo.data;

/**
 * This is deprecated, because it is not needed. It should be deleted.
 * Java bean used to hold data for java methods.
 * 
 * @author bobo
 * 
 */
public final class MethodInfo {
	private final String name;
	private final String descriptor;
	private final ClassInfo containingClass;

	public MethodInfo(final ClassInfo containingClass, final String name, final String descriptor) {
		this.name = name;
		this.descriptor = descriptor;
		this.containingClass = containingClass;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the descriptor
	 */
	public String getDescriptor() {
		return descriptor;
	}

	@Override
	public String toString() {
		return name + "(" + descriptor + ")";
	}

	/**
	 * @return the containingClass
	 */
	public ClassInfo getContainingClass() {
		return containingClass;
	}
}
