package net.sf.milkfish.systrace.android.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.event.ITmfEventField;
import org.eclipse.linuxtools.tmf.core.event.TmfEvent;
import org.eclipse.linuxtools.tmf.core.event.TmfEventField;
import org.eclipse.linuxtools.tmf.core.event.TmfEventType;
import org.eclipse.linuxtools.tmf.core.exceptions.TmfTraceException;
import org.eclipse.linuxtools.tmf.core.request.ITmfDataRequest.ExecutionType;
import org.eclipse.linuxtools.tmf.core.request.TmfEventRequest;
import org.eclipse.linuxtools.tmf.core.signal.TmfSignalHandler;
import org.eclipse.linuxtools.tmf.core.signal.TmfSignalManager;
import org.eclipse.linuxtools.tmf.core.signal.TmfTraceSelectedSignal;
import org.eclipse.linuxtools.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.linuxtools.tmf.core.trace.ITmfContext;
import org.eclipse.linuxtools.tmf.core.trace.ITmfEventParser;
import org.eclipse.linuxtools.tmf.core.trace.ITmfLocation;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.core.trace.TmfContext;
import org.eclipse.linuxtools.tmf.core.trace.TmfLongLocation;
import org.eclipse.linuxtools.tmf.core.trace.TmfTrace;
import org.eclipse.linuxtools.tmf.core.trace.TmfTraceManager;

public class AndroidTrace extends TmfTrace implements ITmfEventParser {

	public static final String PLUGIN_ID = "net.sf.milkfish.systrace.android.core";

	private static final int CHUNK_SIZE = 65536; // seems fast on MY system
	private static final int EVENT_SIZE = 8; // according to spec

	private TmfLongLocation fCurrentLocation;
	private static final TmfLongLocation NULLLOCATION = new TmfLongLocation(
			(Long) null);
	private static final TmfContext NULLCONTEXT = new TmfContext(NULLLOCATION,
			-1L);

	private long fSize;
	private long fOffset;
	private File fFile;
	private String[] fEventTypes;
	private FileChannel fFileChannel;
	private MappedByteBuffer fMappedByteBuffer;

	@Override
	public IStatus validate(IProject project, String path) {

		File f = new File(path);
		if (!f.exists()) {
			return new Status(IStatus.ERROR, PLUGIN_ID, "File does not exist"); //$NON-NLS-1$
		}
		if (!f.isFile()) {
			return new Status(IStatus.ERROR, PLUGIN_ID, path + " is not a file"); //$NON-NLS-1$
		}

		return new Status(IStatus.ERROR, PLUGIN_ID,
				"File does not start as a CSV"); //$NON-NLS-1$
	}

	@Override
	public ITmfLocation getCurrentLocation() {
		return fCurrentLocation;
	}

	@Override
	public void initTrace(IResource resource, String path,
			Class<? extends ITmfEvent> type) throws TmfTraceException {

		super.initTrace(resource, path, type);

		fFile = new File(path);

		fSize = fFile.length();

		if (fSize == 0)
			throw new TmfTraceException("file is empty"); //$NON-NLS-1$

		fEventTypes = new String[] { "sched_switch", "irq" }; // 64 values of types according to //$NON-NLS-1$
		// the 'spec'
		if (getNbEvents() < 1) {
			throw new TmfTraceException("Trace does not have any events"); //$NON-NLS-1$
		}

		try {

			fFileChannel = new FileInputStream(fFile).getChannel();
			seek(0);

		} catch (FileNotFoundException e) {
			throw new TmfTraceException(e.getMessage());
		} catch (IOException e) {
			throw new TmfTraceException(e.getMessage());
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
		if (location == null) {
			nl = new TmfLongLocation(0L);
		}
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
		final long position = fOffset + (rank * EVENT_SIZE);
		int size = Math.min((int) (fFileChannel.size() - position), CHUNK_SIZE);
		fMappedByteBuffer = fFileChannel.map(MapMode.READ_ONLY, position, size);
	}

	@Override
	public ITmfEvent parseEvent(ITmfContext context) {

		if ((context == null) || (context.getRank() == -1)) {
			return null;
		}

		long pos = context.getRank();
		final String title = fEventTypes[0];
		long ts = System.currentTimeMillis();

		TmfTimestamp timestamp = new TmfTimestamp(ts,
				ITmfTimestamp.MICROSECOND_SCALE);

		Random rnd = new Random();

		int payload = rnd.nextInt(100);

		// put the value in a field
		final TmfEventField tmfEventField = new TmfEventField(
				"value", payload, null); //$NON-NLS-1$
		// the field must be in an array
		final TmfEventField[] fields = new TmfEventField[1];
		fields[0] = tmfEventField;
		final TmfEventField content = new TmfEventField(
				ITmfEventField.ROOT_FIELD_ID, null, fields);

		TmfEvent event = new TmfEvent(this, pos, timestamp, null,
				new TmfEventType(title, title, null), content, null);

		return event;
	}

}