package com.bobo.monitor.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author bobo
 *
 */
public final class FileUtils {

    public static final Logger logger = LogManager.getLogger(FileUtils.class);

    public static final String JARFILE_EXTENSSION = ".jar";
    public static final String JAVA_CLASSFILE_EXTENSSION = ".class";

    public static final String WEB_INF = "WEB-INF";
    public static final String CLASSES = "classes";

    public FileUtils() {
    }

    private static int BUFFER_SIZE = 2048;

    /**
     * Deletes the whole directory tree with all its sub-folders and files.
     *
     * @param root
     *            the directory root to be deleted
     */
    public static void deleteDirectoryTree(final File root) {
        if (!root.exists()) {
            return;
        }

        if (root.isDirectory()) {
            final File[] files = root.listFiles();
            for (final File file : files) {
                if (file.isDirectory()) {
                    deleteDirectoryTree(file);
                } else {
                    file.delete();
                }
            }
        }

        // delete the directory itself
        root.delete();
    }

    public static void unzipArchiveToLocation(final File archivedFile, final File destFolder) throws IOException {
        // simple sanity check
        if (!archivedFile.exists()) {
            throw new IllegalArgumentException("The archive file '" + archivedFile + "' does not exist!");
        }

        if (!destFolder.exists()) {
            destFolder.mkdirs();
        }

        final ZipFile zipFile = new ZipFile(archivedFile);
        @SuppressWarnings("unchecked")
        final Enumeration<ZipEntry> entries = (Enumeration<ZipEntry>) zipFile.entries();
        while (entries.hasMoreElements()) {
            final ZipEntry zipEntry = entries.nextElement();
            final String zipEntryName = zipEntry.getName();
            final File destFile = new File(destFolder, zipEntryName);
            if (zipEntry.isDirectory()) {
                // the zip entry is a folder so just create it
                if (!destFile.exists()) {
                    destFile.mkdirs();
                }
            } else {
                // create the parent folders if needed
                File parentFile = destFile.getParentFile();
                if (!parentFile.exists()) {
                    parentFile.mkdirs();
                }

                // create a file in the destination folder
                if (!destFile.exists()) {
                    destFile.createNewFile();
                }

                InputStream inputStream = null;
                OutputStream outputStream = null;
                try {
                    inputStream = new BufferedInputStream(zipFile.getInputStream(zipEntry));
                    outputStream = new BufferedOutputStream(new FileOutputStream(destFile), BUFFER_SIZE);

                    copyToStream(inputStream, outputStream);
                } finally {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                    if (outputStream != null) {
                        outputStream.close();
                    }
                }

            }
        }
    }

