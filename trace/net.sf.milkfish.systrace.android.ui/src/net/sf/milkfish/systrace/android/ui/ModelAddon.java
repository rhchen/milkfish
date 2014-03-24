package net.sf.milkfish.systrace.android.ui;

import javax.annotation.PostConstruct;

import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.linuxtools.tmf.ui.project.model.TmfTraceType;
import org.eclipse.linuxtools.tmf.ui.project.model.TraceTypeHelper;

public class ModelAddon {

	@PostConstruct
	public void init(IEclipseContext context, IExtensionRegistry reg) {
		
		/*
		 * A hack to TMF, since inject must be before TMFTrace implementation
		 * 
		 * The getInstance() call would trigger load extension points
		 * 
		 * It is the point to force inject here
		 * 
		 */
		TraceTypeHelper tth = TmfTraceType.getInstance().getTraceType(Activator.TRACETYPE_ID);
		
		ContextInjectionFactory.inject(tth.getTrace(), context);
		
		System.out.println("ModelAddon : init");
	}
}
