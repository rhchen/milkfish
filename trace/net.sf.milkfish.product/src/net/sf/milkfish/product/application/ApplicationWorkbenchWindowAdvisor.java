package net.sf.milkfish.product.application;

import net.sf.milkfish.product.cli.CliException;
import net.sf.milkfish.product.cli.CliParser;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.linuxtools.tmf.core.TmfCommonConstants;
import org.eclipse.linuxtools.tmf.ui.project.model.TmfNavigatorContentProvider;
import org.eclipse.linuxtools.tmf.ui.project.model.TmfOpenTraceHelper;
import org.eclipse.linuxtools.tmf.ui.project.model.TmfProjectRegistry;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

public class ApplicationWorkbenchWindowAdvisor extends WorkbenchWindowAdvisor {

	private static CliParser fCli;
    
    public ApplicationWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
        super(configurer);
    }

    public ActionBarAdvisor createActionBarAdvisor(IActionBarConfigurer configurer) {
        return new ApplicationActionBarAdvisor(configurer);
    }
    
    @Override
    public void preWindowOpen() {
    	
        IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
        configurer.setInitialSize(new Point(400, 300));
        configurer.setShowCoolBar(true);
        configurer.setShowStatusLine(true);
        configurer.setTitle("Hello RCP"); //$NON-NLS-1$
        
        System.out.println("ApplicationWorkbenchWindowAdvisor : preWindowOpen");
    }
    
    @Override
    public void postWindowCreate() {
    	
    	PlatformUI.getWorkbench().getActiveWorkbenchWindow().addPerspectiveListener(new PerspectiveListener());
        createDefaultProject();
        openTraceIfNecessary();
        
    	System.out.println("ApplicationWorkbenchWindowAdvisor : postWindowCreate");
    }
    
    private static void openTraceIfNecessary() {
    	
    	 String args[] = Platform.getCommandLineArgs();
    	 
         try {
        	 
             fCli = new CliParser(args);
             
         } catch (CliException e) {
             e.printStackTrace();
         }
         
        String traceToOpen = fCli.getArgument(CliParser.OPEN_FILE_LOCATION);
        if (traceToOpen != null) {
            final IWorkspace workspace = ResourcesPlugin.getWorkspace();
            final IWorkspaceRoot root = workspace.getRoot();
            IProject project = root.getProject(TmfCommonConstants.DEFAULT_TRACE_PROJECT_NAME);
            final TmfNavigatorContentProvider ncp = new TmfNavigatorContentProvider();
            ncp.getChildren( project ); // force the model to be populated
            TmfOpenTraceHelper oth = new TmfOpenTraceHelper();
            try {
                oth.openTraceFromPath(TmfCommonConstants.DEFAULT_TRACE_PROJECT_NAME,traceToOpen, PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
            } catch (CoreException e) {
                e.printStackTrace();
            }

        }
    }
    
    private static void createDefaultProject() {
        TmfProjectRegistry.createProject(TmfCommonConstants.DEFAULT_TRACE_PROJECT_NAME, null, new NullProgressMonitor());
    }

    /**
     * A perspective listener implementation
     *
     * @author Bernd Hufmann
     */
    public class PerspectiveListener implements IPerspectiveListener {

        /**
         * Default Constructor
         */
        public PerspectiveListener() {
        }

        @Override
        public void perspectiveActivated(IWorkbenchPage page, IPerspectiveDescriptor perspective) {
            createDefaultProject();
        }

        @Override
        public void perspectiveChanged(IWorkbenchPage page, IPerspectiveDescriptor perspective, String changeId) {
        }
    }
}
