package net.sf.milkfish.systrace.core;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;

import net.sf.milkfish.systrace.core.pipe.impl.TracePipe;

public class ModelAddon {

	private IEclipseContext _context;
	
	private TracePipe pipe;
	
	@PostConstruct
	public void init(IEclipseContext context, IExtensionRegistry reg) {
		
		assert context != null;
		
		_context = context;
		
//		pipe = ContextInjectionFactory.make(TracePipe.class, _context);
//		
//		ContextInjectionFactory.inject(pipe, _context);
		
		System.out.println("net.sf.milkfish.systrace.core.ModelAddon : init");
	}
	
	@PreDestroy
	public void dispose(){
		
		ContextInjectionFactory.uninject(pipe, _context);
		
	}
}
