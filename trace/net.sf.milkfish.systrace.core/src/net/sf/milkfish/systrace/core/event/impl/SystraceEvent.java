package net.sf.milkfish.systrace.core.event.impl;

import net.sf.milkfish.systrace.core.event.ISystraceEvent;

import org.eclipse.e4.core.di.annotations.Creatable;
import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.event.ITmfEventField;
import org.eclipse.linuxtools.tmf.core.event.ITmfEventType;
import org.eclipse.linuxtools.tmf.core.event.TmfEvent;
import org.eclipse.linuxtools.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;

@Creatable
public class SystraceEvent extends TmfEvent implements ISystraceEvent, ITmfEvent{

	private final int sourceCPU;
	
	/* eventName is ftrace event type. ex sched_switch */
    private final String eventName;
    
	public SystraceEvent(ITmfTrace trace, long rank, ITmfTimestamp timestamp,
			String source, ITmfEventType type, ITmfEventField content,
			String reference, int sourceCPU, String eventName) {
		super(trace, rank, timestamp, source, type, content, reference);
		
		this.sourceCPU = sourceCPU;
        this.eventName = eventName;
	}

	public int getCPU() {
		return sourceCPU;
	}

	public String getEventName() {
		return eventName;
	}
    
	
    
}
