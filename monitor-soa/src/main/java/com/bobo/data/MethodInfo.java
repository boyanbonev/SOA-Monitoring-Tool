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
	private final int hash;
	private final ClassInfo containingClass;

	public MethodInfo(final ClassInfo containingClass, final String name, final String descriptor) {
		this.name = name;
		this.descriptor = descriptor;
		this.hash = name.hashCode() ^ 10 + descriptor.hashCode();
		this.containingClass = containingClass;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof MethodInfo)) {
			return false;
		}
		
		final MethodInfo mi = (MethodInfo) obj;
		return mi.name.equals(name) && mi.descriptor.equals(descriptor);
	}

	@Override
	public int hashCode() {
		return hash;
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
