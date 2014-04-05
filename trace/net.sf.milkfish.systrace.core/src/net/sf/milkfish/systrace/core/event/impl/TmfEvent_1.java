package net.sf.milkfish.systrace.core.event.impl;

import org.eclipse.e4.core.di.annotations.Creatable;
import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.event.TmfEvent;

@Creatable
public class TmfEvent_1 extends TmfEvent implements ITmfEvent {

	public TmfEvent_1(ITmfEvent event) {
		super(event);
	}

}
