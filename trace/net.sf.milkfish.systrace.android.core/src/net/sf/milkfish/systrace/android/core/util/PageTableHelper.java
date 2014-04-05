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

import com.google.common.collect.Maps;
import com.google.common.collect.TreeBasedTable;

public class PageTableHelper {

	private static ConcurrentMap<URI, TreeBasedTable<Integer, Long, Long>> pageTables = Maps.<URI, TreeBasedTable<Integer, Long, Long>>newConcurrentMap();
	
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
	
	public void createPageTable(File file) throws IOException{
		
		FileInputStream fis = new FileInputStream(file);
		
		FileChannel fileChannel = fis.getChannel();
		
		long size = fileChannel.size();
		
		int M_BYTE = 1024 * 1024;
		
		int pages = (int) (size / M_BYTE);
		
		long positionStart = 0;
		long positionEnd   = 0;
		String firstLine = null;
		String lastLine  = "";
		
		TreeBasedTable<Integer, Long, Long> table = TreeBasedTable.<Integer, Long, Long>create();
		
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
				
			}//for
			
			positionEnd = i == pages ? limit : limit - lastLine.getBytes().length;
			
			table.put(i, positionStart, positionEnd);
			
			positionStart = positionEnd;
		}
		
		pageTables.put(file.toURI(), table);
	}
}
