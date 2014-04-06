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
    
    /**
     * 
     * @param trace The trace implements ITmfTrace
     * @param rank The order of the event in trace, skip "#"
     * @param timestamp The ITmfTimestamp format timestamp object
     * @param source TBD
     * @param type TBD
     * @param content TBD
     * @param reference In Ftrace is the postfix of the record, 
     *  <pre>
     *  Ex prev_comm=swapper prev_pid=0 prev_prio=120 prev_state=R ==> next_comm=kworker/0:0 next_pid=13696 next_prio=120
     *  </pre>
     * @param sourceCPU In Ftrace is the CPU, 
     *  <pre>
     *  Ex [000]
     *  </pre>
     * @param eventName In Ftrace is the event type
     *  <pre>
     *  Ex. sched_switch
     *  </pre>
     */
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
