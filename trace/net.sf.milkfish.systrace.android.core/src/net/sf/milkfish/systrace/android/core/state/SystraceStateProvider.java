/*******************************************************************************
 * Copyright (c) 2012, 2013 Ericsson
 * Copyright (c) 2010, 2011 ?cole Polytechnique de Montréal
 * Copyright (c) 2010, 2011 Alexandre Montplaisir <alexandre.montplaisir@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package net.sf.milkfish.systrace.android.core.state;

import java.util.HashMap;

import net.sf.milkfish.systrace.android.core.AndroidTrace;
import net.sf.milkfish.systrace.android.core.SystraceEvent;

import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.event.ITmfEventField;
import org.eclipse.linuxtools.tmf.core.exceptions.AttributeNotFoundException;
import org.eclipse.linuxtools.tmf.core.exceptions.StateValueTypeException;
import org.eclipse.linuxtools.tmf.core.exceptions.TimeRangeException;
import org.eclipse.linuxtools.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.linuxtools.tmf.core.statesystem.ITmfStateSystemBuilder;
import org.eclipse.linuxtools.tmf.core.statevalue.ITmfStateValue;
import org.eclipse.linuxtools.tmf.core.statevalue.TmfStateValue;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;

/**
 * This is the state change input plugin for TMF's state system which handles
 * the LTTng 2.0 kernel traces in CTF format.
 *
 * It uses the reference handler defined in CTFKernelHandler.java.
 *
 * @author alexmont
 *
 */
public class SystraceStateProvider extends AbstractTmfStateProvider {

    /**
     * Version number of this state provider. Please bump this if you modify the
     * contents of the generated state history in some way.
     */
    private static final int VERSION = 3;

    /* Event names HashMap. TODO: This can be discarded once we move to Java 7 */
    private final HashMap<String, Integer> knownEventNames;

    // ------------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------------

    /**
     * Instantiate a new state provider plugin.
     *
     * @param trace
     *            The LTTng 2.0 kernel trace directory
     */
    public SystraceStateProvider(ITmfTrace trace) {
        super(trace, ITmfEvent.class, "Android Systrace"); //$NON-NLS-1$
        knownEventNames = fillEventNames();
    }

    // ------------------------------------------------------------------------
    // IStateChangeInput
    // ------------------------------------------------------------------------

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public void assignTargetStateSystem(ITmfStateSystemBuilder ssb) {
        /* We can only set up the locations once the state system is assigned */
        super.assignTargetStateSystem(ssb);
    }

    @Override
    public SystraceStateProvider getNewInstance() {
        return new SystraceStateProvider((AndroidTrace) this.getTrace());
    }

