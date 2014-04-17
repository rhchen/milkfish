package net.sf.milkfish.systrace.core.service;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ExecutionException;

import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;

import com.google.common.collect.BiMap;
import com.google.common.collect.TreeBasedTable;

import net.sf.milkfish.systrace.core.pipe.impl.TracePipe;

public interface ISystraceService {

	public int echo();
	
	public void registPipe(TracePipe pipe);
	
	public void addTrace(URI fileURI) throws IOException;
	
	public TreeBasedTable<Integer, Long, Long> getPageTable(URI fileUri);
	
	public BiMap<Long, Integer> getRankTable(URI fileUri);
	
	public ITmfEvent getTmfEvent(URI fileUri, long rank) throws ExecutionException;
	
}
