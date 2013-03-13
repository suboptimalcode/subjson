package su.boptim.al.subjson;

import java.util.HashMap;
import java.lang.management.ManagementFactory;

public class SectionTimer
{
    // Map of section names to the amount of time spent in those
    // sections so far.
    static HashMap<String, Long> timings;
    static HashMap<String, Long> counts;
    
    // Map of section names to the time those sections were last
    // entered.
    static HashMap<String, Long> inFlight;

    static {
        reset();
    }

    /* Clear all timing information */
    public static void reset()
    {
        timings = new HashMap<String, Long>();
        counts = new HashMap<String, Long>();
        inFlight = new HashMap<String, Long>();
    }

    public static void enterSection(String sectionName)
    {
        inFlight.put(sectionName, ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime());
    }

    public static void leaveSection(String sectionName)
    {
        long finishTime = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
        long enterTime = inFlight.get(sectionName);
        long timeSoFar = timings.containsKey(sectionName) ? timings.get(sectionName) : 0;
        long countSoFar = counts.containsKey(sectionName) ? counts.get(sectionName) : 0;

        timings.put(sectionName, timeSoFar + (finishTime - enterTime));
        counts.put(sectionName, countSoFar + 1);

        inFlight.remove(sectionName);
    }

    // Return the timing map.
    public static HashMap<String, Long> getTimings()
    {
        return (HashMap<String, Long>)timings.clone();
    }

    public static HashMap<String, Long> getCounts()
    {
        return (HashMap<String, Long>)counts.clone();
    }
}
