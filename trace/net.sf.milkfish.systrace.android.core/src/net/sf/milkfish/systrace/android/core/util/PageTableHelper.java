package net.sf.milkfish.systrace.android.core.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import com.google.common.collect.TreeBasedTable;

public class PageTableHelper {

	/*
	 * PageTables is used to store the begin / start position of paged file
	 * Default per MB size is a page.
	 * 
	 * URI. file URI to identify the input trace
	 * TreeBasedTable<page index, start position, end position>
	 */
	private static ConcurrentMap<URI, TreeBasedTable<Integer, Long, Long>> pageTables = Maps.<URI, TreeBasedTable<Integer, Long, Long>>newConcurrentMap();
	
	/*
	 * RankTables is used to store which rank of data store in which page
	 * A rank represents the order the a record in the trace
	 * 
	 * URI. file URI to identify the input trace
	 * BiMap<rank start of data, page number>
	 */
	private static ConcurrentMap<URI, BiMap<Long, Integer>> rankTables = Maps.<URI, BiMap<Long, Integer>>newConcurrentMap();
	
	private static PageTableHelper fInstance = null;
	
	public static PageTableHelper getInstance() {
        
		if (fInstance == null) {
        	
            fInstance = new PageTableHelper();
        }
        
        return fInstance;
    }
	
	public PageTableHelper() {
		
		init();
	
	}

	private void init(){
	
		/* Do nothing */
	}
	
	public void createPageTable(URI fileUri) throws IOException{
		
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
