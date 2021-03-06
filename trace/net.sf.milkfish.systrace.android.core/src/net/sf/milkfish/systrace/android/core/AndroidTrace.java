package net.sf.milkfish.systrace.android.core;

import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import net.sf.milkfish.systrace.core.event.ISystraceEvent;
import net.sf.milkfish.systrace.core.event.impl.SystraceEvent;
import net.sf.milkfish.systrace.core.pipe.impl.TracePipe;
import net.sf.milkfish.systrace.core.service.ISystraceService;
import net.sf.milkfish.systrace.core.service.impl.SystraceService;
import net.sf.milkfish.systrace.core.state.SystraceStateProvider;
import net.sf.milkfish.systrace.core.state.SystraceStrings;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.event.ITmfEventField;
import org.eclipse.linuxtools.tmf.core.event.TmfEventField;
import org.eclipse.linuxtools.tmf.core.event.TmfEventType;
import org.eclipse.linuxtools.tmf.core.exceptions.TmfTraceException;
import org.eclipse.linuxtools.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.linuxtools.tmf.core.statesystem.ITmfStateSystem;
import org.eclipse.linuxtools.tmf.core.statesystem.TmfStateSystemFactory;
import org.eclipse.linuxtools.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.linuxtools.tmf.core.trace.ITmfContext;
import org.eclipse.linuxtools.tmf.core.trace.ITmfEventParser;
import org.eclipse.linuxtools.tmf.core.trace.ITmfLocation;
import org.eclipse.linuxtools.tmf.core.trace.TmfContext;
import org.eclipse.linuxtools.tmf.core.trace.TmfLongLocation;
import org.eclipse.linuxtools.tmf.core.trace.TmfTrace;
import org.eclipse.linuxtools.tmf.core.trace.TmfTraceManager;

public class AndroidTrace extends TmfTrace implements ITmfEventParser {

	/* The file name of the History Tree */
    public final static String HISTORY_TREE_FILE_NAME = "stateHistory.ht"; //$NON-NLS-1$

    /* ID of the state system we will build */
    public static final String STATE_ID = "net.sf.milkfish.systrace.android"; //$NON-NLS-1$
    
	public static final String PLUGIN_ID = "net.sf.milkfish.systrace.android.core";

	private TmfLongLocation fCurrentLocation;
	private static final TmfLongLocation NULLLOCATION = new TmfLongLocation((Long) null);
	private static final TmfContext NULLCONTEXT = new TmfContext(NULLLOCATION,-1L);

	private File fFile;
	
	/* Reference to class ModelAddon, there inject the require instance */
	@Inject private ISystraceService systraceService;
	
	@Inject private TracePipe pipe;
	
	@Inject private IEventBroker eventBroker;
	
	@Override
	public IStatus validate(IProject project, String path) {

		File f = new File(path);
		if (!f.exists()) {
			return new Status(IStatus.ERROR, PLUGIN_ID, "File does not exist"); //$NON-NLS-1$
		}
		if (!f.isFile()) {
			return new Status(IStatus.ERROR, PLUGIN_ID, path + " is not a file"); //$NON-NLS-1$
		}

		int echo = systraceService.echo();
		
		systraceService.registPipe(pipe);
		
		pipe.echo();
		
		System.out.println("AndroidTrace.validate "+ echo);
		
		return Status.OK_STATUS;
	}

	@Override
	public ITmfLocation getCurrentLocation() {
		return fCurrentLocation;
	}

	@Override
	public void initTrace(IResource resource, String path,
			Class<? extends ITmfEvent> type) throws TmfTraceException {

		super.initTrace(resource, path, type);

		try {
			
			fFile = new File(path);
			
			long fSize = SystraceService.getFileSize(fFile.toURI());
		
			if (fSize == 0) throw new TmfTraceException("file is empty"); //$NON-NLS-1$
			
			int nbEvents = 1 + (int)fSize/1024/1024;
			
			/* A guess to number of events */
			setNbEvents(nbEvents * 10000);
			
			if (getNbEvents() < 1) throw new TmfTraceException("Trace does not have any events"); //$NON-NLS-1$

			systraceService.addTrace(fFile.toURI());
			
			seek(0);
			
		} catch (IOException e) {
			
			e.printStackTrace();
		
		}
		
	}

	@Override
	public double getLocationRatio(ITmfLocation location) {
		return ((TmfLongLocation) location).getLocationInfo().doubleValue()
				/ getNbEvents();
	}

