package net.sf.milkfish.systrace.core.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.inject.Inject;
import javax.inject.Singleton;

import net.sf.milkfish.systrace.core.annotation.TraceEvent;
import net.sf.milkfish.systrace.core.annotation.TraceEventInput;
import net.sf.milkfish.systrace.core.annotation.TraceEventOutput;
import net.sf.milkfish.systrace.core.event.impl.SystraceEvent;
import net.sf.milkfish.systrace.core.event.impl.TmfEvent_1;
import net.sf.milkfish.systrace.core.event.impl.TmfEvent_2;
import net.sf.milkfish.systrace.core.pipe.impl.TracePipe;
import net.sf.milkfish.systrace.core.service.ISystraceService;
import net.sf.milkfish.systrace.core.state.SystraceStrings;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Creatable;
import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.event.ITmfEventField;
import org.eclipse.linuxtools.tmf.core.event.TmfEvent;
import org.eclipse.linuxtools.tmf.core.event.TmfEventField;
import org.eclipse.linuxtools.tmf.core.event.TmfEventType;
import org.eclipse.linuxtools.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.linuxtools.tmf.core.trace.ITmfContext;
import org.eclipse.linuxtools.tmf.core.trace.TmfLongLocation;

import com.fasterxml.jackson.core.JsonFactory;
import com.google.common.collect.Lists;

@Singleton
@Creatable
public class SystraceService implements ISystraceService{

	@Inject private IEclipseContext _context;
	
	private SystraceEvent currentEvent;
	
	private List<TracePipe> pipeList = Lists.<TracePipe>newArrayList();
	
	public int echo(){
		
		JsonFactory f = new JsonFactory();
		
		return 0;
		
	}
	
	public void registPipe(TracePipe pipe, SystraceEvent event){
		
		currentEvent= event;
		
		ContextInjectionFactory.invoke(pipe, TraceEventInput.class, _context);
		
		pipeList.add(pipe);
	}

	public SystraceEvent getCurrentEvent() {
		return currentEvent;
	}
	
	
	
}
 