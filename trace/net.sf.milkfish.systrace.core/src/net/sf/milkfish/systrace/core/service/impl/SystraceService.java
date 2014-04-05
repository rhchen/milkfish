package net.sf.milkfish.systrace.core.service.impl;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import javax.inject.Inject;
import javax.inject.Singleton;

import net.sf.milkfish.systrace.core.annotation.TraceEventInput;
import net.sf.milkfish.systrace.core.event.ISystraceEvent;
import net.sf.milkfish.systrace.core.event.impl.SystraceEvent;
import net.sf.milkfish.systrace.core.pipe.impl.TracePipe;
import net.sf.milkfish.systrace.core.service.ISystraceService;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Creatable;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.EventTopic;

import com.fasterxml.jackson.core.JsonFactory;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.TreeBasedTable;

@Singleton
@Creatable
public class SystraceService implements ISystraceService{

	@Inject private IEclipseContext _context;
	
	private SystraceEvent currentEvent;
	
	private static final List<TracePipe> pipeList = Lists.<TracePipe>newArrayList();
	
	private static final List<URI> traceList = Lists.<URI>newArrayList();
	
	/*
	 * PageTables is used to store the begin / start position of paged file
	 * Default per MB size is a page.
	 * 
	 * URI. file URI to identify the input trace
	 * TreeBasedTable<page index, start position, end position>
	 */
	private static final ConcurrentMap<URI, TreeBasedTable<Integer, Long, Long>> pageTables = Maps.<URI, TreeBasedTable<Integer, Long, Long>>newConcurrentMap();
	
	/*
	 * RankTables is used to store which rank of data store in which page
	 * A rank represents the order the a record in the trace
	 * 
	 * URI. file URI to identify the input trace
	 * BiMap<rank start of data, page number>
	 */
	private static final ConcurrentMap<URI, BiMap<Long, Integer>> rankTables = Maps.<URI, BiMap<Long, Integer>>newConcurrentMap();
	
	public int echo(){
		
		JsonFactory f = new JsonFactory();
		
		return 0;
		
	}
	
	public void registPipe(TracePipe pipe){
		
		pipeList.add(pipe);
	}

	public SystraceEvent getCurrentEvent() {
		return currentEvent;
	}
	
	@Inject
	@Optional
	@SuppressWarnings("restriction")
	private void getNotified(@EventTopic(ISystraceEvent.TOPIC_EVENT_NEW) SystraceEvent event) {
		
		/* To avoid context set exception, data must not be interface */
		_context.set(ISystraceEvent.CONTEXT_KEY, event);
		
		for(TracePipe pipe : pipeList){
			
			ContextInjectionFactory.invoke(pipe, TraceEventInput.class, _context);
			
		}
	
	} 
	
	public void addTrace(URI fileURI) throws IOException{
		
		/* RH. Fix me.
		 * Should be in a separate thread
		 */
		createPageTable(fileURI);
		
		traceList.add(fileURI);
	}
	
	/**
	 * 
	 * Create page table to avoid load all trace data into memory
	 * 
	 * @param fileUri The URI of the trace file
	 * @throws IOException
	 */
	private void createPageTable(URI fileUri) throws IOException{
		
		FileInputStream fis = new FileInputStream(fileUri.getPath());
		
		FileChannel fileChannel = fis.getChannel();
		
		long size = fileChannel.size();
		
		int M_BYTE = 1024 * 1024;
		
		int pages = (int) (size / M_BYTE);
		
		long rank = 0;
		long positionStart = 0;
		long positionEnd   = 0;
		String firstLine = null;
		String lastLine  = "";
		
		TreeBasedTable<Integer, Long, Long> pageTable = TreeBasedTable.<Integer, Long, Long>create();
		BiMap<Long, Integer> rankTable = HashBiMap.<Long, Integer>create();
				
		for(int i=0; i<=pages; i++){
			
			long limit = (i+1) * M_BYTE > fileChannel.size() ? fileChannel.size() : (i+1) * M_BYTE;
			
			long bufferSize = limit - (i * M_BYTE);
			
			MappedByteBuffer mmb = fileChannel.map(FileChannel.MapMode.READ_ONLY, i * M_BYTE, bufferSize);

			byte[] buffer = new byte[(int) bufferSize];
			
			mmb.get(buffer);
			
			BufferedReader in = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(buffer)));
			
			for (String line = in.readLine(); line != null; line = in.readLine()) {
				
				firstLine = firstLine == null ? line : firstLine;
				lastLine = line;
				rank++;
				
			}//for
			
			positionEnd = i == pages ? limit : limit - lastLine.getBytes().length;
			
			pageTable.put(i, positionStart, positionEnd);
			
			rank = i == pages ? rank : rank-1;
					
			rankTable.put(rank, i);
			
			positionStart = positionEnd;
		}
		
		pageTables.put(fileUri, pageTable);
		
		rankTables.put(fileUri, rankTable);
	}
}
 