    /**
     * Copies one stream to another without closing any of them.
     *
     * @param inputStream
     * @param outputStream
     * @throws IOException
     */
    public static void copyToStream(final InputStream inputStream, final OutputStream outputStream) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("The input stream is null!");
        }

        if (outputStream == null) {
            throw new IllegalArgumentException("The output stream is null!");
        }

        byte data[] = new byte[BUFFER_SIZE];
        // read and write until last byte is encountered
        int bytesRead = 0;
        while ((bytesRead = inputStream.read(data)) > 0) {
            outputStream.write(data, 0, bytesRead);
        }
        outputStream.flush();
    }

    /**
     * Deep copies one folder to another. The destination folder is first deleted.
     *
     * @param src
     * @param dest
     * @param deleteDestFolder
     */
    public static void copyDirectoryTree(final File src, final File dest, boolean deleteDestFolder) {
        if (src == null) {
            throw new IllegalArgumentException("The source folder parameter is null!");
        }

        if (dest == null) {
            throw new IllegalArgumentException("The detination folder parameter is null!");
        }

        if (!src.exists() || !src.isDirectory()) {
            throw new IllegalArgumentException("The source folder '" + src.getAbsolutePath() + "' doesn't exist");
        }

        if (deleteDestFolder) {
            if (dest.exists()) {
                deleteDirectoryTree(dest);
            }
            if (!dest.mkdirs()) {
                throw new IllegalStateException("Cannot create destination folder '" + dest.getAbsolutePath() + "'!");
            }
        }

        if (!dest.exists()) {
            if (!dest.mkdirs()) {
                throw new IllegalStateException("Cannot create destination folder '" + dest.getAbsolutePath() + "'!");
            }
        }

        final File[] files = src.listFiles();
        for (final File file : files) {
            final File newDest = new File(dest, file.getName());
            if (file.isDirectory()) {
                copyDirectoryTree(file, newDest, deleteDestFolder);
            } else {
                try {
                    copyFile(file, newDest);
                } catch (final IOException e) {
                    logger.error("Error while coping file '" + file.getAbsolutePath() + "' to location '" + dest.getAbsolutePath()
                            + "'. Cause: " + e.getMessage(), e);
                    // TODO: Does it make sense to abort the execution on error
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * Copies the content of one file to another. The second file is overridden.
     *
     * @param file
     * @param dest
     * @throws IOException
     */
    public static void copyFile(final File file, final File dest) throws IOException {
        if (!file.exists()) {
            throw new IllegalArgumentException("The source file '" + file.getAbsolutePath() + "' doesn't exist");
        }

        if (dest.exists()) {
            dest.delete();
        }
        if (!dest.createNewFile()) {
            throw new IllegalStateException("Cannot create destination file '" + dest.getAbsolutePath() + "'!");
        }

        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            inputStream = new BufferedInputStream(new FileInputStream(file));
            outputStream = new BufferedOutputStream(new FileOutputStream(dest), BUFFER_SIZE);
            copyToStream(inputStream, outputStream);
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
        }

    }

    /**
     * Archive using zip a whole folder with all of its content. The archive is stored into a single file. The folder's content is added to the archive
     * recursively.
     *
     * @param srcDir the source directory to be archived
     * @param archiveFileName the archive file's name. If there's such a file it will be deleted first.
     */
    public static void zipFolderToFile(final File srcDir, final String archiveFileName) {
        if (!srcDir.exists()) {
            throw new RuntimeException("The tmp sorce folder '" + srcDir.getAbsolutePath() + "' doesn't exist!");
        }
        if (!srcDir.isDirectory()) {
            throw new RuntimeException("The file '" + srcDir.getAbsolutePath() + "' have to be directory and it is not!");
        }
        final File archiveFile = new File(archiveFileName);
        if (archiveFile.exists()) {
            if (!archiveFile.delete()) {
                throw new RuntimeException("The file '" + archiveFile.getAbsolutePath() + "' cannot be deleted!");
            }
        }

        try {
            archiveFile.createNewFile();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }

        zipFolderToLocation(srcDir, archiveFile);
    }

    private static void zipFolderToLocation(final File srcFolder, final File destFile) {
        ZipOutputStream zipOutStream = null;
        try {
            zipOutStream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(destFile)));
            //  zip the folder after all the preparations
            addFolderToArchive(srcFolder, zipOutStream, srcFolder.getAbsolutePath() + File.separator);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        } finally {
            if (zipOutStream != null) {
                try {
                    zipOutStream.close();
                } catch (IOException e) {
                    logger.warn(e.getMessage(), e);
                }
            }
        }
    }

    private static void addFolderToArchive(final File folder, final ZipOutputStream zipStream, final String rootPath) throws IOException {
        final File[] files = folder.listFiles();
        for (final File file : files) {
            final String name = file.getAbsolutePath().substring(rootPath.length());
            if (file.isDirectory()) {
                // The '/' character must be added in the end of the name of the ZipEntry if it is folder
                zipStream.putNextEntry(new ZipEntry(name + "/"));
                addFolderToArchive(file, zipStream, rootPath);
            } else {
                zipStream.putNextEntry(new ZipEntry(name));
                InputStream in = null;
                try {
                    in = new BufferedInputStream(new FileInputStream(file));
                    copyToStream(in, zipStream);
                } finally {
                    if (in != null) {
                        in.close();
                    }
                }

            }
        }
    }

    /**
     * Finds all .class files on the given file path. The classes are searches recursively.
     *
     * @param root
     *            the start search folder.
     * @return list of file pointing to all the found .class files
     */
    public static List<File> findFilteredFilesOnPath(final File root, final String fileExtension) {
        if (root == null || !root.exists()) {
            return Collections.emptyList();
        }

        return findClassesOnPathInternal(root, new LinkedList<File>(), fileExtension);
    }

    /**
     * @param root
     * @param fileExtension
     * @return
     */
    private static List<File> findClassesOnPathInternal(final File root, final List<File> result, final String fileExtension) {
        final File[] files = root.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                findClassesOnPathInternal(file, result, fileExtension);
            } else if (file.getName().toLowerCase().endsWith(fileExtension)) {
                result.add(file);
            }
        }

        return result;
    }

    /**
     * @param file
     * @return
     */
    public static String getFileNameWithoutExtension(final File file) {
        String fileName = file.getName();
        if (fileName.contains(".")) {
            fileName = fileName.substring(0, fileName.lastIndexOf("."));
        }

        return fileName;
    }

}
