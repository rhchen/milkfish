package net.sf.milkfish.product.handlers;

import net.sf.milkfish.systrace.core.ISystraceService;
import net.sf.milkfish.systrace.core.impl.SystraceService;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.InjectorFactory;
import org.eclipse.e4.ui.workbench.lifecycle.PostContextCreate;

/* Use for lifeCycleURI definition in product */
public class LifeCycleHandler {

	@PostContextCreate
    public void startup(final IEclipseContext context) {
		
            System.out.println("LifeCycleHandler.startup()");
    
            InjectorFactory.getDefault().addBinding(ISystraceService.class).implementedBy(SystraceService.class);
            
	}
}
