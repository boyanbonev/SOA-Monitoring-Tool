/**
 *
 * @author bobo
 *
 */
package com.bobo.monitor.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import com.bobo.data.ClassInfo;

/**
 * Utility class used for class files manipulations.
 *
 * @author bobo
 *
 */
public final class ClassModificationUtils {

    public static final Logger logger = LogManager.getLogger(ClassModificationUtils.class);

    /**
     * Enables monitoring support for all the listed classes with the provided methods. It is important to mention that
     * this method makes a byte code modifications on the selected class files.
     *
     * @param classes
     *            the classes with the methods that will be monitored
     */
    public static void enableMonitoringForSelectedClasses(final Collection<ClassInfo> classes) {
        for (final ClassInfo classInfo : classes) {
            try {
                enableMonitoringForClassMethods(classInfo);
            } catch (FileNotFoundException e) {
                // this shouldn't happen, because if the class is successfully
                // loaded, then there must be a class file for it
                logger.error(e.getMessage(), e);
            } catch (IOException e) {
                // this should't happen
                logger.error(e.getMessage(), e);
            }
        }
    }

    /**
     * @param filePath
     * @throws FileNotFoundException
     * @throws IOException
     */
    private static void enableMonitoringForClassMethods(final ClassInfo classInfo) throws FileNotFoundException, IOException {
        final InputStream inp = new FileInputStream(classInfo.getPathToClassFile());
        byte[] newClassData = new byte[0];
        try {
            final ClassReader classReader = new ClassReader(inp);
            final ClassWriter classWritter = new ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES);
            final ClassAdapter filteringAdapter = new MethodFilteringClassAdapter(classWritter, classInfo);

            // start class processing
            classReader.accept(filteringAdapter, ClassReader.EXPAND_FRAMES);

            // get the new class' content in compiled state
            newClassData = classWritter.toByteArray();
        } finally {
            // close the input stream before starting to write back to its source
            inp.close();
        }

        if (newClassData.length > 0) {
            // just a precaution not to overwrite a working class with empty data in case of error
             saveFileContent(classInfo.getPathToClassFile(), newClassData);
        }
    }

    /**
     * @param classInfo
     * @param newClassData
     * @throws IOException
     * @throws FileNotFoundException
     */
    private static void saveFileContent(final String path, byte[] newClassData) throws IOException, FileNotFoundException {
        final File outFile = new File(path);
        outFile.delete();
        outFile.createNewFile();
        final FileOutputStream outFileStream = new FileOutputStream(outFile);
        outFileStream.write(newClassData);
        outFileStream.close();
    }

}
