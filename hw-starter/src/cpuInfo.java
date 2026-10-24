import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Information about the processor, read from /proc/cpuinfo and /proc/stat.
 *
 * The busy and idle values are counts of "ticks" since the machine booted
 * (usually 100 ticks per second). To find the load, read them twice, wait about
 * a second, read them again, and work out busy / (busy + idle) from the DIFFERENCES.
 */
public class cpuInfo
{
    private static final Path CPUINFO = sysFile.path("/proc/cpuinfo");
    private static final Path STAT = sysFile.path("/proc/stat");

    /** Number of cores (logical processors) the operating system sees. */
    public static int getCount()
    {
        int count = 0;
        for (String line : sysFile.lines(CPUINFO))
            if (line.startsWith("processor"))
                count++;
        return count;
    }

    /**
     * The processor model, for example "Intel(R) Xeon(R) Processor @ 2.80GHz".
     * On arm64 there is no model name line, so this returns "unknown"; use
     * getField("CPU part") or getField("CPU implementer") instead.
     */
    
    public static String getModel()
    {
        String model = getField("model name");
         //Flourish:  If the normal CPU model exists get it 
    if (!model.isEmpty()) {
        return model;
    }
    // Flourish: get the ARM64 info
    String implementer = getField("CPU implementer");
    String part = getField("CPU part");
        // Flourish: Checking if the ARM64 fields are missing
    if (implementer.isEmpty() && part.isEmpty()) {
        return "unknown";
    }
        //Flourish: added getField() calls and added labels for cleaner output

return "CPU Implementer" + getField("CPU implementer") + "  | CPU Part: " + getField("CPU part");


    }
    // Flourish: Get CPU frequency in MHz
    //craetinf a method for the frequncy that returns a string
public static String getFrequency()
{
    //calling exisitng getfiels method on the frequency
    String frequency = getField("cpu MHz");
//if empty output something
    if (frequency.isEmpty()) {
        return "unknown";
    }
//print the frequency + the unit
    return frequency + " MHz";
}
// Flourish: Get logical threads 
//method to get the logical threads
public static String getLogicalThreads()
{
    //set a string varianble on the vallue called from getfield with the siblings param
    String threads = getField("siblings");
// prints something if blank (dont think it ever wiil be though)
    if (threads.isEmpty()) {
        return "unknown";
    }
// return the value read from getFields(siblings)
    return threads;
}
// Flourish: Get physical cores
// method to get the actual core
public static String getPhysicalCores()
{
    //set a string varianble on the vallue called from getfield with the cores param
    String cores = getField("cpu cores");
    return cores;
}
// Flourish: check if a hypervisor is beign used
//crate method
public static String getHypervisor()
{
    //set a string varianble on the vallue called from getfield with the flags param

    String flags = getField("flags");
//simple yes no detection

    if (flags.contains("hypervisor")) {
        return "Yes";
    }

    return "No";
}
    /** The value of the first line in /proc/cpuinfo whose name is exactly 'name', or "". */
    public static String getField(String name)
    {
        for (String line : sysFile.lines(CPUINFO)) {
            int colon = line.indexOf(':');
            if (colon > 0 && line.substring(0, colon).strip().equals(name))
                return line.substring(colon + 1).strip();
        }
        return "";
    }

    /** Ticks since boot that core number 'core' (0, 1, 2 ...) has spent doing work. */
    public static long getBusy(int core)
    {
        long[] f = fields(core);
        // user + nice + system + irq + softirq + steal
        return f == null ? -1 : f[0] + f[1] + f[2] + f[5] + f[6] + f[7];
    }

    /** Ticks since boot that core number 'core' has spent idle (including waiting for I/O). */
    public static long getIdle(int core)
    {
        long[] f = fields(core);
        return f == null ? -1 : f[3] + f[4];
    }

    /** The numbers from the "cpuN" line of /proc/stat, or null if there is no such core. */
    private static long[] fields(int core)
    {
        String prefix = "cpu" + core + " ";
        for (String line : sysFile.lines(STAT)) {
            if (line.startsWith(prefix)) {
                String[] part = line.strip().split("\\s+");
                long[] f = new long[8];
                for (int i = 0; i < 8; i++)
                    f[i] = (i + 1 < part.length) ? Long.parseLong(part[i + 1]) : 0;
                return f;
            }
        }
        return null;
    }

    public static double perCoreLoad(int core){
        try {
            long busy = cpuInfo.getBusy(core);
            long idle = cpuInfo.getIdle(core);
            TimeUnit.SECONDS.sleep(1); 
            busy = (cpuInfo.getBusy(core) - busy);
            idle = (cpuInfo.getIdle(core) - idle);
            long total = busy + idle;   
            double loadPercentage = (double) busy / total * 100;

            
            return loadPercentage;
        } catch (Exception e) {
            System.err.println("Error calculating load for core " + core + ": " + e.getMessage());
            return 0.0;
        }
    }

    public static double allCoresLoad() {
        int coreCount = cpuInfo.getCount();
        double totalLoad = 0.0;
        for (int i = 0; i < coreCount; i++) {
            totalLoad += perCoreLoad(i);
        }
        totalLoad /= coreCount;
        return totalLoad;
    }

    public static double[] fastLoad() {  //Added by Eoin for faster data collection
        int coreCount = cpuInfo.getCount();
        double coreBusyStarts[] = new double[coreCount];
        double coreIdleStarts[] = new double[coreCount];
        double coreLoads[] = new double[coreCount + 1];
        double averageLoad = 0;

        for (int i = 0; i < coreCount; i++) {
            coreBusyStarts[i] = (double) cpuInfo.getBusy(i);
            coreIdleStarts[i] = (double) cpuInfo.getIdle(i);
        }

        try {          
            TimeUnit.SECONDS.sleep(1); 
        } catch (Exception e) {
            System.err.println("Error calculating load");
            return null;
        }
        for (int i = 0; i < coreCount; i++) {
            double busy = ( cpuInfo.getBusy(i) - coreBusyStarts[i] );
            double idle = ( cpuInfo.getIdle(i) - coreIdleStarts[i] );
            double load = 100.0 * busy / (busy + idle);
            coreLoads[i] = load; 
            averageLoad += load;
        }
        
        coreLoads[coreLoads.length - 1] = (averageLoad / coreCount); // The last value is the average load
        return coreLoads;
    }
}
