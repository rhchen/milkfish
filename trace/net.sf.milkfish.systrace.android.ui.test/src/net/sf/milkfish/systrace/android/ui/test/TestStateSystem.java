package net.sf.milkfish.systrace.android.ui.test;

import static org.junit.Assert.*;

import java.io.File;
import java.util.List;

import net.sf.milkfish.systrace.android.core.AndroidTrace;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.linuxtools.internal.tmf.ui.Activator;
import org.eclipse.linuxtools.internal.tmf.ui.project.model.TmfImportHelper;
import org.eclipse.linuxtools.internal.tmf.ui.project.model.TmfTraceImportException;
import org.eclipse.linuxtools.tmf.core.TmfCommonConstants;
import org.eclipse.linuxtools.tmf.core.exceptions.StateSystemDisposedException;
import org.eclipse.linuxtools.tmf.core.exceptions.TimeRangeException;
import org.eclipse.linuxtools.tmf.core.interval.ITmfStateInterval;
import org.eclipse.linuxtools.tmf.core.statesystem.ITmfStateSystem;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.ui.project.model.Messages;
import org.eclipse.linuxtools.tmf.ui.project.model.TmfNavigatorContentProvider;
import org.eclipse.linuxtools.tmf.ui.project.model.TmfOpenTraceHelper;
import org.eclipse.linuxtools.tmf.ui.project.model.TmfTraceFolder;
import org.eclipse.linuxtools.tmf.ui.project.model.TmfTraceType;
import org.eclipse.linuxtools.tmf.ui.project.model.TraceTypeHelper;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Start with plugin test
 * 1. Assign Product definition
 * 
 * Reference ProjectModelTestData to switch thread
 * 
 * The plugin not in MVN build process, due to the open trace operation requires run in main thread
 * If enabled <useUIThread>false</useUIThread> in surefire, it lead to SWTbot fail
 * As know inject now works in unit test, users has to manual inject class
 * For E4 service, it now work by manual injection
 * 
 * In junit plugin test, it works
 * 
 */
public class TestStateSystem {

	private static final File _file = new File("testdata/android_systrace.txt");
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		
		//Thread.sleep(3000);
	}

	@After
	public void tearDown() throws Exception {
		
		//Thread.sleep(10000);
	}

	@Test
	public final void test() throws CoreException, InterruptedException, TimeRangeException, StateSystemDisposedException {
		
		Assert.assertNotNull(new Object());
		
		final IWorkspace workspace = ResourcesPlugin.getWorkspace();
        final IWorkspaceRoot root = workspace.getRoot();
        IProject project = root.getProject(TmfCommonConstants.DEFAULT_TRACE_PROJECT_NAME);
        final TmfNavigatorContentProvider ncp = new TmfNavigatorContentProvider();
        ncp.getChildren( project ); // force the model to be populated
        
		//TmfOpenTraceHelper oth = new TmfOpenTraceHelper();
		
		//oth.openTraceFromPath(TmfCommonConstants.DEFAULT_TRACE_PROJECT_NAME, _file.getAbsolutePath(), Display.getDefault().getActiveShell());
        
        TraceTypeHelper tth = TmfTraceType.getInstance().getTraceType(net.sf.milkfish.systrace.android.ui.Activator.TRACETYPE_ID);
        
        try {
			openTraceFromPath(TmfCommonConstants.DEFAULT_TRACE_PROJECT_NAME, _file.getAbsolutePath(), "android_systrace.txt", tth);
		} catch (CoreException e) {
			
			e.printStackTrace();
		}
        
        delayThread(5000);
        
        ITmfTrace tmfTrace = tth.getTrace();
		
		assert tmfTrace != null;
		
		Assert.assertTrue(tmfTrace instanceof AndroidTrace);
		
		AndroidTrace aTrace = (AndroidTrace) tmfTrace;
		
		
		ITmfStateSystem ss = aTrace.getStateSystems().get(AndroidTrace.STATE_ID);
		
		assert ss != null;
		
		ss.waitUntilBuilt();
		
		int nb = ss.getNbAttributes();
		
		System.out.println("TestStateSystem.test "+ nb);
		
		long ts = ss.getStartTime();
		
		List<ITmfStateInterval> ll = ss.queryFullState(ts);
		
		System.out.println("TestStateSystem.test queryFullState "+ ll.size());
	}

	public IStatus openTraceFromPath(String projectRoot, String path, String traceName, TraceTypeHelper traceTypeToSet) throws CoreException {
        TmfTraceType tt = TmfTraceType.getInstance();
        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectRoot);
        IFolder folder = project.getFolder(TmfTraceFolder.TRACE_FOLDER_NAME);
        final IPath tracePath = folder.getFullPath().append(traceName);
        final IPath pathString = Path.fromOSString(path);
        IResource linkedTrace = TmfImportHelper.createLink(folder, pathString, traceName);
        if (linkedTrace != null && linkedTrace.exists()) {
            IStatus ret = TmfTraceType.setTraceType(tracePath, traceTypeToSet);
            if (ret.isOK()) {
                ret = TmfOpenTraceHelper.openTraceFromProject(projectRoot, traceName);
            }
            return ret;
        }
        return new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                Messages.TmfOpenTraceHelper_LinkFailed);
    }
	
	/**
     * Makes the main display thread sleep, so it gives a chance to other thread
     * needing the main display to execute
     *
     * @param waitTimeMillis
     *            time to wait in millisecond
     */
    public static void delayThread(final long waitTimeMillis) {
        final Display display = Display.getCurrent();
        if (display != null) {
            final long endTimeMillis = System.currentTimeMillis() + waitTimeMillis;
            while (System.currentTimeMillis() < endTimeMillis) {
                if (!display.readAndDispatch()) {
                    display.sleep();
                }
                display.update();
            }
        } else {
            try {
                Thread.sleep(waitTimeMillis);
            } catch (final InterruptedException e) {
                // Ignored
            }
        }
    }
}
