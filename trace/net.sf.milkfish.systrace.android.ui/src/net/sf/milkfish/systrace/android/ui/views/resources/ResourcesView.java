package net.sf.milkfish.systrace.android.ui.views.resources;

import java.util.ArrayList;
import java.util.List;

import net.sf.milkfish.systrace.android.core.AndroidTrace;
import net.sf.milkfish.systrace.android.ui.Messages;
import net.sf.milkfish.systrace.android.ui.views.resources.ResourcesEntry.Type;
import net.sf.milkfish.systrace.core.state.SystraceAttributes;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.linuxtools.tmf.core.exceptions.AttributeNotFoundException;
import org.eclipse.linuxtools.tmf.core.exceptions.StateSystemDisposedException;
import org.eclipse.linuxtools.tmf.core.exceptions.StateValueTypeException;
import org.eclipse.linuxtools.tmf.core.exceptions.TimeRangeException;
import org.eclipse.linuxtools.tmf.core.interval.ITmfStateInterval;
import org.eclipse.linuxtools.tmf.core.statesystem.ITmfStateSystem;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.core.trace.TmfTraceManager;
import org.eclipse.linuxtools.tmf.ui.views.timegraph.AbstractTimeGraphView;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.model.NullTimeEvent;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.model.TimeEvent;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.model.TimeGraphEntry;

public class ResourcesView extends AbstractTimeGraphView {

    /** View ID. */
    public static final String ID = "net.sf.milkfish.systrace.android.ui.views.resources.ResourcesView"; //$NON-NLS-1$

    private static final String[] FILTER_COLUMN_NAMES = new String[] {
            Messages.ResourcesView_stateTypeName
    };

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * Default constructor
     */
    public ResourcesView() {
        super(ID, new ResourcesPresentationProvider());
        setFilterColumns(FILTER_COLUMN_NAMES);
    }

    @Override
    protected String getNextText() {
        return Messages.ResourcesView_nextResourceActionNameText;
    }

    @Override
    protected String getNextTooltip() {
        return Messages.ResourcesView_nextResourceActionToolTipText;
    }

    @Override
    protected String getPrevText() {
        return Messages.ResourcesView_previousResourceActionNameText;
    }

    @Override
    protected String getPrevTooltip() {
        return Messages.ResourcesView_previousResourceActionToolTipText;
    }

    // ------------------------------------------------------------------------
    // Internal
    // ------------------------------------------------------------------------

