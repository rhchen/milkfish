package net.sf.milkfish.systrace.android.ui;

import javax.inject.Inject;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;

import net.sf.milkfish.systrace.android.ui.views.controlflow.ControlFlowView;
import net.sf.milkfish.systrace.core.service.ISystraceService;

public class ControlFlowViewWrapper extends ControlFlowView {

	// ------------------------------------------------------------------------
    // Injections
    // ------------------------------------------------------------------------
    
    @Inject private ISystraceService systraceService;
    
    @Override
    public void init(IViewSite site) throws PartInitException {
       super.init(site);
  
       IEclipseContext context = (IEclipseContext) site.getService(IEclipseContext.class);
       ContextInjectionFactory.inject(this, context);
       
       int echo = systraceService.echo();
       
       System.out.println("ControlFlowView.init "+ echo);
    }
    
    
}
