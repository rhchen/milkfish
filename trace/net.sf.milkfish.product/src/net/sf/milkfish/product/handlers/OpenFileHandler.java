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

import javax.inject.Inject;

import net.sf.milkfish.product.messages.Messages;
import net.sf.milkfish.systrace.core.ISystraceService;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.linuxtools.tmf.core.TmfCommonConstants;
import org.eclipse.linuxtools.tmf.ui.project.model.TmfOpenTraceHelper;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

/**
 * Open file handler, used to open files (not directories)
 * 
 */
public class OpenFileHandler  {

	@Inject private ISystraceService systraceService;
	
	@Execute
	public void execute(Shell shell){
        FileDialog fd = new FileDialog(shell);
        fd.setText(Messages.OpenFileHandler_SelectTraceFile);
        String filePath = fd.open();
        if (filePath == null) {
            return ;
        }
        TmfOpenTraceHelper oth = new TmfOpenTraceHelper();
        try {
            oth.openTraceFromPath(TmfCommonConstants.DEFAULT_TRACE_PROJECT_NAME, filePath, shell);
        } catch (CoreException e) {
            e.printStackTrace();
        }
        
        int echo = systraceService.echo();
		
		System.out.println("AndroidTrace.validate "+ echo);
		
		
        return ;
    }
}