    @Override
    protected void buildEventList(ITmfTrace trace, IProgressMonitor monitor) {
        setStartTime(Long.MAX_VALUE);
        setEndTime(Long.MIN_VALUE);

        ArrayList<ResourcesEntry> entryList = new ArrayList<ResourcesEntry>();
        for (ITmfTrace aTrace : TmfTraceManager.getTraceSet(trace)) {
            if (monitor.isCanceled()) {
                return;
            }
            if (aTrace instanceof AndroidTrace) {
            	AndroidTrace lttngKernelTrace = (AndroidTrace) aTrace;
                ITmfStateSystem ssq = lttngKernelTrace.getStateSystems().get(AndroidTrace.STATE_ID);
                if (!ssq.waitUntilBuilt()) {
                    return;
                }
                long startTime = ssq.getStartTime();
                long endTime = ssq.getCurrentEndTime() + 1;
                ResourcesEntry groupEntry = new ResourcesEntry(lttngKernelTrace, aTrace.getName(), startTime, endTime, 0);
                entryList.add(groupEntry);
                setStartTime(Math.min(getStartTime(), startTime));
                setEndTime(Math.max(getEndTime(), endTime));
                List<Integer> cpuQuarks = ssq.getQuarks(SystraceAttributes.CPUS, "*"); //$NON-NLS-1$
                ResourcesEntry[] cpuEntries = new ResourcesEntry[cpuQuarks.size()];
                for (int i = 0; i < cpuQuarks.size(); i++) {
                    int cpuQuark = cpuQuarks.get(i);
                    int cpu = Integer.parseInt(ssq.getAttributeName(cpuQuark));
                    ResourcesEntry entry = new ResourcesEntry(cpuQuark, lttngKernelTrace, getStartTime(), getEndTime(), Type.CPU, cpu);
                    groupEntry.addChild(entry);
                    cpuEntries[i] = entry;
                }
                List<Integer> irqQuarks = ssq.getQuarks(SystraceAttributes.RESOURCES, SystraceAttributes.IRQS, "*"); //$NON-NLS-1$
                ResourcesEntry[] irqEntries = new ResourcesEntry[irqQuarks.size()];
                for (int i = 0; i < irqQuarks.size(); i++) {
                    int irqQuark = irqQuarks.get(i);
                    int irq = Integer.parseInt(ssq.getAttributeName(irqQuark));
                    ResourcesEntry entry = new ResourcesEntry(irqQuark, lttngKernelTrace, getStartTime(), getEndTime(), Type.IRQ, irq);
                    groupEntry.addChild(entry);
                    irqEntries[i] = entry;
                }
                List<Integer> softIrqQuarks = ssq.getQuarks(SystraceAttributes.RESOURCES, SystraceAttributes.SOFT_IRQS, "*"); //$NON-NLS-1$
                ResourcesEntry[] softIrqEntries = new ResourcesEntry[softIrqQuarks.size()];
                for (int i = 0; i < softIrqQuarks.size(); i++) {
                    int softIrqQuark = softIrqQuarks.get(i);
                    int softIrq = Integer.parseInt(ssq.getAttributeName(softIrqQuark));
                    ResourcesEntry entry = new ResourcesEntry(softIrqQuark, lttngKernelTrace, getStartTime(), getEndTime(), Type.SOFT_IRQ, softIrq);
                    groupEntry.addChild(entry);
                    softIrqEntries[i] = entry;
                }
            }
        }
        putEntryList(trace, new ArrayList<TimeGraphEntry>(entryList));

        if (trace.equals(getTrace())) {
            refresh();
        }
        for (ResourcesEntry traceEntry : entryList) {
            if (monitor.isCanceled()) {
                return;
            }
            AndroidTrace lttngKernelTrace = traceEntry.getTrace();
            ITmfStateSystem ssq = lttngKernelTrace.getStateSystems().get(AndroidTrace.STATE_ID);
            long startTime = ssq.getStartTime();
            long endTime = ssq.getCurrentEndTime() + 1;
            long resolution = (endTime - startTime) / getDisplayWidth();
            for (TimeGraphEntry entry : traceEntry.getChildren()) {
                List<ITimeEvent> eventList = getEventList(entry, startTime, endTime, resolution, monitor);
                entry.setEventList(eventList);
                redraw();
            }
        }
    }