	@Override
	public ITmfContext seekEvent(ITmfLocation location) {
		
		TmfLongLocation nl = (TmfLongLocation) location;
		
		if (location == null) nl = new TmfLongLocation(0L);
		
		try {
			
			seek(nl.getLocationInfo());
		
		} catch (IOException e) {
			return NULLCONTEXT;
		}
		
		return new TmfContext(nl, nl.getLocationInfo());
	}

	@Override
	public ITmfContext seekEvent(double ratio) {
		
		long rank = (long) (ratio * getNbEvents());
		
		try {
			
			seek(rank);
		
		} catch (IOException e) {
			return NULLCONTEXT;
		}
		return new TmfContext(new TmfLongLocation(rank), rank);
	}

	private void seek(long rank) throws IOException {
		
		try {
			
			ITmfEvent event = systraceService.getTmfEvent(fFile.toURI(), rank);
		
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		
	}

	@Override
	public ITmfEvent parseEvent(ITmfContext context) {
		
		if ((context == null) || (context.getRank() == -1)) return null;

		long pos = context.getRank();
		
		/* Escape on count == NBevents */
		if(pos == getNbEvents()) return null;
		
		fCurrentLocation = new TmfLongLocation(pos);
		
		try {
			
			ITmfEvent event = systraceService.getTmfEvent(fFile.toURI(), pos);

			if(event == null) return null;
			
			event = ((ISystraceEvent) event).setTrace(this);
			
			/* To avoid context set exception, data must NOT be interface */
			eventBroker.post(ISystraceEvent.TOPIC_EVENT_NEW, event);
			
			return event;
			
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	/* State system is for flowcontrol view */
	@Override
    protected void buildStateSystem() throws TmfTraceException {
        super.buildStateSystem();

        /* Build the state system specific to LTTng kernel traces */
        String directory = TmfTraceManager.getSupplementaryFileDir(this);
        final File htFile = new File(directory + HISTORY_TREE_FILE_NAME);
        final ITmfStateProvider htInput = new SystraceStateProvider(this);

        ITmfStateSystem ss = TmfStateSystemFactory.newFullHistory(htFile, htInput, false);
        fStateSystems.put(STATE_ID, ss);
    }
	
	/* Dummy Codes, could be removed */
	private String[] fEventTypes = new String[] { "sched_switch", "irq" }; // 64 values of types according to //$NON-NLS-1$;
	
	public ITmfEvent parseEvent2(ITmfContext context) {

		if ((context == null) || (context.getRank() == -1)) return null;

		long pos = context.getRank();
		
		/* Escape on count == NBevents */
		if(pos == getNbEvents()) return null;
		
		final String title = fEventTypes[0];
		long ts = System.currentTimeMillis()*1000;

		TmfTimestamp timestamp = new TmfTimestamp(ts,ITmfTimestamp.MICROSECOND_SCALE);

		Random rnd = new Random();

		long payload = Long.valueOf(rnd.nextInt(10));

		// put the value in a field
		final TmfEventField tmfEventField = new TmfEventField("value", payload, null); //$NON-NLS-1$
		final TmfEventField tmfEventField_PREV_TID = new TmfEventField(SystraceStrings.PREV_TID, payload, null); //$NON-NLS-1$
		final TmfEventField tmfEventField_PREV_STATE = new TmfEventField(SystraceStrings.PREV_STATE, 0L, null); //$NON-NLS-1$
		final TmfEventField tmfEventField_NEXT_COMM = new TmfEventField(SystraceStrings.NEXT_COMM, payload+1+"", null); //$NON-NLS-1$
		final TmfEventField tmfEventField_NEXT_TID = new TmfEventField(SystraceStrings.NEXT_TID, payload+1, null); //$NON-NLS-1$
		
		
		// the field must be in an array
		final TmfEventField[] fields = new TmfEventField[5];
		fields[0] = tmfEventField;
		fields[1] = tmfEventField_PREV_TID;
		fields[2] = tmfEventField_PREV_STATE;
		fields[3] = tmfEventField_NEXT_COMM;
		fields[4] = tmfEventField_NEXT_TID;
		
		final TmfEventField content = new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null, fields);

		fCurrentLocation = new TmfLongLocation(pos);
		
		SystraceEvent event = new SystraceEvent(this, pos, timestamp, null,new TmfEventType(title, title, null), content, timestamp.toString(), rnd.nextInt(10), title);
		
		/* To avoid context set exception, data must not be interface */
		eventBroker.post(ISystraceEvent.TOPIC_EVENT_NEW, event);
		
		return event;
	}
}
