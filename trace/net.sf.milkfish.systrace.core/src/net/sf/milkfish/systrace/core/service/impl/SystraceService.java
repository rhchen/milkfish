package net.sf.milkfish.systrace.core.service.impl;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;
import javax.inject.Singleton;

import net.sf.commonstringutil.StringUtil;
import net.sf.milkfish.systrace.core.annotation.TraceEventInput;
import net.sf.milkfish.systrace.core.cache.TraceCache;
import net.sf.milkfish.systrace.core.event.ISystraceEvent;
import net.sf.milkfish.systrace.core.event.impl.SystraceEvent;
import net.sf.milkfish.systrace.core.pipe.impl.TracePipe;
import net.sf.milkfish.systrace.core.service.ISystraceService;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Creatable;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.EventTopic;
import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;

import com.fasterxml.jackson.core.JsonFactory;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableMap;
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
	
	private static final ConcurrentMap<URI, TraceCache> cacheTables = Maps.<URI, TraceCache>newConcurrentMap();
	
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
	
	/* RH. Fix me.
	 * could avoid pass ITmfTrace? */
	public void addTrace(URI fileUri, ITmfTrace tmfTrace) throws IOException{
		
		/* RH. Fix me.
		 * Should be in a separate thread*/
		createPageTable(fileUri);
		
		createCacheTable(fileUri, tmfTrace);
		
		traceList.add(fileUri);
	}
	
	public TreeBasedTable<Integer, Long, Long> getPageTable(URI fileUri){
		
		return pageTables.get(fileUri);
		
	}
	
	public BiMap<Long, Integer> getRankTable(URI fileUri){
		
		return rankTables.get(fileUri);
	}
	
	private void createCacheTable(URI fileUri, ITmfTrace tmfTrace) throws FileNotFoundException{
		
		FileInputStream fis = new FileInputStream(fileUri.getPath());
		
		FileChannel fileChannel = fis.getChannel();
		
		TraceCache cache = new TraceCache();
		
		cache.init(fileChannel, pageTables.get(fileUri), rankTables.get(fileUri), tmfTrace);
		
		cacheTables.put(fileUri, cache);
		
	}
	
	public ITmfEvent getTmfEvent(URI fileUri, long rank) throws ExecutionException{
		
		BiMap<Long, Integer> bMap = rankTables.get(fileUri);
		
		TreeMap<Long, Integer> tMap = Maps.<Long, Integer>newTreeMap();
		
		tMap.putAll(bMap);
		
		long prevK = tMap.firstKey();
		
		if(rank >= prevK) {
			
			Iterator<Long> it = tMap.keySet().iterator();
			
			while(it.hasNext()){
				
				long k = it.next();
				
				if(rank <= k) break;
				
				prevK = k;
				
			}
			
		}
		
		int pageNumber = rankTables.get(fileUri).get(prevK);
		
		ImmutableMap<Long, ITmfEvent> data = cacheTables.get(fileUri).get(pageNumber);
		
		return data.get(rank);
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
		
		long rank          = 0;
		long positionStart = 0;
		long positionEnd   = 0;
		String lastLine    = "";
		
		TreeBasedTable<Integer, Long, Long> pageTable = TreeBasedTable.<Integer, Long, Long>create();
		BiMap<Long, Integer> rankTable = HashBiMap.<Long, Integer>create();
				
		for(int i=0; i<=pages; i++){
			
			long limit = (i+1) * M_BYTE > fileChannel.size() ? fileChannel.size() : (i+1) * M_BYTE;
			
			long bufferSize = limit - (i * M_BYTE);
			
			MappedByteBuffer mmb = fileChannel.map(FileChannel.MapMode.READ_ONLY, i * M_BYTE, bufferSize);

			byte[] buffer = new byte[(int) bufferSize];
			
			mmb.get(buffer);
			
			BufferedReader in = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(buffer)));
			
			/* Do a guess to skip the first 3 lines */
			positionStart = i == 0 ? skipLines(in, 3) : positionStart;
			
			for (String line = in.readLine(); line != null; line = in.readLine()) {
				
				lastLine = line;
				
				/* Rank increase on # not appears */
				rank = StringUtil.startsWithIgnoreCase(line, "#") == true ? rank : rank+1;
				
			}//for
			
			positionEnd = i == pages ? limit : limit - lastLine.getBytes().length;
			
			pageTable.put(i, positionStart, positionEnd);
			
			rank = i == pages ? rank : rank-1;
					
			rankTable.put(rank, i);
			
			positionStart = positionEnd;
		}
		
		pageTables.put(fileUri, pageTable);
		
		rankTables.put(fileUri, rankTable);
		
		fis.close();
	}
	
	/**
	 * 
	 * Skips number of lines to reduce dummy check
	 * <p>
	 * In Systrace header
	 * </p>
	 * <pre>
	  
		  var linuxPerfData = "\
		# tracer: nop\n\
		#\n\
		#           TASK-PID    CPU#    TIMESTAMP  FUNCTION\n\
		#              | |       |          |         |\n\
		
	 * </pre>
	 * <p>
	 * In Ftrace header
	 * </p>
	 * <pre>
		# tracer: nop
		#
		#           TASK-PID    CPU#    TIMESTAMP  FUNCTION
		#              | |       |          |         |
	 
	 * </pre>
	 * 
	 * @param in BufferedReader
	 * @param linesToSkip Numbers of line to skip
	 * @return The position start after skip
	 * @throws IOException
	 */
	private long skipLines(BufferedReader in, int linesToSkip) throws IOException{
		
		long positionStart = 0;
		
		for(int i=0; i<linesToSkip; i++){
			
			String line = in.readLine();
			
			/*
			 * RH. Fix me
			 * A hack of the readline. case the systrace input contain \n\ in every line
			 * increase the length to workaround it
			 * Ex. 
			 *  # tracer: nop\n\
				#\n\
				#           TASK-PID    CPU#    TIMESTAMP  FUNCTION\n\
				#              | |       |          |         |\n\
			 */
			positionStart += line.getBytes().length + 1;
		}
		
		return positionStart;
	}
}
 