    @Override
    protected List<ITimeEvent> getEventList(TimeGraphEntry entry,
            long startTime, long endTime, long resolution,
            IProgressMonitor monitor) {
        ResourcesEntry resourcesEntry = (ResourcesEntry) entry;
        ITmfStateSystem ssq = resourcesEntry.getTrace().getStateSystems().get(AndroidTrace.STATE_ID);
        final long realStart = Math.max(startTime, ssq.getStartTime());
        final long realEnd = Math.min(endTime, ssq.getCurrentEndTime() + 1);
        if (realEnd <= realStart) {
            return null;
        }
        List<ITimeEvent> eventList = null;
        int quark = resourcesEntry.getQuark();

        try {
            if (resourcesEntry.getType().equals(Type.CPU)) {
                int statusQuark = ssq.getQuarkRelative(quark, SystraceAttributes.STATUS);
                List<ITmfStateInterval> statusIntervals = ssq.queryHistoryRange(statusQuark, realStart, realEnd - 1, resolution, monitor);
                eventList = new ArrayList<ITimeEvent>(statusIntervals.size());
                long lastEndTime = -1;
                for (ITmfStateInterval statusInterval : statusIntervals) {
                    if (monitor.isCanceled()) {
                        return null;
                    }
                    int status = statusInterval.getStateValue().unboxInt();
                    long time = statusInterval.getStartTime();
                    long duration = statusInterval.getEndTime() - time + 1;
                    if (!statusInterval.getStateValue().isNull()) {
                        if (lastEndTime != time && lastEndTime != -1) {
                            eventList.add(new TimeEvent(entry, lastEndTime, time - lastEndTime));
                        }
                        eventList.add(new TimeEvent(entry, time, duration, status));
                    } else if (lastEndTime == -1 || time + duration >= endTime) {
                        // add null event if it intersects the start or end time
                        eventList.add(new NullTimeEvent(entry, time, duration));
                    }
                    lastEndTime = time + duration;
                }
            } else if (resourcesEntry.getType().equals(Type.IRQ)) {
                List<ITmfStateInterval> irqIntervals = ssq.queryHistoryRange(quark, realStart, realEnd - 1, resolution, monitor);
                eventList = new ArrayList<ITimeEvent>(irqIntervals.size());
                long lastEndTime = -1;
                boolean lastIsNull = true;
                for (ITmfStateInterval irqInterval : irqIntervals) {
                    if (monitor.isCanceled()) {
                        return null;
                    }
                    long time = irqInterval.getStartTime();
                    long duration = irqInterval.getEndTime() - time + 1;
                    if (!irqInterval.getStateValue().isNull()) {
                        int cpu = irqInterval.getStateValue().unboxInt();
                        eventList.add(new TimeEvent(entry, time, duration, cpu));
                        lastIsNull = false;
                    } else {
                        if (lastEndTime == -1) {
                            // add null event if it intersects the start time
                            eventList.add(new NullTimeEvent(entry, time, duration));
                        } else {
                            if (lastEndTime != time && lastIsNull) {
                                /* This is a special case where we want to show IRQ_ACTIVE state but we don't know the CPU (it is between two null samples) */
                                eventList.add(new TimeEvent(entry, lastEndTime, time - lastEndTime, -1));
                            }
                            if (time + duration >= endTime) {
                                // add null event if it intersects the end time
                                eventList.add(new NullTimeEvent(entry, time, duration));
                            }
                        }
                        lastIsNull = true;
                    }
                    lastEndTime = time + duration;
                }
            } else if (resourcesEntry.getType().equals(Type.SOFT_IRQ)) {
                List<ITmfStateInterval> softIrqIntervals = ssq.queryHistoryRange(quark, realStart, realEnd - 1, resolution, monitor);
                eventList = new ArrayList<ITimeEvent>(softIrqIntervals.size());
                long lastEndTime = -1;
                boolean lastIsNull = true;
                for (ITmfStateInterval softIrqInterval : softIrqIntervals) {
                    if (monitor.isCanceled()) {
                        return null;
                    }
                    long time = softIrqInterval.getStartTime();
                    long duration = softIrqInterval.getEndTime() - time + 1;
                    if (!softIrqInterval.getStateValue().isNull()) {
                        int cpu = softIrqInterval.getStateValue().unboxInt();
                        eventList.add(new TimeEvent(entry, time, duration, cpu));
                    } else {
                        if (lastEndTime == -1) {
                            // add null event if it intersects the start time
                            eventList.add(new NullTimeEvent(entry, time, duration));
                        } else {
                            if (lastEndTime != time && lastIsNull) {
                                /* This is a special case where we want to show IRQ_ACTIVE state but we don't know the CPU (it is between two null samples) */
                                eventList.add(new TimeEvent(entry, lastEndTime, time - lastEndTime, -1));
                            }
                            if (time + duration >= endTime) {
                                // add null event if it intersects the end time
                                eventList.add(new NullTimeEvent(entry, time, duration));
                            }
                        }
                        lastIsNull = true;
                    }
                    lastEndTime = time + duration;
                }
            }

        } catch (AttributeNotFoundException e) {
            e.printStackTrace();
        } catch (TimeRangeException e) {
            e.printStackTrace();
        } catch (StateValueTypeException e) {
            e.printStackTrace();
        } catch (StateSystemDisposedException e) {
            /* Ignored */
        }
        return eventList;
    }

}
