/**********************************************************************
 * Copyright (c) 2013 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Matthew Khouzam - Initial API and implementation
 **********************************************************************/

package net.sf.milkfish.product.handlers;

import net.sf.milkfish.product.messages.Messages;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.linuxtools.tmf.core.TmfCommonConstants;
import org.eclipse.linuxtools.tmf.ui.project.model.TmfOpenTraceHelper;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Shell;

/**
 * Open a directory, not a file
 *
 * @author Matthew Khouzam
 */
public class OpenDirHandler {

	@Execute
	public void execute(Shell shell){
        // Open a directory
         DirectoryDialog dd = new DirectoryDialog(shell);
        dd.setText(Messages.OpenDirHandler_SelectTraceType);
        String dir = dd.open();
        if (dir == null) {
            return ;
        }
        TmfOpenTraceHelper oth = new TmfOpenTraceHelper();
        try {
            oth.openTraceFromPath(TmfCommonConstants.DEFAULT_TRACE_PROJECT_NAME, dir, shell);
        } catch (CoreException e) {
            e.printStackTrace();
        }
        return ;
    }
}
