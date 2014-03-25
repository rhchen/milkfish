package net.sf.milkfish.systrace.android.ui;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.ui.project.model.TmfTraceType;
import org.eclipse.linuxtools.tmf.ui.project.model.TraceTypeHelper;

public class ModelAddon {

	private IEclipseContext _context;
	
	private ITmfTrace tmfTrace;
	
	@PostConstruct
	public void init(IEclipseContext context, IExtensionRegistry reg) {
		
		assert context != null;
		
		_context = context;
		
		/*
		 * A hack to TMF, since inject must be before TMFTrace implementation
		 * 
		 * The getInstance() call would trigger load extension points
		 * 
		 * It is the point to force inject here
		 * 
		 */
		TraceTypeHelper tth = TmfTraceType.getInstance().getTraceType(Activator.TRACETYPE_ID);
		
		tmfTrace = tth.getTrace();
		
		assert tmfTrace != null;
		
		ContextInjectionFactory.inject(tmfTrace, _context);
		
		System.out.println("ModelAddon : init");
	}
	
	@PreDestroy
	public void dispose(){
		
		ContextInjectionFactory.uninject(tmfTrace, _context);
		
	}
}
