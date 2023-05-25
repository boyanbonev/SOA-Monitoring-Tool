package com.bobo.filescanner;

import static com.bobo.monitor.utils.FileUtils.CLASSES;
import static com.bobo.monitor.utils.FileUtils.WEB_INF;
import static com.bobo.monitor.utils.FileUtils.JAVA_CLASSFILE_EXTENSSION;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bobo.data.ClassInfo;
import com.bobo.monitor.utils.ClassModificationUtils;
import com.bobo.monitor.utils.FileUtils;
import com.bobo.monitor.utils.ReflectionUtils;

public class ArchiveScanner implements FileScanner {

    private static final Logger logger = LogManager.getLogger(ArchiveScanner.class);
    private final String TEMP_DIR_PATH;
    private File srcFolder;
    private File userClassesRoot;
    private final File archiveFile;
    private String destinationZipFile;

    private static final String DEPENDENT_CLASS_PACKAGE_NAME = "com/bobo/monitor/";
    private static final String[] DEPENDENT_RESOURCES_NAMES = new String[] { "MonitorEventDispatcher.class",
            "MonitorEventDispatcher$1.class" };

    public ArchiveScanner(final String archiveFilePath) {
        this(System.getProperty("java.io.tmpdir") + "soa_tmp", archiveFilePath);
    }

    public ArchiveScanner(final String tmpDirPath, final String archiveFilePath) {
        this(tmpDirPath, archiveFilePath, null);
    }

    public ArchiveScanner(String tmpDirPath, final String archiveFilePath, final String destinationZipFile) {
        if (tmpDirPath == null) {
            tmpDirPath = "soa_tmp";
        }
        TEMP_DIR_PATH = tmpDirPath;

        final File archiveFile = new File(archiveFilePath);
        if (!archiveFile.exists()) {
            throw new IllegalArgumentException("The file '" + archiveFilePath + "' doesn't exist!");
        }
        if (!archiveFile.isFile()) {
            throw new IllegalArgumentException("'" + archiveFilePath + "' is not a valid file!");
        }

        this.archiveFile = archiveFile;
        this.setDestinationZipFile(destinationZipFile);
    }

    @Override
    public List<ClassInfo> collectClassesMethods(final boolean skipSettersAndGetters) {
        final List<String> classes = unzipAndGetUserClasses(archiveFile);
        return ReflectionUtils.collectClassesInfo(classes, skipSettersAndGetters);
    }

    @Override
    public String createMonitoringEnabledArchiveForClasses(final Collection<ClassInfo> classInfos) {

        // recreate the target classes. they must be clean in order not to apply changes over
        // already changed class
        unzipFileToLocation(archiveFile, TEMP_DIR_PATH, true);

        // Adds monitoring info to the compiled class, meaning that this alters the compiled classes
        ClassModificationUtils.enableMonitoringForSelectedClasses(classInfos);

        String zipFilePath = null;
        if (destinationZipFile == null) {
            zipFilePath = archiveFile.getName();
            int ind = zipFilePath.indexOf(".");
            if (ind > 0) {
                zipFilePath = zipFilePath.substring(0, ind);
            }
            zipFilePath = archiveFile.getParent() + File.separator + zipFilePath + "_modified.war";
        } else {
            zipFilePath = destinationZipFile;
        }

        try {
            copyRequiredResources();

            // package everything back together
            FileUtils.zipFolderToFile(srcFolder, zipFilePath);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        } finally {
            FileUtils.deleteDirectoryTree(new File(TEMP_DIR_PATH));
        }

        return zipFilePath;
    }

    /**
     * @param zipFilePath
     * @throws FileNotFoundException
     * @throws IOException
     */
    private void copyRequiredResources() throws FileNotFoundException, IOException {
        final File destFolder = new File(getUserClassesRootFolder(), DEPENDENT_CLASS_PACKAGE_NAME);
        if (!destFolder.exists()) {
            destFolder.mkdirs();
        }
        for (final String className : DEPENDENT_RESOURCES_NAMES) {
            final InputStream inpStream = openResourceInputStream(className);
            if (inpStream == null) {
                throw new IllegalStateException("The required resource('" + className + "') cannot be found!");
            }
            OutputStream outputStream = null;
            try {
                // Open stream to destination file
                outputStream = new FileOutputStream(new File(destFolder, className));

                // Copy the dependent resource
                FileUtils.copyToStream(inpStream, outputStream);
            } finally {
                if (outputStream != null) {
                    outputStream.close();
                }
                if (inpStream != null) {
                    inpStream.close();
                }
            }

        }
    }

