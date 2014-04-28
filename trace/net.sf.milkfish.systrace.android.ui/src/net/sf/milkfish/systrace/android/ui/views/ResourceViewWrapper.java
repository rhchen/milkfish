package net.sf.milkfish.systrace.android.ui.views;

import javax.inject.Inject;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;

import net.sf.milkfish.systrace.android.ui.views.controlflow.ControlFlowView;
import net.sf.milkfish.systrace.android.ui.views.resources.ResourcesView;
import net.sf.milkfish.systrace.core.service.ISystraceService;

public class ResourceViewWrapper extends ResourcesView {

	
	private IEclipseContext _context;
	
	// ------------------------------------------------------------------------
    // Injections
    // ------------------------------------------------------------------------
    
    @Inject private ISystraceService systraceService;
    
    @Override
    public void init(IViewSite site) throws PartInitException {
       
  
       _context = (IEclipseContext) site.getService(IEclipseContext.class);
       
       assert _context != null;
       
       super.init(site);
       
       ContextInjectionFactory.inject(this, _context);
       
       int echo = systraceService.echo();
       
       System.out.println("ResourceViewWrapper.init "+ echo);
    }

	@Override
	public void dispose() {
		
		super.dispose();
		
		ContextInjectionFactory.uninject(this, _context);
		
	}
    
    
}
