package net.sf.milkfish.systrace.core.pipe.impl;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.e4.core.di.annotations.Creatable;
import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;

import net.sf.milkfish.systrace.core.annotation.TraceEvent;
import net.sf.milkfish.systrace.core.annotation.TraceEventInput;
import net.sf.milkfish.systrace.core.event.impl.TmfEvent_1;
import net.sf.milkfish.systrace.core.event.impl.TmfEvent_2;
import net.sf.milkfish.systrace.core.pipe.ITracePipe;

@Singleton
@Creatable
public class TracePipe implements ITracePipe {

	private String str = "";
	
	public void echo(){
		
		System.out.println("TracePipe.echo "+ str);
	}
	
	/*
	 * RH : Fix Me
	 *
	@Inject
	public void Input_1(@TraceEventInput TmfEvent_1 event_1){
		
		str += " "+ event_1.getReference();
	}
	
	@Inject
	public void Input_2(@TraceEventInput TmfEvent_2 event_1){
		
		str += " "+ event_1.getReference();
	}
	
	
	
	@Inject
	public ITmfEvent Output(){
		
		return null;
	}
	*/
	
	@TraceEventInput
	public void callBack(@TraceEvent ITmfEvent event){
		
		System.out.println("TracePipe.callBack "+ event.getReference());
	}
}
