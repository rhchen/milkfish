package net.sf.milkfish.systrace.core.service;

import javax.inject.Singleton;

import net.sf.milkfish.systrace.core.event.impl.SystraceEvent;
import net.sf.milkfish.systrace.core.pipe.impl.TracePipe;

import org.eclipse.e4.core.di.annotations.Creatable;

public interface ISystraceService {

	public int echo();
	
	public void registPipe(TracePipe pipe);
	
}