    @Override
    protected void eventHandle(ITmfEvent ev) {
        /*
         * AbstractStateChangeInput should have already checked for the correct
         * class type
         */
    	SystraceEvent event = (SystraceEvent) ev;

        int quark;
        ITmfStateValue value;

        final ITmfEventField content = event.getContent();
        final String eventName = event.getEventName();
        final long ts = event.getTimestamp().getValue();

        try {
            /* Shortcut for the "current CPU" attribute node */
            final Integer currentCPUNode = ss.getQuarkRelativeAndAdd(getNodeCPUs(), String.valueOf(event.getCPU()));

            /*
             * Shortcut for the "current thread" attribute node. It requires
             * querying the current CPU's current thread.
             */
            quark = ss.getQuarkRelativeAndAdd(currentCPUNode, SystraceAttributes.CURRENT_THREAD);
            value = ss.queryOngoingState(quark);
            int thread = value.unboxInt();
            final Integer currentThreadNode = ss.getQuarkRelativeAndAdd(getNodeThreads(), String.valueOf(thread));

            /*
             * Feed event to the history system if it's known to cause a state
             * transition.
             */
            switch (getEventIndex(eventName)) {

            case 1: // "exit_syscall":
            /* Fields: int64 ret */
            {
                /* Clear the current system call on the process */
                quark = ss.getQuarkRelativeAndAdd(currentThreadNode, SystraceAttributes.SYSTEM_CALL);
                value = TmfStateValue.nullValue();
                ss.modifyAttribute(ts, value, quark);

                /* Put the process' status back to user mode */
                quark = ss.getQuarkRelativeAndAdd(currentThreadNode, SystraceAttributes.STATUS);
                value = SystraceStateValues.PROCESS_STATUS_RUN_USERMODE_VALUE;
                ss.modifyAttribute(ts, value, quark);

                /* Put the CPU's status back to user mode */
                quark = ss.getQuarkRelativeAndAdd(currentCPUNode, SystraceAttributes.STATUS);
                value = SystraceStateValues.CPU_STATUS_RUN_USERMODE_VALUE;
                ss.modifyAttribute(ts, value, quark);
            }
                break;

            case 2: // "irq_handler_entry":
            /* Fields: int32 irq, string name */
            {
                Integer irqId = ((Long) content.getField(SystraceStrings.IRQ).getValue()).intValue();

                /* Mark this IRQ as active in the resource tree.
                 * The state value = the CPU on which this IRQ is sitting */
                quark = ss.getQuarkRelativeAndAdd(getNodeIRQs(), irqId.toString());
                value = TmfStateValue.newValueInt(event.getCPU());
                ss.modifyAttribute(ts, value, quark);

                /* Change the status of the running process to interrupted */
                quark = ss.getQuarkRelativeAndAdd(currentThreadNode, SystraceAttributes.STATUS);
                value = SystraceStateValues.PROCESS_STATUS_INTERRUPTED_VALUE;
                ss.modifyAttribute(ts, value, quark);

                /* Change the status of the CPU to interrupted */
                quark = ss.getQuarkRelativeAndAdd(currentCPUNode, SystraceAttributes.STATUS);
                value = SystraceStateValues.CPU_STATUS_IRQ_VALUE;
                ss.modifyAttribute(ts, value, quark);
            }
                break;

            case 3: // "irq_handler_exit":
            /* Fields: int32 irq, int32 ret */
            {
                Integer irqId = ((Long) content.getField(SystraceStrings.IRQ).getValue()).intValue();

                /* Put this IRQ back to inactive in the resource tree */
                quark = ss.getQuarkRelativeAndAdd(getNodeIRQs(), irqId.toString());
                value = TmfStateValue.nullValue();
                ss.modifyAttribute(ts, value, quark);

                /* Set the previous process back to running */
                setProcessToRunning(ts, currentThreadNode);

                /* Set the CPU status back to running or "idle" */
                cpuExitInterrupt(ts, currentCPUNode, currentThreadNode);
            }
                break;

            case 4: // "softirq_entry":
            /* Fields: int32 vec */
            {
                Integer softIrqId = ((Long) content.getField(SystraceStrings.VEC).getValue()).intValue();

                /* Mark this SoftIRQ as active in the resource tree.
                 * The state value = the CPU on which this SoftIRQ is processed */
                quark = ss.getQuarkRelativeAndAdd(getNodeSoftIRQs(), softIrqId.toString());
                value = TmfStateValue.newValueInt(event.getCPU());
                ss.modifyAttribute(ts, value, quark);

                /* Change the status of the running process to interrupted */
                quark = ss.getQuarkRelativeAndAdd(currentThreadNode, SystraceAttributes.STATUS);
                value = SystraceStateValues.PROCESS_STATUS_INTERRUPTED_VALUE;
                ss.modifyAttribute(ts, value, quark);

                /* Change the status of the CPU to interrupted */
                quark = ss.getQuarkRelativeAndAdd(currentCPUNode, SystraceAttributes.STATUS);
                value = SystraceStateValues.CPU_STATUS_SOFTIRQ_VALUE;
                ss.modifyAttribute(ts, value, quark);
            }
                break;

            case 5: // "softirq_exit":
            /* Fields: int32 vec */
            {
                Integer softIrqId = ((Long) content.getField(SystraceStrings.VEC).getValue()).intValue();

                /* Put this SoftIRQ back to inactive (= -1) in the resource tree */
                quark = ss.getQuarkRelativeAndAdd(getNodeSoftIRQs(), softIrqId.toString());
                value = TmfStateValue.nullValue();
                ss.modifyAttribute(ts, value, quark);

                /* Set the previous process back to running */
                setProcessToRunning(ts, currentThreadNode);

                /* Set the CPU status back to "busy" or "idle" */
                cpuExitInterrupt(ts, currentCPUNode, currentThreadNode);
            }
                break;

            case 6: // "softirq_raise":
            /* Fields: int32 vec */
            {
                Integer softIrqId = ((Long) content.getField(SystraceStrings.VEC).getValue()).intValue();

                /* Mark this SoftIRQ as *raised* in the resource tree.
                 * State value = -2 */
                quark = ss.getQuarkRelativeAndAdd(getNodeSoftIRQs(), softIrqId.toString());
                value = SystraceStateValues.SOFT_IRQ_RAISED_VALUE;
                ss.modifyAttribute(ts, value, quark);
            }
                break;

            case 7: // "sched_switch":
            /*
             * Fields: string prev_comm, int32 prev_tid, int32 prev_prio, int64 prev_state,
             *         string next_comm, int32 next_tid, int32 next_prio
             */
            {
                Integer prevTid = ((Long) content.getField(SystraceStrings.PREV_TID).getValue()).intValue();
                Long prevState = (Long) content.getField(SystraceStrings.PREV_STATE).getValue();
                String nextProcessName = (String) content.getField(SystraceStrings.NEXT_COMM).getValue();
                Integer nextTid = ((Long) content.getField(SystraceStrings.NEXT_TID).getValue()).intValue();

                Integer formerThreadNode = ss.getQuarkRelativeAndAdd(getNodeThreads(), prevTid.toString());
                Integer newCurrentThreadNode = ss.getQuarkRelativeAndAdd(getNodeThreads(), nextTid.toString());

                /* Set the status of the process that got scheduled out. */
                quark = ss.getQuarkRelativeAndAdd(formerThreadNode, SystraceAttributes.STATUS);
                if (prevState != 0) {
                    value = SystraceStateValues.PROCESS_STATUS_WAIT_BLOCKED_VALUE;
                } else {
                    value = SystraceStateValues.PROCESS_STATUS_WAIT_FOR_CPU_VALUE;
                }
                ss.modifyAttribute(ts, value, quark);

                /* Set the status of the new scheduled process */
                setProcessToRunning(ts, newCurrentThreadNode);

                /* Set the exec name of the new process */
                quark = ss.getQuarkRelativeAndAdd(newCurrentThreadNode, SystraceAttributes.EXEC_NAME);
                value = TmfStateValue.newValueString(nextProcessName);
                ss.modifyAttribute(ts, value, quark);

                /* Make sure the PPID and system_call sub-attributes exist */
                ss.getQuarkRelativeAndAdd(newCurrentThreadNode, SystraceAttributes.SYSTEM_CALL);
                ss.getQuarkRelativeAndAdd(newCurrentThreadNode, SystraceAttributes.PPID);

                /* Set the current scheduled process on the relevant CPU */
                quark = ss.getQuarkRelativeAndAdd(currentCPUNode, SystraceAttributes.CURRENT_THREAD);
                value = TmfStateValue.newValueInt(nextTid);
                ss.modifyAttribute(ts, value, quark);

                /* Set the status of the CPU itself */
                if (nextTid > 0) {
                    /* Check if the entering process is in kernel or user mode */
                    quark = ss.getQuarkRelativeAndAdd(newCurrentThreadNode, SystraceAttributes.SYSTEM_CALL);
                    if (ss.queryOngoingState(quark).isNull()) {
                        value = SystraceStateValues.CPU_STATUS_RUN_USERMODE_VALUE;
                    } else {
                        value = SystraceStateValues.CPU_STATUS_RUN_SYSCALL_VALUE;
                    }
                } else {
                    value = SystraceStateValues.CPU_STATUS_IDLE_VALUE;
                }
                quark = ss.getQuarkRelativeAndAdd(currentCPUNode, SystraceAttributes.STATUS);
                ss.modifyAttribute(ts, value, quark);
            }
                break;

            case 8: // "sched_process_fork":
            /* Fields: string parent_comm, int32 parent_tid,
             *         string child_comm, int32 child_tid */
            {
                // String parentProcessName = (String) event.getFieldValue("parent_comm");
                String childProcessName = (String) content.getField(SystraceStrings.CHILD_COMM).getValue();
                // assert ( parentProcessName.equals(childProcessName) );

                Integer parentTid = ((Long) content.getField(SystraceStrings.PARENT_TID).getValue()).intValue();
                Integer childTid = ((Long) content.getField(SystraceStrings.CHILD_TID).getValue()).intValue();

                Integer parentTidNode = ss.getQuarkRelativeAndAdd(getNodeThreads(), parentTid.toString());
                Integer childTidNode = ss.getQuarkRelativeAndAdd(getNodeThreads(), childTid.toString());

                /* Assign the PPID to the new process */
                quark = ss.getQuarkRelativeAndAdd(childTidNode, SystraceAttributes.PPID);
                value = TmfStateValue.newValueInt(parentTid);
                ss.modifyAttribute(ts, value, quark);

                /* Set the new process' exec_name */
                quark = ss.getQuarkRelativeAndAdd(childTidNode, SystraceAttributes.EXEC_NAME);
                value = TmfStateValue.newValueString(childProcessName);
                ss.modifyAttribute(ts, value, quark);

                /* Set the new process' status */
                quark = ss.getQuarkRelativeAndAdd(childTidNode, SystraceAttributes.STATUS);
                value = SystraceStateValues.PROCESS_STATUS_WAIT_FOR_CPU_VALUE;
                ss.modifyAttribute(ts, value, quark);

                /* Set the process' syscall name, to be the same as the parent's */
                quark = ss.getQuarkRelativeAndAdd(parentTidNode, SystraceAttributes.SYSTEM_CALL);
                value = ss.queryOngoingState(quark);
                if (value.isNull()) {
                    /*
                     * Maybe we were missing info about the parent? At least we
                     * will set the child right. Let's suppose "sys_clone".
                     */
                    value = TmfStateValue.newValueString(SystraceStrings.SYS_CLONE);
                }
                quark = ss.getQuarkRelativeAndAdd(childTidNode, SystraceAttributes.SYSTEM_CALL);
                ss.modifyAttribute(ts, value, quark);
            }
                break;

            case 9: // "sched_process_exit":
            /* Fields: string comm, int32 tid, int32 prio */
                break;

            case 10: // "sched_process_free":
            /* Fields: string comm, int32 tid, int32 prio */
            /*
             * A sched_process_free will always happen after the sched_switch
             * that will remove the process from the cpu for the last time. So
             * this is when we should delete everything wrt to the process.
             */
            {
                Integer tid = ((Long) content.getField(SystraceStrings.TID).getValue()).intValue();
                /*
                 * Remove the process and all its sub-attributes from the
                 * current state
                 */
                quark = ss.getQuarkRelativeAndAdd(getNodeThreads(), tid.toString());
                ss.removeAttribute(ts, quark);
            }
                break;

            case 11: // "lttng_statedump_process_state":
            /* Fields:
             * int32 type, int32 mode, int32 pid, int32 submode, int32 vpid,
             * int32 ppid, int32 tid, string name, int32 status, int32 vtid */
            {
                Integer tid = ((Long) content.getField(SystraceStrings.TID).getValue()).intValue();
                int ppid = ((Long) content.getField(SystraceStrings.PPID).getValue()).intValue();
                int status = ((Long) content.getField(SystraceStrings.STATUS).getValue()).intValue();
                String name = (String) content.getField(SystraceStrings.NAME).getValue();
                /*
                 * "mode" could be interesting too, but it doesn't seem to be
                 * populated with anything relevant for now.
                 */

                int curThreadNode = ss.getQuarkRelativeAndAdd(getNodeThreads(), tid.toString());

                /* Set the process' name */
                quark = ss.getQuarkRelativeAndAdd(curThreadNode, SystraceAttributes.EXEC_NAME);
                if (ss.queryOngoingState(quark).isNull()) {
                    /* If the value didn't exist previously, set it */
                    value = TmfStateValue.newValueString(name);
                    ss.modifyAttribute(ts, value, quark);
                }

                /* Set the process' PPID */
                quark = ss.getQuarkRelativeAndAdd(curThreadNode, SystraceAttributes.PPID);
                if (ss.queryOngoingState(quark).isNull()) {
                    value = TmfStateValue.newValueInt(ppid);
                    ss.modifyAttribute(ts, value, quark);
                }

                /* Set the process' status */
                quark = ss.getQuarkRelativeAndAdd(curThreadNode, SystraceAttributes.STATUS);
                if (ss.queryOngoingState(quark).isNull()) {
                     /* "2" here means "WAIT_FOR_CPU", and "5" "WAIT_BLOCKED" in the LTTng kernel. */
                    if (status == 2) {
                        value = SystraceStateValues.PROCESS_STATUS_WAIT_FOR_CPU_VALUE;
                    } else if (status == 5) {
                        value = SystraceStateValues.PROCESS_STATUS_WAIT_BLOCKED_VALUE;
                    } else {
                        value = SystraceStateValues.PROCESS_STATUS_UNKNOWN_VALUE;
                    }
                    ss.modifyAttribute(ts, value, quark);
                }
            }
                break;

            case 12: // "sched_wakeup":
            case 13: // "sched_wakeup_new":
            /* Fields (same fields for both types):
             * string comm, int32 pid, int32 prio, int32 success,
             * int32 target_cpu */
            {
                final int tid = ((Long) content.getField(SystraceStrings.TID).getValue()).intValue();
                final int threadNode = ss.getQuarkRelativeAndAdd(getNodeThreads(), String.valueOf(tid));

                /*
                 * The process indicated in the event's payload is now ready to
                 * run. Assign it to the "wait for cpu" state, but only if it
                 * was not already running.
                 */
                quark = ss.getQuarkRelativeAndAdd(threadNode, SystraceAttributes.STATUS);
                int status = ss.queryOngoingState(quark).unboxInt();

                if (status != SystraceStateValues.PROCESS_STATUS_RUN_SYSCALL &&
                    status != SystraceStateValues.PROCESS_STATUS_RUN_USERMODE) {
                    value = SystraceStateValues.PROCESS_STATUS_WAIT_FOR_CPU_VALUE;
                    ss.modifyAttribute(ts, value, quark);
                }
            }
                break;

            default:
            /* Other event types not covered by the main switch */
            {
                if (eventName.startsWith(SystraceStrings.SYSCALL_PREFIX)
                        || eventName.startsWith(SystraceStrings.COMPAT_SYSCALL_PREFIX)) {
                    /*
                     * This is a replacement for the old sys_enter event. Now
                     * syscall names are listed into the event type
                     */

                    /* Assign the new system call to the process */
                    quark = ss.getQuarkRelativeAndAdd(currentThreadNode, SystraceAttributes.SYSTEM_CALL);
                    value = TmfStateValue.newValueString(eventName);
                    ss.modifyAttribute(ts, value, quark);

                    /* Put the process in system call mode */
                    quark = ss.getQuarkRelativeAndAdd(currentThreadNode, SystraceAttributes.STATUS);
                    value = SystraceStateValues.PROCESS_STATUS_RUN_SYSCALL_VALUE;
                    ss.modifyAttribute(ts, value, quark);

                    /* Put the CPU in system call (kernel) mode */
                    quark = ss.getQuarkRelativeAndAdd(currentCPUNode, SystraceAttributes.STATUS);
                    value = SystraceStateValues.CPU_STATUS_RUN_SYSCALL_VALUE;
                    ss.modifyAttribute(ts, value, quark);
                }
            }
                break;
            } // End of big switch

        } catch (AttributeNotFoundException ae) {
            /*
             * This would indicate a problem with the logic of the manager here,
             * so it shouldn't happen.
             */
            ae.printStackTrace();

        } catch (TimeRangeException tre) {
            /*
             * This would happen if the events in the trace aren't ordered
             * chronologically, which should never be the case ...
             */
            System.err.println("TimeRangeExcpetion caught in the state system's event manager."); //$NON-NLS-1$
            System.err.println("Are the events in the trace correctly ordered?"); //$NON-NLS-1$
            tre.printStackTrace();

        } catch (StateValueTypeException sve) {
            /*
             * This would happen if we were trying to push/pop attributes not of
             * type integer. Which, once again, should never happen.
             */
            sve.printStackTrace();
        }
    }

