package net.sf.milkfish.systrace.core.annotation;

import org.eclipse.e4.core.di.suppliers.ExtendedObjectSupplier;
import org.eclipse.e4.core.di.suppliers.IObjectDescriptor;
import org.eclipse.e4.core.di.suppliers.IRequestor;
import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.event.ITmfEventField;
import org.eclipse.linuxtools.tmf.core.event.TmfEvent;
import org.eclipse.linuxtools.tmf.core.event.TmfEventField;
import org.eclipse.linuxtools.tmf.core.event.TmfEventType;
import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimestamp;

public class TraceEventOutputSupplier extends ExtendedObjectSupplier {

	@Override
	public Object get(IObjectDescriptor descriptor, IRequestor requestor,
			boolean track, boolean group) {
		
    	/**
         * Object suppliers (primary & extended) are provide object describing the injection site 
         * 
         * IObjectDescriptor: provides access to the type and annotations of the field/parameter 
         * 
         * IRequestor: the requesting location: the method, field, constructor; can re-trigger
         */
    	
        
		return new Object();
	}

}
