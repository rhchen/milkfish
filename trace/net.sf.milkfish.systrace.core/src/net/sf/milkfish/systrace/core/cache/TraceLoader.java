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
import net.sf.milkfish.systrace.core.service.impl.SystraceService;
import net.sf.milkfish.systrace.core.state.SystraceStrings;

import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.event.ITmfEventField;
import org.eclipse.linuxtools.tmf.core.event.TmfEventField;
import org.eclipse.linuxtools.tmf.core.event.TmfEventType;
import org.eclipse.linuxtools.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimestamp;
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
	
	private long _currentRank;
	
	public TraceLoader(FileChannel fileChannel, TreeBasedTable<Integer, Long, Long> pageTable, BiMap<Long, Integer> rankTable) {
		
		super();
		this._fileChannel = fileChannel;
		this._pageTable   = pageTable;
		this._rankTable   = rankTable;
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
			
			boolean isFind = SystraceService.isLineMatch(line);
			
			if(isFind){
				
				this._currentRank ++;
				
				/* RH. Fix me
				 * Should have better way to do it */
				if(StringUtil.countText(line, "sched_switch") > 0){
					
					ITmfEvent event = handleSchedleSwitchEvent(line);
					
					dataMap.put(this._currentRank, event);
					
					continue;
					
				}else if(StringUtil.countText(line, "sched_wakeup") > 0){
					
					ITmfEvent event = handleSchedleWakeupEvent(line);
					
					dataMap.put(this._currentRank, event);
					
					continue;
					
				}else if(StringUtil.countText(line, "softirq_raise") > 0 ||
						 StringUtil.countText(line, "softirq_entry") > 0 ||
						 StringUtil.countText(line, "softirq_exit") > 0){
					
					ITmfEvent event = handleSoftIrqEvent(line);
					
					dataMap.put(this._currentRank, event);
					
					continue;
					
				}else if(StringUtil.countText(line, "irq_handler_entry") > 0 ||
				         StringUtil.countText(line, "irq_handler_exit") > 0){
					
					ITmfEvent event = handleIrqEvent(line);
					
					dataMap.put(this._currentRank, event);
					
					continue;
					
				}else if(StringUtil.countText(line, "trace_event_clock_sync") > 0){
					
					/* 
					 * The dummy event is the last event of the trace
					 * The type is trace_event_clock_sync, just ignore it, ex
					 * 
					 * 	dummy-0000  [000] 0.0: 0: trace_event_clock_sync: parent_ts=0.0\n";
					 * 
					 * When trace iterate to this, return null to lead to escape the trace parse
					 * Here we do nothing
					 */
					continue;
					
				}else{
					
					ITmfEvent event = handleUndefinedEvent(line);
					
					dataMap.put(this._currentRank, event);
					
					continue;
				}
				
			}else{
				
				System.out.println("line ignore "+ line);
			}
			
		}//for
		
		in.close();
		
		return builder.putAll(dataMap).build();
	}
	
	/* Inner class to store header of line */
	private final class Head{
	
		public short cpuId    = 0;
		public long timeStamp = 0L;
		public String title   = "undefine";
		public String suffStr = "undefine";
		
		public Head(final short cpuId, final long timeStamp, final String title, final String suffStr){
			
			this.cpuId     = cpuId;
			this.timeStamp = timeStamp;
			this.title     = title;
			this.suffStr   = suffStr;
		}
	}
	
	private final Head parseHead(String line){
		
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
				title = StringUtil.remove(sn, ":").trim().intern();
			
				/* Content of the event depends */
				@SuppressWarnings("unchecked")
				List<String> list = StringUtil.splitAsList(line, sn);
				
				suffStr = list.get(1).trim();
				
				break;
				
			}//if
			
			
		}//while
		
		return new Head(cpuId, timeStamp, title, suffStr);
	}
	
	private final ITmfEvent handleIrqEvent(String line){
		
		Head head = parseHead(line);

		String suffStr = head.suffStr;
		suffStr = StringUtil.replace(suffStr, "irq" , "||");
		suffStr = StringUtil.replace(suffStr, "name"  , "||");
		suffStr = StringUtil.replace(suffStr, "ret"  , "||");
		
		@SuppressWarnings("unchecked")
		List<String> rlist = StringUtil.splitAsList(suffStr, "||=");
		
		TmfTimestamp ts = new TmfTimestamp(head.timeStamp,ITmfTimestamp.NANOSECOND_SCALE);
		Random rnd = new Random();
		long payload = Long.valueOf(rnd.nextInt(10));
		
		/* Put the value in a field
		 * The field is required by SystraceStateProvider.eventHandle()*/
		final TmfEventField tmfEventField = new TmfEventField("value", payload, null); //$NON-NLS-1$
		final TmfEventField tmfEventField_IRQ = new TmfEventField(SystraceStrings.IRQ, Long.parseLong(rlist.get(1).trim()), null); //$NON-NLS-1$
		/* The name is optional, could be safe removed */
		final TmfEventField tmfEventField_NAME = new TmfEventField(SystraceStrings.IRQ_NAME, rlist.get(2).trim(), null); //$NON-NLS-1$
		
		// the field must be in an array
		final TmfEventField[] fields = new TmfEventField[3];
		fields[0] = tmfEventField;
		fields[1] = tmfEventField_IRQ;
		fields[2] = tmfEventField_NAME;
		
		final TmfEventField content = new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null, fields);
		SystraceEvent event = new SystraceEvent(null, _currentRank, ts, String.valueOf(this._currentRank),new TmfEventType(head.title, head.title, null), content, suffStr, head.cpuId, head.title);
		
		return event;
	}

	private final ITmfEvent handleSoftIrqEvent(String line){
		
		Head head = parseHead(line);

		String suffStr = head.suffStr;
		suffStr = StringUtil.remove(suffStr, "[");
		suffStr = StringUtil.remove(suffStr, "]");
		suffStr = StringUtil.replace(suffStr, "vec" , "||");
		suffStr = StringUtil.replace(suffStr, "action"  , "||");
		
		@SuppressWarnings("unchecked")
		List<String> rlist = StringUtil.splitAsList(suffStr, "||=");
		
		TmfTimestamp ts = new TmfTimestamp(head.timeStamp,ITmfTimestamp.NANOSECOND_SCALE);
		Random rnd = new Random();
		long payload = Long.valueOf(rnd.nextInt(10));
		
		/* Put the value in a field
		 * The field is required by SystraceStateProvider.eventHandle()*/
		final TmfEventField tmfEventField = new TmfEventField("value", payload, null); //$NON-NLS-1$
		final TmfEventField tmfEventField_VEC = new TmfEventField(SystraceStrings.VEC, Long.parseLong(rlist.get(1).trim()), null); //$NON-NLS-1$
		/* The action field is optional, could be safe remove */
		final TmfEventField tmfEventField_ACTION = new TmfEventField(SystraceStrings.ACTION, rlist.get(2).trim(), null); //$NON-NLS-1$
		
		// the field must be in an array
		final TmfEventField[] fields = new TmfEventField[3];
		fields[0] = tmfEventField;
		fields[1] = tmfEventField_VEC;
		fields[2] = tmfEventField_ACTION;
		
		final TmfEventField content = new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null, fields);
		SystraceEvent event = new SystraceEvent(null, _currentRank, ts, String.valueOf(this._currentRank),new TmfEventType(head.title, head.title, null), content, suffStr, head.cpuId, head.title);
		
		return event;
	}
	
	private final ITmfEvent handleUndefinedEvent(String line){
		
		Head head = parseHead(line);
		
		TmfTimestamp ts = new TmfTimestamp(head.timeStamp,ITmfTimestamp.NANOSECOND_SCALE);
		Random rnd = new Random();
		long payload = Long.valueOf(rnd.nextInt(10));
		
		// put the value in a field
		final TmfEventField tmfEventField = new TmfEventField("value", payload, null); //$NON-NLS-1$
		
		// the field must be in an array
		final TmfEventField[] fields = new TmfEventField[1];
		fields[0] = tmfEventField;
		
		final TmfEventField content = new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null, fields);
		SystraceEvent event = new SystraceEvent(null, _currentRank, ts, String.valueOf(this._currentRank),new TmfEventType(head.title, head.title, null), content, line, head.cpuId, head.title);
		
		return event;
	}
	
	private final ITmfEvent handleSchedleWakeupEvent(String line){
		
		Head head = parseHead(line);

		String suffStr = head.suffStr;
		suffStr = StringUtil.replace(suffStr, "comm" , "||");
		suffStr = StringUtil.replace(suffStr, "pid"  , "||");
		suffStr = StringUtil.replace(suffStr, "prio" , "||");
		suffStr = StringUtil.replace(suffStr, "success", "||");
		suffStr = StringUtil.replace(suffStr, "target_cpu" , "||");
		
		@SuppressWarnings("unchecked")
		List<String> rlist = StringUtil.splitAsList(suffStr, "||=");
		
		TmfTimestamp ts = new TmfTimestamp(head.timeStamp,ITmfTimestamp.NANOSECOND_SCALE);
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
		SystraceEvent event = new SystraceEvent(null, _currentRank, ts, String.valueOf(this._currentRank),new TmfEventType(head.title, head.title, null), content, suffStr, head.cpuId, head.title);
		
		return event;
	}
	
	private ITmfEvent handleSchedleSwitchEvent(String line){
		
		Head head = parseHead(line);
		
		String suffStr = head.suffStr;
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
		
		TmfTimestamp ts = new TmfTimestamp(head.timeStamp,ITmfTimestamp.NANOSECOND_SCALE);
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

		SystraceEvent event = new SystraceEvent(null, _currentRank, ts, String.valueOf(this._currentRank),new TmfEventType(head.title, head.title, null), content, suffStr, head.cpuId, head.title);
		
		return event;
	}

}