    // ------------------------------------------------------------------------
    // Convenience methods for commonly-used attribute tree locations
    // ------------------------------------------------------------------------

    private int getNodeCPUs() {
        return ss.getQuarkAbsoluteAndAdd(SystraceAttributes.CPUS);
    }

    private int getNodeThreads() {
        return ss.getQuarkAbsoluteAndAdd(SystraceAttributes.THREADS);
    }

    private int getNodeIRQs() {
        return ss.getQuarkAbsoluteAndAdd(SystraceAttributes.RESOURCES, SystraceAttributes.IRQS);
    }

    private int getNodeSoftIRQs() {
        return ss.getQuarkAbsoluteAndAdd(SystraceAttributes.RESOURCES, SystraceAttributes.SOFT_IRQS);
    }

    // ------------------------------------------------------------------------
    // Workaround for the lack of switch-on-strings in Java < 7
    // ------------------------------------------------------------------------

    private static HashMap<String, Integer> fillEventNames() {
        /*
         * TODO Replace with straight strings in the switch/case once we move to
         * Java 7
         */
        HashMap<String, Integer> map = new HashMap<String, Integer>();

        map.put(SystraceStrings.EXIT_SYSCALL, 1);
        map.put(SystraceStrings.IRQ_HANDLER_ENTRY, 2);
        map.put(SystraceStrings.IRQ_HANDLER_EXIT, 3);
        map.put(SystraceStrings.SOFTIRQ_ENTRY, 4);
        map.put(SystraceStrings.SOFTIRQ_EXIT, 5);
        map.put(SystraceStrings.SOFTIRQ_RAISE, 6);
        map.put(SystraceStrings.SCHED_SWITCH, 7);
        map.put(SystraceStrings.SCHED_PROCESS_FORK, 8);
        map.put(SystraceStrings.SCHED_PROCESS_EXIT, 9);
        map.put(SystraceStrings.SCHED_PROCESS_FREE, 10);
        map.put(SystraceStrings.STATEDUMP_PROCESS_STATE, 11);
        map.put(SystraceStrings.SCHED_WAKEUP, 12);
        map.put(SystraceStrings.SCHED_WAKEUP_NEW, 13);

        return map;
    }

