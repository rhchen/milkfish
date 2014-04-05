package net.sf.milkfish.systrace.core.service.impl;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import net.sf.milkfish.systrace.core.annotation.TraceEventInput;
import net.sf.milkfish.systrace.core.event.ISystraceEvent;
import net.sf.milkfish.systrace.core.event.impl.SystraceEvent;
import net.sf.milkfish.systrace.core.pipe.impl.TracePipe;
import net.sf.milkfish.systrace.core.service.ISystraceService;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Creatable;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.EventTopic;

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
	
	public void registPipe(TracePipe pipe){
		
		pipeList.add(pipe);
	}

	public SystraceEvent getCurrentEvent() {
		return currentEvent;
	}
	
	@Inject
	@Optional
	@SuppressWarnings("restriction")
	private void getNotified(@EventTopic(ISystraceEvent.TOPIC_EVENT_NEW) SystraceEvent event) {
		
		/* To avoid context set exception, data must not be interface */
		_context.set(ISystraceEvent.CONTEXT_KEY, event);
		
		for(TracePipe pipe : pipeList){
			
			ContextInjectionFactory.invoke(pipe, TraceEventInput.class, _context);
			
		}
	
	} 
	
}
 