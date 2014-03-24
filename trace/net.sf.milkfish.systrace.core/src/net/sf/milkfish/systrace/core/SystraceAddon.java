package net.sf.milkfish.systrace.core;

import javax.annotation.PostConstruct;

import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.e4.core.contexts.IEclipseContext;

public class SystraceAddon {

	@PostConstruct
	public void init(IEclipseContext context, IExtensionRegistry reg) {
		
		
		  
		System.out.println("SystraceAddon : init");
	}
}
