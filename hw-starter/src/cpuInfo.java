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
     * On arm64 there is no model name line, so the CPU implementer and part numbers
     * are returned instead, for example "CPU implementer: 0x41 | CPU part: 0xd08".
     * Returns "unknown" if none of these lines exist.
     */
    public static String getModel()
    {
        // Flourish: if the normal CPU model exists, use it
        String model = getField("model name");
        if (!model.isEmpty()) {
            return model;
        }

        // Flourish: arm64 has no model name, so use the implementer and part numbers
        String implementer = getField("CPU implementer");
        String part = getField("CPU part");
        if (implementer.isEmpty() && part.isEmpty()) {
            return "unknown";
        }
        return "CPU implementer: " + (implementer.isEmpty() ? "unknown" : implementer)
             + " | CPU part: " + (part.isEmpty() ? "unknown" : part);
    }
    // Flourish: CPU frequency in MHz, from the "cpu MHz" line
    public static String getFrequency()
    {
        String frequency = getField("cpu MHz");
        // arm64 has no "cpu MHz" line
        if (frequency.isEmpty()) {
            return "unknown";
        }
        return frequency + " MHz";
    }

    // Flourish: logical threads per package, from the "siblings" line
    // (with hyper-threading this is twice the number of physical cores)
    public static String getLogicalThreads()
    {
        String threads = getField("siblings");
        if (threads.isEmpty()) {
            return "unknown";
        }
        return threads;
    }

    // Flourish: physical cores per package, from the "cpu cores" line
    public static String getPhysicalCores()
    {
        String cores = getField("cpu cores");
        // arm64 has no "cpu cores" line
        if (cores.isEmpty()) {
            return "unknown";
        }
        return cores;
    }

    // Whether SMT is active, inferred from logical threads versus physical cores per package.
    public static String getSmt()
    {
        String threads = getLogicalThreads();
        String cores = getPhysicalCores();
        if (threads.equals("unknown") || cores.equals("unknown")) {
            return "unknown";
        }

        try {
            int logicalThreads = Integer.parseInt(threads);
            int physicalCores = Integer.parseInt(cores);
            if (logicalThreads < physicalCores) {
                return "unknown";
            }
            return logicalThreads > physicalCores ? "Yes" : "No";
        } catch (NumberFormatException e) {
            return "unknown";
        }
    }

    // Flourish: the "flags" line contains the word hypervisor when Linux runs inside
    // a virtual machine (for example alderaan or WSL2)
    public static String getHypervisor()
    {
        String flags = getField("flags");
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
            // no ticks counted (core missing from /proc/stat): avoid dividing by zero
            if (total <= 0) {
                return 0.0;
            }
            return (double) busy / total * 100;
        } catch (Exception e) {
            System.err.println("Error calculating load for core " + core + ": " + e.getMessage());
            return 0.0;
        }
    }

    public static double allCoresLoad() {
        int coreCount = cpuInfo.getCount();
        double totalLoad = 0.0;
        if (coreCount == 0) {
            return 0.0;
        }
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
            // no ticks counted (core missing from /proc/stat): avoid dividing by zero
            double load = (busy + idle > 0) ? 100.0 * busy / (busy + idle) : 0;
            coreLoads[i] = load;
            averageLoad += load;
        }

        // The last value is the average load (0 when no cores could be read)
        coreLoads[coreLoads.length - 1] = (coreCount > 0) ? averageLoad / coreCount : 0;
        return coreLoads;
    }
}
