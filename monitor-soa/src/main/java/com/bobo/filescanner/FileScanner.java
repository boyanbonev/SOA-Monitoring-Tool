/**
 *
 * @author bobo
 *
 */
package com.bobo.filescanner;

import java.util.Collection;
import java.util.List;

import com.bobo.data.ClassInfo;

/**
 * @author bobo
 * 
 */
public interface FileScanner {

	/**
	 * Scans a WAR file and finds all user defined classed within.
	 * 
	 * @param archiveFile
	 * @param skipSettersAndGetters
	 *            property telling whether the getters and setters should be skipped
	 * @return a list of classes infos and their methods
	 * @see com.bobo.data.ClassInfo
	 * @see com.bobo.data.MethodInfo
	 */
	public abstract List<ClassInfo> collectClassesMethods(final boolean skipSettersAndGetters);

	/**
	 * Generates a monitoring callbacks in the specified classes. Then archives everything back together to a previously
	 * specified destination.
	 * 
	 * @return path to the created archive file
	 * @param classInfos
	 */
	public abstract String createMonitoringEnabledArchiveForClasses(final Collection<ClassInfo> classInfos);

	/**
	 * @return the destinationZipFile
	 */
	public abstract String getDestinationZipFile();

	/**
	 * @param destinationZipFile
	 *            the destinationZipFile to set
	 */
	public abstract void setDestinationZipFile(String destinationZipFile);

}