/**
 *
 * @author bobo
 *
 */
package com.bobo.monitor.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.objectweb.asm.ClassReader;

import com.bobo.data.ClassInfo;
import com.bobo.data.MethodInfo;

/**
 * @author bobo
 * 
 */
public final class ReflectionUtils {

	public static final Logger logger = Logger.getLogger(ReflectionUtils.class);

	private ReflectionUtils() {
	}

	public static List<ClassInfo> collectClassesInfo(final List<String> classes, final boolean skipSettersAndGetters) {
		final List<ClassInfo> result = new ArrayList<ClassInfo>();
		if (classes == null) {
			return result;
		}

		// filter classes to be unique
		final Set<String> uniqueClasses = new LinkedHashSet<String>(classes);
		for (final String pathToClass : uniqueClasses) {
			final ClassInfo classInfo = createClassInfoInstance(pathToClass, skipSettersAndGetters);
			result.add(classInfo);
		}

		return result;
	}

	/**
	 * Collects methods for a given name, skipping constructors and methods coming from Object.class. It also sets the real classes'name.
	 *  
	 * @param classFilePath
	 * @param skipSettersAndGetters
	 * @param objectMethods
	 * @param result
	 */
	private static ClassInfo createClassInfoInstance(final String pathToClass, final boolean skipSettersAndGetters) {
		InputStream inp = null;
		final ClassInfo classInfo = new ClassInfo();
		try {
			inp = new FileInputStream(pathToClass);
			final ClassReader classReader = new ClassReader(inp);
			// collected classes will be added to result
			classInfo.setMethods(new ArrayList<MethodInfo>());
			// set the path to the class file
			classInfo.setPathToClassFile(pathToClass);
			//set the class's name from the class reader
			classInfo.setName(classReader.getClassName());
			// collect class' methods
			classReader.accept(new MethodCollectingClassVisitor(classInfo, skipSettersAndGetters), 0);
		} catch (FileNotFoundException e) {
			logger.error(e.getMessage(), e);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		} finally {
			if (inp != null) {
				try {
					inp.close();
				} catch (IOException e) {
					logger.warn(e.getMessage(), e);
				}
			}
		}

		return classInfo;
	}

}
