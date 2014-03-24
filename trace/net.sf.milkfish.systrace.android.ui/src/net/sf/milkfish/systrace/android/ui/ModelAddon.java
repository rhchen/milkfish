package net.sf.milkfish.systrace.android.ui;

import javax.annotation.PostConstruct;

import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.e4.core.contexts.IEclipseContext;

public class ModelAddon {

	@PostConstruct
	public void init(IEclipseContext context, IExtensionRegistry reg) {
		
		
		  
		System.out.println("ModelAddon : init");
	}
}
