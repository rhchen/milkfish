package net.sf.milkfish.systrace.core.cache;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;

import com.google.common.cache.CacheLoader;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.TreeBasedTable;

public class TraceLoader extends CacheLoader<Integer, ImmutableMap<Long, ITmfEvent>>{

	private FileChannel _fileChannel;
	
	/*
	 * RankTables is used to store which rank of data store in which page
	 * A rank represents the order the a record in the trace
	 * 
	 * BiMap<rank start of data, page number>
	 */
	private BiMap<Long, Integer> _rankTable;
	
	/*
	 * PageTable is used to store the begin / start position of paged file
	 * Default per MB size is a page.
	 * 
	 * TreeBasedTable<page index, start position, end position>
	 */
	private TreeBasedTable<Integer, Long, Long> _pageTable;
	
	public TraceLoader(FileChannel fileChannel, TreeBasedTable<Integer, Long, Long> pageTable, BiMap<Long, Integer> rankTable) {
		
		super();
		this._fileChannel = fileChannel;
		this._pageTable   = pageTable;
		this._rankTable   = rankTable;
	}
	
	@Override
	public ImmutableMap<Long, ITmfEvent> load(Integer pageNum) throws Exception {
		
		SortedMap<Long, Long> map = _pageTable.row(pageNum);
		
		long positionStart = map.firstKey();
		long positionEnd   = map.get(positionStart);
		long bufferSize = positionEnd - positionStart;
		
		MappedByteBuffer mmb = _fileChannel.map(FileChannel.MapMode.READ_ONLY, 0,bufferSize);

		byte[] buffer = new byte[(int) bufferSize];
		
		mmb.get(buffer);
		
		BufferedReader in = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(buffer)));

		String line;
		
		for (line = in.readLine(); line != null; line = in.readLine()) {
			
		}
		
		return null;
	}

}