    private int getEventIndex(String eventName) {
        Integer ret = knownEventNames.get(eventName);
        return (ret != null) ? ret : -1;
    }

    // ------------------------------------------------------------------------
    // Advanced state-setting methods
    // ------------------------------------------------------------------------

    /**
     * When we want to set a process back to a "running" state, first check
     * its current System_call attribute. If there is a system call active, we
     * put the process back in the syscall state. If not, we put it back in
     * user mode state.
     */
    private void setProcessToRunning(long ts, int currentThreadNode)
            throws AttributeNotFoundException, TimeRangeException,
            StateValueTypeException {
        int quark;
        ITmfStateValue value;

        quark = ss.getQuarkRelativeAndAdd(currentThreadNode, SystraceAttributes.SYSTEM_CALL);
        if (ss.queryOngoingState(quark).isNull()) {
            /* We were in user mode before the interruption */
            value = SystraceStateValues.PROCESS_STATUS_RUN_USERMODE_VALUE;
        } else {
            /* We were previously in kernel mode */
            value = SystraceStateValues.PROCESS_STATUS_RUN_SYSCALL_VALUE;
        }
        quark = ss.getQuarkRelativeAndAdd(currentThreadNode, SystraceAttributes.STATUS);
        ss.modifyAttribute(ts, value, quark);
    }

