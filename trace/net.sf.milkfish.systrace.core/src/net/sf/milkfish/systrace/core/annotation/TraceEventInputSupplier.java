package net.sf.milkfish.systrace.core.annotation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import javax.inject.Inject;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.InjectionException;
import org.eclipse.e4.core.di.suppliers.ExtendedObjectSupplier;
import org.eclipse.e4.core.di.suppliers.IObjectDescriptor;
import org.eclipse.e4.core.di.suppliers.IRequestor;
import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.event.ITmfEventField;
import org.eclipse.linuxtools.tmf.core.event.TmfEvent;
import org.eclipse.linuxtools.tmf.core.event.TmfEventField;
import org.eclipse.linuxtools.tmf.core.event.TmfEventType;
import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimestamp;

public class TraceEventInputSupplier extends ExtendedObjectSupplier {

	private final String fContext = "UnitTest";
    private final String fTypeId2 = "Some type2";

    private final String   fLabel0 = "label1";
    private final String   fLabel1 = "label2";
    private final String   fLabel2 = "label3";
    private final String[] fLabels = new String[] { fLabel0, fLabel1, fLabel2 };

    private final TmfTimestamp fTimestamp = new TmfTimestamp(12355, (byte) 2, 5);

    private final String fSource = "Source";

    private final TmfEventType fType = new TmfEventType(fContext, fTypeId2, TmfEventField.makeRoot(fLabels));

    private final String fReference = "Some reference";
    
    private static int count = 0;
    
    @Inject private IEclipseContext _context;
    
	@SuppressWarnings({ "unchecked", "restriction", "rawtypes" })
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
    	
		String str = requestor.getRequestingObjectClass().getCanonicalName();
    	
		Class clazz = (Class) descriptor.getDesiredType();
		
		//Object o = ContextInjectionFactory.make(clazz, _context);
		
		System.out.println("TraceEventInputSupplier.get "+ clazz.getName());
		
		TmfEventField fContent = new TmfEventField(ITmfEventField.ROOT_FIELD_ID, "Some other different content", null);
		count++;
		ITmfEvent fEvent = new TmfEvent(null, fTimestamp, fSource, fType, fContent, ""+ count);
        
		Object o;
		
		try {
			
			o = clazz.getConstructor(ITmfEvent.class).newInstance(fEvent);
			
			return o;
			
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
		
		return null;
	}

}
