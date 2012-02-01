package com.bobo.monitoring.tool;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import com.bobo.data.ClassInfo;
import com.bobo.data.MethodInfo;
import com.bobo.filescanner.ArchiveScanner;
import com.bobo.filescanner.FileScanner;

public class View extends ViewPart {
	public View() {
	}

	public static final String ID = "RcpTest2.view";

	private FileScanner archiveScanner;

	private CheckboxTreeViewer viewer;
	private Text fileNameEditor;
	private Button continueBtn;
	private Text outFileLocation;

	/**
	 * The content provider class is responsible for providing objects to the view. It can wrap existing objects in
	 * adapters or simply return objects as-is. These objects may be sensitive to the current input of the view, or
	 * ignore it and always show the same content (like Task List, for example).
	 */
	class ViewContentProvider implements ITreeContentProvider {
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		}

		public void dispose() {
		}

		@SuppressWarnings("unchecked")
		public Object[] getElements(Object parent) {
			List<ClassInfo> elements;
			if (parent instanceof List) {
				elements = (List<ClassInfo>) parent;
				return elements.toArray(new ClassInfo[elements.size()]);
			}
			return new Object[0];
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof ClassInfo) {
				final ClassInfo clInfo = (ClassInfo) parentElement;
				final List<MethodInfo> methods = clInfo.getMethods();
				return methods.toArray(new MethodInfo[methods.size()]);
			}

