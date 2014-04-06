package net.sf.milkfish.systrace.core.cache;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

import net.sf.commonstringutil.StringUtil;
import net.sf.milkfish.systrace.core.event.impl.SystraceEvent;
import net.sf.milkfish.systrace.core.state.SystraceStrings;

import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.event.ITmfEventField;
import org.eclipse.linuxtools.tmf.core.event.TmfEventField;
import org.eclipse.linuxtools.tmf.core.event.TmfEventType;
import org.eclipse.linuxtools.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.core.trace.TmfLongLocation;

import com.google.common.cache.CacheLoader;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
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
	
	private ITmfTrace _tmfTrace;
	
	private long _currentRank;
	
	public TraceLoader(FileChannel fileChannel, TreeBasedTable<Integer, Long, Long> pageTable, BiMap<Long, Integer> rankTable, ITmfTrace tmfTrace) {
		
		super();
		this._fileChannel = fileChannel;
		this._pageTable   = pageTable;
		this._rankTable   = rankTable;
		this._tmfTrace    = tmfTrace;
	}
	
	@Override
	public ImmutableMap<Long, ITmfEvent> load(Integer pageNum) throws Exception {
		
		Builder<Long, ITmfEvent> builder = ImmutableMap.<Long, ITmfEvent>builder();
		
		ConcurrentMap<Long, ITmfEvent> dataMap = Maps.<Long, ITmfEvent>newConcurrentMap();
		
		/* Set the current rank map to the page number 
		 * This would be the position of the TMF event instance */
		this._currentRank = pageNum == 0 ? -1 : _rankTable.inverse().get(pageNum-1);
				
		SortedMap<Long, Long> map = _pageTable.row(pageNum);
		
		long positionStart = map.firstKey();
		long positionEnd   = map.get(positionStart);
		long bufferSize = positionEnd - positionStart;
		
		MappedByteBuffer mmb = _fileChannel.map(FileChannel.MapMode.READ_ONLY, positionStart, bufferSize);

		byte[] buffer = new byte[(int) bufferSize];
		
		mmb.get(buffer);
		
		BufferedReader in = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(buffer)));

		String line;
		
		/* Another way is to use regular expression, but seems slower
		 * 
		Pattern pt_sched_switch = Pattern.compile("(?i).*sched_switch.*", Pattern.CASE_INSENSITIVE);
		*/
		
		for (line = in.readLine(); line != null; line = in.readLine()) {
			
			//boolean isFind = pt_sched_switch.matcher(line).find();
			
			boolean isFind = StringUtil.startsWithIgnoreCase(line, "#");
			
			if(!isFind){
				
				this._currentRank ++;
				
				/* RH. Fix me
				 * Should have better way to do it */
				if(StringUtil.countText(line, "sched_switch") > 0){
					
					ITmfEvent event = handleSchedleSwitchEvent(line);
					
					dataMap.put(this._currentRank, event);
					
				}else if(StringUtil.countText(line, "sched_wakeup") > 0){
					
					ITmfEvent event = handleSchedleWakeupEvent(line);
					
					dataMap.put(this._currentRank, event);
					
				}else{
					
					ITmfEvent event = handleUndefinedEvent(line);
					
					dataMap.put(this._currentRank, event);
				}
				
			}//if
			
		}//for
		
		return builder.putAll(dataMap).build();
	}
	
	private ITmfEvent handleUndefinedEvent(String line){
		
		@SuppressWarnings("resource")
		Scanner scan = new Scanner(line);
		
		short cpuId    = 0;
		long timeStamp = 0L;
		String title   = "undefine";
		String suffStr = "undefine";
		
		while(scan.hasNext()){
			
			String sn = scan.next();
			
			/* Expect to read [00.] */
			if(StringUtil.countText(sn, "[0") > 0){
				
				/* Get CPU id, Ex. [000] */
				sn = StringUtil.remove(sn, "[");
				sn = StringUtil.remove(sn, "]");
				cpuId = Short.parseShort(sn);
				
				/* Get Timestamp, Ex. 50260.647833: */
				sn = scan.next();
				sn = StringUtil.remove(sn, ":");
				sn = StringUtil.remove(sn, ".");
				timeStamp =Long.parseLong(sn);
				
				/* Get Event Type Ex. sched_wakeup: */
				sn = scan.next();
				title = StringUtil.remove(sn, ":");
			
				/* Content of the event depends */
				suffStr = scan.next();
				
				break;
				
			}//if
			
			
		}//while

		TmfTimestamp ts = new TmfTimestamp(timeStamp,ITmfTimestamp.MICROSECOND_SCALE);
		Random rnd = new Random();
		long payload = Long.valueOf(rnd.nextInt(10));
		
		// put the value in a field
		final TmfEventField tmfEventField = new TmfEventField("value", payload, null); //$NON-NLS-1$
		
		// the field must be in an array
		final TmfEventField[] fields = new TmfEventField[1];
		fields[0] = tmfEventField;
		
		final TmfEventField content = new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null, fields);
		SystraceEvent event = new SystraceEvent(_tmfTrace, _currentRank, ts, null,new TmfEventType(title, title, null), content, suffStr, cpuId, title);
		
		return event;
	}
	
	private ITmfEvent handleSchedleWakeupEvent(String line){
		
		@SuppressWarnings("unchecked")
		List<String> list = StringUtil.splitAsList(line, "sched_wakeup: ");
		
		String prefStr = list.get(0);
		String suffStr = list.get(1);
		
		/* Suppose cores not more than 100 */
		String subPrefStr = (String) StringUtil.splitAsList(prefStr, "[0").get(1);
		
		@SuppressWarnings("resource")
		Scanner scan = new Scanner(subPrefStr);
		
		int count = 0;
		
		short cpuId = 0;
		long timeStamp = 0L;
		
		while(scan.hasNext()){
			
			String sn = scan.next();
			
			switch(count){
			
			case 0:
				sn = StringUtil.remove(sn, "]");
				cpuId = Short.parseShort(sn);
				break;
			case 1:
				sn = StringUtil.remove(sn, ":");
				sn = StringUtil.remove(sn, ".");
				timeStamp =Long.parseLong(sn);
				break;
			}
			
			count++;
			
		}//while

		suffStr = suffStr.trim();
		suffStr = StringUtil.replace(suffStr, "comm" , "||");
		suffStr = StringUtil.replace(suffStr, "pid"  , "||");
		suffStr = StringUtil.replace(suffStr, "prio" , "||");
		suffStr = StringUtil.replace(suffStr, "success", "||");
		suffStr = StringUtil.replace(suffStr, "target_cpu" , "||");
		
		@SuppressWarnings("unchecked")
		List<String> rlist = StringUtil.splitAsList(suffStr, "||=");
		
		final String title = "sched_wakeup";
		
		TmfTimestamp ts = new TmfTimestamp(timeStamp,ITmfTimestamp.MICROSECOND_SCALE);
		Random rnd = new Random();
		long payload = Long.valueOf(rnd.nextInt(10));
		
		/* Put the value in a field
		 * The field is required by SystraceStateProvider.eventHandle()*/
		final TmfEventField tmfEventField = new TmfEventField("value", payload, null); //$NON-NLS-1$
		final TmfEventField tmfEventField_TID = new TmfEventField(SystraceStrings.TID, Long.parseLong(rlist.get(2).trim()), null); //$NON-NLS-1$
		
		// the field must be in an array
		final TmfEventField[] fields = new TmfEventField[2];
		fields[0] = tmfEventField;
		fields[1] = tmfEventField_TID;
		
		final TmfEventField content = new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null, fields);
		SystraceEvent event = new SystraceEvent(_tmfTrace, _currentRank, ts, null,new TmfEventType(title, title, null), content, suffStr, cpuId, title);
		
		return event;
	}
	
	private ITmfEvent handleSchedleSwitchEvent(String line){
		
		@SuppressWarnings("unchecked")
		List<String> list = StringUtil.splitAsList(line, "sched_switch: ");
		
		String prefStr = list.get(0);
		String suffStr = list.get(1);
		
		/* Suppose cores not more than 100 */
		String subPrefStr = (String) StringUtil.splitAsList(prefStr, "[0").get(1);
		
		@SuppressWarnings("resource")
		Scanner scan = new Scanner(subPrefStr);
		
		int count = 0;
		
		short cpuId = 0;
		long timeStamp = 0L;
		
		while(scan.hasNext()){
			
			String sn = scan.next();
			
			switch(count){
			
			case 0:
				sn = StringUtil.remove(sn, "]");
				cpuId = Short.parseShort(sn);
				break;
			case 1:
				sn = StringUtil.remove(sn, ":");
				sn = StringUtil.remove(sn, ".");
				timeStamp =Long.parseLong(sn);
				break;
			}
			
			count++;
			
		}//while
		
		suffStr = suffStr.trim();
		suffStr = StringUtil.replace(suffStr, "==>", "");
		suffStr = StringUtil.replace(suffStr, "prev_comm" , "||");
		suffStr = StringUtil.replace(suffStr, "prev_pid"  , "||");
		suffStr = StringUtil.replace(suffStr, "prev_prio" , "||");
		suffStr = StringUtil.replace(suffStr, "prev_state", "||");
		suffStr = StringUtil.replace(suffStr, "next_comm" , "||");
		suffStr = StringUtil.replace(suffStr, "next_pid"  , "||");
		suffStr = StringUtil.replace(suffStr, "next_prio" , "||");
		
		@SuppressWarnings("unchecked")
		List<String> rlist = StringUtil.splitAsList(suffStr, "||=");
		
		String prevTask_Name_Id = rlist.get(1) +"-"+ rlist.get(2);
		String nextTask_Name_Id = rlist.get(5) +"-"+ rlist.get(6);
		
		
		final String title = "sched_switch";
		
		TmfTimestamp ts = new TmfTimestamp(timeStamp,ITmfTimestamp.MICROSECOND_SCALE);
		Random rnd = new Random();
		long payload = Long.valueOf(rnd.nextInt(10));

		/* Put the value in a field
		 * The field is required by SystraceStateProvider.eventHandle()*/
		final TmfEventField tmfEventField = new TmfEventField("value", payload, null); //$NON-NLS-1$
		final TmfEventField tmfEventField_PREV_TID = new TmfEventField(SystraceStrings.PREV_TID, Long.parseLong(rlist.get(2).trim()), null); //$NON-NLS-1$
		final TmfEventField tmfEventField_PREV_STATE = new TmfEventField(SystraceStrings.PREV_STATE, payload, null); //$NON-NLS-1$
		final TmfEventField tmfEventField_NEXT_COMM = new TmfEventField(SystraceStrings.NEXT_COMM, rlist.get(5).trim(), null); //$NON-NLS-1$
		final TmfEventField tmfEventField_NEXT_TID = new TmfEventField(SystraceStrings.NEXT_TID, Long.parseLong(rlist.get(6).trim()), null); //$NON-NLS-1$
		
		
		// the field must be in an array
		final TmfEventField[] fields = new TmfEventField[5];
		fields[0] = tmfEventField;
		fields[1] = tmfEventField_PREV_TID;
		fields[2] = tmfEventField_PREV_STATE;
		fields[3] = tmfEventField_NEXT_COMM;
		fields[4] = tmfEventField_NEXT_TID;
		
		final TmfEventField content = new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null, fields);

		SystraceEvent event = new SystraceEvent(_tmfTrace, _currentRank, ts, null,new TmfEventType(title, title, null), content, suffStr, cpuId, title);
		
		return event;
	}

}
