package net.sf.milkfish.product.application;

import java.io.File;
import java.net.URL;
import java.text.MessageFormat;

import net.sf.milkfish.product.messages.Messages;

import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

/**
 * This class controls all aspects of the application's execution
 */
public class Application implements IApplication {

	/**
     * The default workspace name
     */
    public static final String WORKSPACE_NAME = ".traceviewer"; //$NON-NLS-1$
    
	@Override
    public Object start(IApplicationContext context) throws Exception {
        Display display = PlatformUI.createDisplay();
        try {
            // fetch the Location that we will be modifying
            Location instanceLoc = Platform.getInstanceLocation();

            // -data @noDefault in <applName>.ini allows us to set the workspace here.
            // If the user wants to change the location then he has to change
            // @noDefault to a specific location or remove -data @noDefault for
            // default location
            if (!instanceLoc.allowsDefault() && !instanceLoc.isSet()) {
                File workspaceRoot = new File(getWorkspaceRoot());

                if (!workspaceRoot.exists()) {
                    MessageDialog.openError(display.getActiveShell(),
                            Messages.Application_WorkspaceCreationError,
                            MessageFormat.format(Messages.Application_WorkspaceRootNotExistError, new Object[] { getWorkspaceRoot() }));
                    return IApplication.EXIT_OK;
                }

                if (!workspaceRoot.canWrite()) {
                    MessageDialog.openError(display.getActiveShell(),
                            Messages.Application_WorkspaceCreationError,
                            MessageFormat.format(Messages.Application_WorkspaceRootPermissionError, new Object[] { getWorkspaceRoot() }));
                    return IApplication.EXIT_OK;
                }

                String workspace = getWorkspaceRoot() + File.separator + WORKSPACE_NAME;
                // set location to workspace
                instanceLoc.set(new URL("file", null, workspace), false); //$NON-NLS-1$
            }

            int returnCode = PlatformUI.createAndRunWorkbench(display, new ApplicationWorkbenchAdvisor());
            if (returnCode == PlatformUI.RETURN_RESTART) {
                return IApplication.EXIT_RESTART;
            }
            return IApplication.EXIT_OK;
        } finally {
            display.dispose();
        }
    }

    @Override
    public void stop() {
        if (!PlatformUI.isWorkbenchRunning()) {
            return;
        }
        final IWorkbench workbench = PlatformUI.getWorkbench();
        final Display display = workbench.getDisplay();
        display.syncExec(new Runnable() {
            @Override
            public void run() {
                if (!display.isDisposed()) {
                    workbench.close();
                }
            }
        });
    }
    
    /**
     * Gets the tracing workspace root directory
     *
     * @return the tracing workspace root directory
     */
    public static String getWorkspaceRoot() {
        return System.getProperty("user.home"); //$NON-NLS-1$
    }
}
