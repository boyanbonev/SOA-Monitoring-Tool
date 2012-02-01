/**
 *
 * @author bobo
 *
 */
package com.bobo.monitor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author bobo
 * 
 */
public class MonitorEventDispatcher {

	private static final MonitorEventDispatcher instance = new MonitorEventDispatcher();
	private final File file;

	private final ExecutorService executor;

	private MonitorEventDispatcher() {
		final String tmpDirPath = System.getProperty("java.io.tmpdir");
		final File folder = new File(tmpDirPath, "soa_monitoring");
		if (!folder.exists()) {
			folder.mkdirs();
		}

		file = new File(folder, "monitoring_event.log");
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		System.out.println("Statistical file location: " + file.getAbsolutePath());

		// consumer-producer with a lot of producers and only one consumer
		executor = Executors.newSingleThreadExecutor();
	}

	public static MonitorEventDispatcher getInstance() {
		return instance;
	}

	/**
	 * 
	 * @param methodName
	 * @param className
	 * @param instance
	 * @param startTime
	 * @param endTime
	 */
	public void dispatchMonitoringEvent(final String methodName, final String className, final Object instance, final long startTime,
			final long endTime) {
		// run the task asynchronously in order to return as fast as possible, and not to delay the execution of the
		// caller
		executor.submit(new Runnable() {
			@Override
			public void run() {
				final DateFormat format = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss SSS", Locale.ENGLISH);
				long executionTime = endTime - startTime;
				System.out.println(format.format(new Date()) + ": The method '" + className + "." + methodName + "' was called for "
						+ executionTime + " milli sec. Object instance = " + instance);

				appendToOutFile(className + ";" + methodName + ";" + startTime + ";" + endTime + ";" + instance);
			}
		});

	}

	private void appendToOutFile(final String line) {
		PrintWriter outStream = null;
		try {
			outStream = new PrintWriter(new FileOutputStream(file, true), false);
			outStream.println(line);
			outStream.flush();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			if (outStream != null) {
				outStream.close();
			}

		}
	}

}