    /**
     * Similar logic as above, but to set the CPU's status when it's coming out
     * of an interruption.
     */
    private void cpuExitInterrupt(long ts, int currentCpuNode, int currentThreadNode)
            throws StateValueTypeException, AttributeNotFoundException,
            TimeRangeException {
        int quark;
        ITmfStateValue value;

        quark = ss.getQuarkRelativeAndAdd(currentCpuNode, SystraceAttributes.CURRENT_THREAD);
        if (ss.queryOngoingState(quark).unboxInt() > 0) {
            /* There was a process on the CPU */
            quark = ss.getQuarkRelative(currentThreadNode, SystraceAttributes.SYSTEM_CALL);
            if (ss.queryOngoingState(quark).isNull()) {
                /* That process was in user mode */
                value = SystraceStateValues.CPU_STATUS_RUN_USERMODE_VALUE;
            } else {
                /* That process was in a system call */
                value = SystraceStateValues.CPU_STATUS_RUN_SYSCALL_VALUE;
            }
        } else {
            /* There was no real process scheduled, CPU was idle */
            value = SystraceStateValues.CPU_STATUS_IDLE_VALUE;
        }
        quark = ss.getQuarkRelativeAndAdd(currentCpuNode, SystraceAttributes.STATUS);
        ss.modifyAttribute(ts, value, quark);
    }
}
