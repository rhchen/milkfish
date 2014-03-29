package net.sf.milkfish.systrace.core.service.impl;

import javax.inject.Inject;
import javax.inject.Singleton;

import net.sf.milkfish.systrace.core.annotation.TraceEvent;
import net.sf.milkfish.systrace.core.service.ISystraceService;

import org.eclipse.e4.core.di.annotations.Creatable;
import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;

import com.fasterxml.jackson.core.JsonFactory;

public class SystraceService implements ISystraceService{

	public int echo(){
		
		JsonFactory f = new JsonFactory();
		
		return 0;
		
	}
	
	@Inject
	public void annotate(@TraceEvent ITmfEvent event){
		
	}
}