			return null;
		}

		@Override
		public Object getParent(Object element) {
			if (element instanceof MethodInfo) {
				return ((MethodInfo) element).getContainingClass();
			}

			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			if (element instanceof ClassInfo) {
				final ClassInfo clInfo = (ClassInfo) element;
				final List<MethodInfo> methods = clInfo.getMethods();
				return methods.size() > 0;
			}
			return false;
		}
	}

	class ViewLabelProvider extends LabelProvider implements ILabelProvider {
		private final Image CLASS_IMAGE = Activator.getImageDescriptor("/icons/class.gif").createImage();
		private final Image METHOD_IMAGE = Activator.getImageDescriptor("/icons/method.gif").createImage();

		@Override
		public String getText(Object element) {
			if (element instanceof ClassInfo) {
				final ClassInfo clInfo = (ClassInfo) element;
				return clInfo.getName().replace("/", ".");
			} else if (element instanceof MethodInfo) {
				final MethodInfo mi = (MethodInfo) element;
				return mi.getName() + mi.getDescriptor().replace("/", ".");
			}
			return "label for element not found: " + element;
		}

		public Image getColumnImage(Object obj, int index) {
			return getImage(obj);
		}

		public Image getImage(Object element) {
			if (element instanceof ClassInfo) {
				return CLASS_IMAGE;
			} else if (element instanceof MethodInfo) {
				return METHOD_IMAGE;
			}
			return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_ELEMENT);
		}
	}

	/**
	 * This is a callback that will allow us to create the viewer and initialize it.
	 */
	public void createPartControl(final Composite parent) {
		final SashForm sashForm = new SashForm(parent, SWT.VERTICAL);
		sashForm.setLayout(new GridLayout(3, false));

		final Composite fileInputSection = new Composite(sashForm, SWT.NONE);
		fileInputSection.setLayout(new GridLayout(2, false));

		final Label label = new Label(fileInputSection, SWT.NONE);
		label.setText("Choose a web archive file:");
		GridData gridData = new GridData();
		gridData.horizontalSpan = 2;
		label.setLayoutData(gridData);

		fileNameEditor = new Text(fileInputSection, SWT.BORDER);
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		fileNameEditor.setLayoutData(gridData);
		fileNameEditor.setEditable(false);

		final Button browseForFileBtn = new Button(fileInputSection, SWT.PUSH);
		browseForFileBtn.setText("Browse");
		browseForFileBtn.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent event) {
				continueBtn.setEnabled(false);

				final Shell shell = parent.getShell();
				final FileDialog dialog = new FileDialog(shell, SWT.OPEN);
				dialog.setFilterExtensions(new String[] { "*.war", "*.*" });
				dialog.setFilterNames(new String[] { "WAR File", "All Files" });
				String filePath = dialog.open();
				if (filePath == null || !new File(filePath).exists()) {
					return;
				}
				if (outFileLocation != null) {
					outFileLocation.setText("");
				}

				fileNameEditor.setText(filePath);

				// Initialize FileScanner with the correct instance
				archiveScanner = new ArchiveScanner(filePath);

				@SuppressWarnings("unchecked")
				final List<ClassInfo>[] collectClassesMethods = new List[1];
				final ProgressMonitorDialog progresDialog = new ProgressMonitorDialog(parent.getShell());
				try {
					// fork is true, so the UI thread is not blocked
					progresDialog.run(true, false, new IRunnableWithProgress() {
						public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
							try {
								monitor.beginTask("Parsing WAR structure...", -1);
								collectClassesMethods[0] = archiveScanner.collectClassesMethods(true);
							} finally {
								monitor.done();
							}
						}
					});
				} catch (Exception e) {
					final String message = "A problem occured while trying to open the file and parse its structure: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
					final MessageDialog errDialog = new MessageDialog(parent.getShell(), "Problem occured", null, message, MessageDialog.ERROR,
							new String[] { "OK" }, 0);
					errDialog.open();
					continueBtn.setEnabled(false);
					archiveScanner = null;
				}

				viewer.setInput(collectClassesMethods[0]);
				continueBtn.setEnabled(true);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent event) {
				widgetSelected(event);
			}
		});

		final Label outLabel = new Label(fileInputSection, SWT.NONE);
		outLabel.setText("Choose a destination file (optional):");
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		outLabel.setLayoutData(gridData);

		outFileLocation = new Text(fileInputSection, SWT.BORDER);
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		outFileLocation.setLayoutData(gridData);
		outFileLocation.setEditable(false);

		final Button browseOutputFileBtn = new Button(fileInputSection, SWT.PUSH);
		browseOutputFileBtn.setText("Browse");
		browseOutputFileBtn.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				widgetSelected(arg0);
			}

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				archiveScanner.setDestinationZipFile(null);
				final Shell shell = parent.getShell();
				final FileDialog dialog = new FileDialog(shell, SWT.OPEN);
				dialog.setFilterExtensions(new String[] { "*.war", "*.*" });
				dialog.setFilterNames(new String[] { "WAR File", "All Files" });
				final String outPath = dialog.open();
				if (StringUtils.isNotBlank(outPath)) {
					archiveScanner.setDestinationZipFile(outPath);
				}
				outFileLocation.setText(outPath);
			}
		});

		viewer = new CheckboxTreeViewer(sashForm, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		viewer.setContentProvider(new ViewContentProvider());
		viewer.setLabelProvider(new ViewLabelProvider());
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		data.verticalAlignment = SWT.FILL;
		data.grabExcessVerticalSpace = true;
		viewer.getControl().setLayoutData(data);
		final CheckboxTreeViewer treeViewer = (CheckboxTreeViewer) viewer;
		treeViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				final Object element = event.getElement();
				final boolean parentChecked = event.getChecked();
				if (element instanceof MethodInfo) {
					final ClassInfo classInfo = ((MethodInfo) element).getContainingClass();
					final TreeItem[] parents = treeViewer.getTree().getItems();
					boolean childChecked = false;
					for (final TreeItem parentItem : parents) {
						// we can use simple instance comparison, because we are sure that this instance is contained
						// into the tree
						if (parentItem.getData() == classInfo) {
							final TreeItem[] children = parentItem.getItems();
							// search for at least one checked child
							for (final TreeItem childItem : children) {
								if (childItem.getChecked()) {
									childChecked = true;
									break;
								}
							}
						}
					}
					// the parent should remain selected if there's at least one child selected
					// and vice versa - it should be unchecked if there are no children selected
					treeViewer.setChecked(classInfo, childChecked | parentChecked);
					return;
				}
				// if the parent is selected the all its children should also be selected
				treeViewer.setSubtreeChecked(element, parentChecked);
			}
		});

		final Composite continueBtnSection = new Composite(sashForm, SWT.NONE);
		continueBtnSection.setLayout(new GridLayout(3, false));

		new Label(continueBtnSection, SWT.NONE).setText("(C)opyright Boyan G. Bonev");

		final Button btnClose = new Button(continueBtnSection, SWT.NONE);
		btnClose.setText("Close");
		btnClose.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, true));
		btnClose.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Runtime.getRuntime().exit(0);
			}
		});

		continueBtn = new Button(continueBtnSection, SWT.PUSH);
		continueBtn.setEnabled(false);
		continueBtn.setText("Finish");
		continueBtn.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, true));
		continueBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				final Object[] checkedElements = viewer.getCheckedElements();
				final Collection<ClassInfo> selectedClasses = filterSelectedElements(checkedElements);
				final ProgressMonitorDialog progresDialog = new ProgressMonitorDialog(parent.getShell());
				Exception exception = null;
				try {
					// fork is true, so the UI thread is not blocked
					progresDialog.run(true, false, new IRunnableWithProgress() {
						public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
							try {
								monitor.beginTask("Creating WAR file...", -1);
								archiveScanner.createMonitoringEnabledArchiveForClasses(selectedClasses);
							} finally {
								monitor.done();

							}
						}
					});
				} catch (Exception e1) {
					e1.printStackTrace();
					exception = e1;
				} finally {
					handleTaskFinish(parent, exception);
				}
			}

			/**
			 * @param checkedElements
			 * @param ci
			 * @return
			 */
			private Collection<ClassInfo> filterSelectedElements(final Object[] checkedElements) {
				final Set<MethodInfo> methods = new HashSet<MethodInfo>();
				for (final Object object : checkedElements) {
					if (object instanceof MethodInfo) {
						methods.add((MethodInfo) object);
					}
				}

				final Map<String, ClassInfo> classes = new HashMap<String, ClassInfo>();
				for (MethodInfo m : methods) {
					final String className = m.getContainingClass().getName();
					List<MethodInfo> classMethods = null;
					ClassInfo classInfo = null;
					if (classes.containsKey(className)) {
						classInfo = classes.get(className);
						classMethods = classInfo.getMethods();
					} else {
						// the class is still not added to the filtered classes,
						// so clone it and add it
						classInfo = new ClassInfo(m.getContainingClass());
						// if this is a new classinfo instance, then its methods
						// should be filtered - cleared and new ones should be added
						classMethods = new ArrayList<MethodInfo>();
						classes.put(className, classInfo);
					}

					// this class has some methods added (based on the filter) or it doesn't have any
					// anyway this is a selected method so add it
					classMethods.add(new MethodInfo(classInfo, m.getName(), m.getDescriptor()));
					// and overwrite the old class' methods (they will be present in the new methods)
					classInfo.setMethods(classMethods);
				}

				return classes.values();
			}

			/**
			 * @param parent
			 * @param exception
			 */
			private void handleTaskFinish(final Composite parent, Exception exception) {
				MessageDialog messageDialog = null;
				if (exception == null) {
					messageDialog = new MessageDialog(parent.getShell(), "Task finished", null,
							"Creating modified WAR file finished successfully!", MessageDialog.INFORMATION, new String[] { "OK" }, 0);
				} else {
					messageDialog = new MessageDialog(parent.getShell(), "Task finished", null,
							"Creating modified WAR file finished with an error: " + exception.getMessage(), MessageDialog.ERROR,
							new String[] { "OK" }, 0);
				}
				messageDialog.open();
			}
		});

		sashForm.setWeights(new int[] { 2, 6, 1 });
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		viewer.getControl().setFocus();
	}
}