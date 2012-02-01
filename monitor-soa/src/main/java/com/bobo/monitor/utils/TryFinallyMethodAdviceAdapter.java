/**
 *
 * @author bobo
 *
 */
package com.bobo.monitor.utils;

import java.lang.reflect.Modifier;
import java.util.Random;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

import com.bobo.monitor.MonitorEventDispatcher;

/**
 * @author bobo
 * 
 */
public class TryFinallyMethodAdviceAdapter extends AdviceAdapter {

	private static final long OFFSET = 1000000L;
	private static final String TIME_VAR_NAME = "startTimeVar_" + (OFFSET + new Random().nextInt());

	private final String methodName;
	private final String className;
	private final Class<? extends MonitorEventDispatcher> eventDispatcherClass;
	private final int access;

	private final Label startFinallyLbl;
	private final Label timeVarStartLbl;
	private final Label timeVarEndLbl;

	private int startTimeVarIndex;

	/**
	 * @param mv
	 * @param access
	 * @param name
	 * @param methodDescriptor
	 */
	protected TryFinallyMethodAdviceAdapter(final MethodVisitor mv, final int access, final String methodName,
			final String methodDescriptor, final String className, Class<MonitorEventDispatcher> eventDispatcherClass) {
		super(mv, access, methodName, methodDescriptor);

		this.access = access;
		this.methodName = methodName;
		this.className = className;
		this.eventDispatcherClass = eventDispatcherClass;

		startFinallyLbl = new Label();
		timeVarStartLbl = new Label();
		timeVarEndLbl = new Label();

		startTimeVarIndex = -1;
	}

	@Override
	public void visitCode() {
		super.visitCode();

		mv.visitLabel(startFinallyLbl);
	}

	@Override
	public void visitMaxs(int maxStack, int maxLocals) {
		visitLabel(timeVarEndLbl);

		final Label endFinallyLbl = new Label();
		mv.visitTryCatchBlock(startFinallyLbl, endFinallyLbl, endFinallyLbl, null);
		mv.visitLabel(endFinallyLbl);

		onFinally(ATHROW);
		mv.visitInsn(ATHROW);

		mv.visitMaxs(maxStack, maxLocals);
	}

	@Override
	protected void onMethodEnter() {
		visitLabel(timeVarStartLbl);

		startTimeVarIndex = newLocal(Type.LONG_TYPE);
		visitLocalVariable(TIME_VAR_NAME, "J", null, timeVarStartLbl, timeVarEndLbl, startTimeVarIndex);

		// store System.currentTimeMillis() in the newly created variable
		visitMethodInsn(INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J");
		visitVarInsn(LSTORE, startTimeVarIndex);
	}

	@Override
	protected void onMethodExit(int opcode) {
		if (opcode != ATHROW) {
			onFinally(opcode);
		}
	}

	private void onFinally(int opcode) {
		String getInstanceDescriptor = "";
		try {
			getInstanceDescriptor = Type.getMethodDescriptor(eventDispatcherClass.getMethod("getInstance"));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		final String eventDispatcherInternalName = Type.getInternalName(eventDispatcherClass);
		// By convention the event dispatching class must have no arguments "getInstance" method.
		visitMethodInsn(INVOKESTATIC, eventDispatcherInternalName, "getInstance", getInstanceDescriptor);

		// put the method's name on the stack
		visitLdcInsn(methodName);
		// put the class' name on the stack
		visitLdcInsn(className.replace("/", "."));
		// put the instance on the stack
		if (Modifier.isStatic(access)) {
			// put "null" if it is static method
			visitInsn(ACONST_NULL);
		} else {
			// or "this" otherwise
			visitIntInsn(ALOAD, 0);
		}

		String dispatchMonitoringEventDescriptor = "";
		try {
			dispatchMonitoringEventDescriptor = Type.getMethodDescriptor(eventDispatcherClass.getMethod("dispatchMonitoringEvent",
					String.class, String.class, Object.class, long.class, long.class));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		// put the start time variable on the stack so that it will be passed as a parameter
		visitVarInsn(LLOAD, startTimeVarIndex);
		// put System.currentTimeMillis() on the stack
		visitMethodInsn(INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J");

		visitMethodInsn(INVOKEVIRTUAL, eventDispatcherInternalName, "dispatchMonitoringEvent", dispatchMonitoringEventDescriptor);
	}

}