    /**
     * Opens a input stream to a system resource. In most cases this will be file, but there's a case where this
     * resource can be into an archive (like jar) and in this case there's no file reference and it should be used a
     * stream directly.
     *
     * @param resourceName
     *            the name of the resource that it is going to be open
     * @return an input stream to the resource or null if the resource cannot be found
     */
    private InputStream openResourceInputStream(final String resourceName) {
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(DEPENDENT_CLASS_PACKAGE_NAME + resourceName);
        if (inputStream == null) {
            inputStream = ClassLoader.getSystemClassLoader().getResourceAsStream(DEPENDENT_CLASS_PACKAGE_NAME + resourceName);
        }

        return inputStream;
    }

    private File unzipFileToLocation(final File archiveFile, final String tmpDirPath, final boolean overwriteDestFolder) {
        final File destFolder = new File(tmpDirPath);
        if (overwriteDestFolder && destFolder.exists()) {
            FileUtils.deleteDirectoryTree(destFolder);
        }

        // create the destination folder
        if (!destFolder.mkdirs()) {
            throw new RuntimeException("The destination folder '" + destFolder.getAbsolutePath() + "' cannot be created!");
        }

        try {
            // unzip the archive content into the destination folder
            FileUtils.unzipArchiveToLocation(archiveFile, destFolder);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            // there's a critical, unrecoverable error and the execution should
            // be terminated
            throw new RuntimeException(e);
        }

        final String fileName = FileUtils.getFileNameWithoutExtension(archiveFile);
        File unzippedFolder = new File(destFolder, fileName);
        if (!unzippedFolder.exists()) {
            unzippedFolder = destFolder;
        }
        return unzippedFolder;
    }

    private List<String> unzipAndGetUserClasses(final File archiveFile) {
        final File destFolder = unzipFileToLocation(archiveFile, TEMP_DIR_PATH, true);
        srcFolder = destFolder;
        final String fileName = archiveFile.getName().toLowerCase();

        File userClassesRootFolder = null;
        List<String> classes = null;
        if (fileName.endsWith(".war")) {
            userClassesRootFolder = new File(destFolder, WEB_INF + File.separator + CLASSES);
            userClassesRoot = userClassesRootFolder;
            List<File> classPaths = FileUtils.findFilteredFilesOnPath(userClassesRootFolder, JAVA_CLASSFILE_EXTENSSION);
            classes = new ArrayList<String>(classPaths.size());
            for (final File classFile : classPaths) {
                classes.add(classFile.getAbsolutePath());
            }
        } else if (fileName.endsWith(".ear")) {
            // TODO: Not implemented
            throw new RuntimeException("'*.EAR' support is not added yet!");
        }

        return classes;
    }

    private File getUserClassesRootFolder() {
        return userClassesRoot;
    }

    // public static void main(String[] args) {
    // long startTime = System.currentTimeMillis();
    // try {
    // // new ArchiveScanner("D:\\tmp\\ServletTestPrj.war").scanProject();
    // final FileScanner archiveScanner = new ArchiveScanner("D:\\tmp\\ServletTestPrj.war");
    // final List<ClassInfo> classes = archiveScanner.collectClassesMethods(true);
    // archiveScanner.createMonitoringEnabledArchiveForClasses(classes);
    // } finally {
    // System.out.println("Execution time = " + ((System.currentTimeMillis() - startTime) / 1000f) + "s");
    // }
    // }

    /**
     * @return the destinationZipFile
     */
    @Override
    public String getDestinationZipFile() {
        return destinationZipFile;
    }

    /**
     * @param destinationZipFile
     *            the destinationZipFile to set
     */
    @Override
    public void setDestinationZipFile(String destinationZipFile) {
        this.destinationZipFile = destinationZipFile;
    }

}
