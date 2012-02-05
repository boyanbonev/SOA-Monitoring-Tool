/**
 *
 * @author bobo
 *
 */
package com.bobo.filescanner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.bobo.data.ClassInfo;
import com.bobo.data.MethodInfo;
import com.bobo.monitor.utils.FileUtils;

/**
 * @author bobo
 * 
 */
public class ArchiveScannerIT {
	private ArchiveScanner scanner;
	private static String tmpDirPath;
	private static String archiveFilePath;
	private static String destinationZipFile;

	@BeforeClass
	public static void setupClass() {
		final ClassLoader cl = ArchiveScannerIT.class.getClassLoader();
		archiveFilePath = cl.getResource("sample.war").getFile();
		File file = new File(archiveFilePath);
		assertTrue(file.exists());

		final File tmpFolder = new File("target/tests");

		tmpFolder.mkdirs();
		tmpDirPath = tmpFolder.getAbsolutePath();
		destinationZipFile = tmpDirPath + "/sample_modified.war";
	}

	@Before
	public void setUp() {
		scanner = new ArchiveScanner(tmpDirPath, archiveFilePath, destinationZipFile);
	}

	@After
	public void clean() {
		final File tmpFolder = new File(tmpDirPath);
		if (tmpFolder.exists()) {
			FileUtils.deleteDirectoryTree(tmpFolder);
		}
	}

	@Test
	public void testCollectClassesMethods() {
		final List<ClassInfo> classes = scanner.collectClassesMethods(true);

		assertNotNull(classes);
		assertEquals(1, classes.size());

		final ClassInfo classInfo = classes.get(0);
		assertEquals("mypackage/Hello", classInfo.getName());
		assertEquals(1, classInfo.getMethods().size());

		final MethodInfo methodInfo = classInfo.getMethods().get(0);
		assertEquals("doGet", methodInfo.getName());
	}

	@Test
	public void testCreateMonitoringEnabledArchiveForClasses() throws IOException {
		final List<ClassInfo> classes = scanner.collectClassesMethods(true);
		assertNotNull(classes);
		assertEquals(1, classes.size());

		final String path = scanner.createMonitoringEnabledArchiveForClasses(classes);
		assertEquals(destinationZipFile, path);

		FileInputStream srcWarInStream = null;
		FileInputStream destWarInStream = null;
		try {
			srcWarInStream = new FileInputStream(archiveFilePath);
			long srcWarSize = srcWarInStream.getChannel().size();

			destWarInStream = new FileInputStream(path);
			long modifiedWarSize = destWarInStream.getChannel().size();

			assertTrue(
					"The source war file should be smaller than the destination one. This is because the destination war contains more classes.",
					srcWarSize < modifiedWarSize);
		} finally {
			if (srcWarInStream != null) {
				srcWarInStream.close();
			}
			if (destWarInStream != null) {
				destWarInStream.close();
			}
		}
	}
}
