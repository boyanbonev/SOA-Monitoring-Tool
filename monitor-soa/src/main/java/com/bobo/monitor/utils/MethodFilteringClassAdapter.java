/**
 *
 * @author bobo
 *
 */
package com.bobo.monitor.utils;

import java.util.List;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import com.bobo.data.ClassInfo;
import com.bobo.data.MethodInfo;
import com.bobo.monitor.MonitorEventDispatcher;

/**
 * Class adapter used to filter class' methods by name and description(signature).
 * 
 * @author bobo
 * 
 */
class MethodFilteringClassAdapter extends ClassAdapter {

	private final ClassInfo classInfo;

	/**
	 * @param cv
	 */
	public MethodFilteringClassAdapter(final ClassVisitor cv, final ClassInfo classInfo) {
		super(cv);
		this.classInfo = classInfo;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor methodVisitor = super.visitMethod(access, name, desc, signature, exceptions);
		if (methodVisitor != null && shouldEnableMonitoringForMethod(name, desc, classInfo.getMethods())) {
			// if the method should be monitored, change the default method adapter with one that will generate code in
			// the beginning of the method when visitMethod is invoked
			methodVisitor = new TryFinallyMethodAdviceAdapter(methodVisitor, access, name, desc, classInfo.getName(), MonitorEventDispatcher.class);
		}
		// Although it is set that the ClassWritter will compute frames and MAXs it is still needed to call visitMaxs.
		// Its parameter will not be taken into consideration
		return methodVisitor;
	}

	private boolean shouldEnableMonitoringForMethod(final String name, final String desc, final List<MethodInfo> methodsToMonitor) {
		for (final MethodInfo method : methodsToMonitor) {
			if (method.getName().equals(name) && method.getDescriptor().equals(desc)) {
				return true;
			}
		}

		return false;
	}

}
