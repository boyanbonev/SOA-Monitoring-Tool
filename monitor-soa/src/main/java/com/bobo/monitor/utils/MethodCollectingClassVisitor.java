/**
 *
 * @author bobo
 *
 */
package com.bobo.monitor.utils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.bobo.data.ClassInfo;
import com.bobo.data.MethodInfo;

/**
 * @author bobo
 * 
 */
class MethodCollectingClassVisitor implements ClassVisitor, Opcodes {

	private final List<MethodInfo> classMethods;

	private final boolean skipSettersAndGetters;
	
	private final ClassInfo classInfo;

	private static final Set<String> methodsToSkip = new LinkedHashSet<String>();
	static {
		final Method[] methods = Object.class.getMethods();
		for (final Method method : methods) {
			methodsToSkip.add(method.getName());
		}
		methodsToSkip.add("init");
		methodsToSkip.add("<init>");
		methodsToSkip.add("clinit");
		methodsToSkip.add("<clinit>");
	}

	public MethodCollectingClassVisitor(final ClassInfo classInfo, final boolean skipSettersAndGetters) {
		this.classMethods = classInfo.getMethods();
		this.skipSettersAndGetters = skipSettersAndGetters;
		this.classInfo = classInfo;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
	}

	@Override
	public void visitSource(String source, String debug) {

	}

	@Override
	public void visitOuterClass(String owner, String name, String desc) {
	}

	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		return null;
	}

	@Override
	public void visitAttribute(Attribute attr) {
	}

	@Override
	public void visitInnerClass(String name, String outerName, String innerName, int access) {
	}

	@Override
	public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
		return null;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		boolean isAccessDefaultOrPrivate = !Modifier.isPublic(access) && !Modifier.isProtected(access);
		
		// skip methods coming from object and getters and setters if it is said to
		if (methodsToSkip.contains(name) || isAccessDefaultOrPrivate || skipSettersAndGetters
				&& (name.startsWith("set") || name.startsWith("get"))) {
			return null;
		}
		classMethods.add(new MethodInfo(classInfo, name, desc));
		return null;
	}

	@Override
	public void visitEnd() {
	}

}
