package net.sf.milkfish.systrace.core.event;

import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;

public interface ISystraceEvent extends ITmfEvent{

	public static final String TOPIC_EVENT        = "TOPIC_EVENT";
	public static final String TOPIC_EVENT_ALL    = TOPIC_EVENT + "/*";
	public static final String TOPIC_EVENT_NEW    = TOPIC_EVENT + "/NEW";
	public static final String TOPIC_EVENT_UPDATE = TOPIC_EVENT + "/UPDATE";
	
	public static final String CONTEXT_KEY = ISystraceEvent.class.getName();
}
