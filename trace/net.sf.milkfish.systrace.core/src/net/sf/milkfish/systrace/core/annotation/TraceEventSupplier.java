package net.sf.milkfish.systrace.core.annotation;

import javax.inject.Inject;

import net.sf.milkfish.systrace.core.service.ISystraceService;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.suppliers.ExtendedObjectSupplier;
import org.eclipse.e4.core.di.suppliers.IObjectDescriptor;
import org.eclipse.e4.core.di.suppliers.IRequestor;
import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.event.ITmfEventField;
import org.eclipse.linuxtools.tmf.core.event.TmfEvent;
import org.eclipse.linuxtools.tmf.core.event.TmfEventField;
import org.eclipse.linuxtools.tmf.core.event.TmfEventType;
import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimestamp;

public class TraceEventSupplier extends ExtendedObjectSupplier {

	private final String fContext = "UnitTest";
    private final String fTypeId2 = "Some type2";

    private final String   fLabel0 = "label1";
    private final String   fLabel1 = "label2";
    private final String   fLabel2 = "label3";
    private final String[] fLabels = new String[] { fLabel0, fLabel1, fLabel2 };

    private final TmfTimestamp fTimestamp3 = new TmfTimestamp(12355, (byte) 2, 5);

    private final String fSource = "Source";

    private final TmfEventType fType3 = new TmfEventType(fContext, fTypeId2, TmfEventField.makeRoot(fLabels));

    private final String fReference = "Some reference";
 
    @Inject private ISystraceService systraceService;
    
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
    	
    	ITmfEvent event = systraceService.getCurrentEvent();
    	
//    	String str = requestor.getRequestingObjectClass().getCanonicalName();
//    	
//		TmfEventField fContent3 = new TmfEventField(ITmfEventField.ROOT_FIELD_ID, "Some other different content", null);
//		ITmfEvent fEvent3 = new TmfEvent(null, fTimestamp3, fSource, fType3, fContent3, fReference);
        
		return event;
	}

}
