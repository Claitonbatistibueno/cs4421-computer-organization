import java.nio.file.Path;

/**
 * Information about memory, read from /proc/meminfo. All sizes are in KiB
 * (1 KiB = 1024 bytes), which is the unit Linux uses in that file.
 */
public class memInfo
{
    private static final Path MEMINFO = sysFile.path("/proc/meminfo");

    /** Total installed memory in KiB, or -1 if it cannot be read. */
    public static double getTotal()
    {
        return field("MemTotal");
    }

    /**
     * Memory that programs could use right now (free plus easily reclaimed), in KiB.
     * MemAvailable only exists since Linux 3.14. On older kernels we add up MemFree,
     * Buffers and Cached instead, which is a close estimate of the same thing.
     */
    public static double getAvailable()
    {
        double available = field("MemAvailable");
        if (available >= 0)
            return available;

        double free = field("MemFree");
        if (free < 0)
            return -1;
        return free + Math.max(0, field("Buffers")) + Math.max(0, field("Cached"));
    }

    /** Memory in use in KiB: total minus available. */
    public static double getUsed()
    {
        double total = getTotal();
        double available = getAvailable();
        return (total < 0 || available < 0) ? -1 : total - available;
    }

    /** The value in KiB of a named line of /proc/meminfo such as "SwapTotal", or -1. */
    public static double field(String name)
    {

        for (String line : sysFile.lines(MEMINFO)) {
            if (line.startsWith(name + ":")) {
                String[] part = line.substring(name.length() + 1).strip().split("\\s+");
                try {
                    return Double.parseDouble(part[0]);
                } catch (NumberFormatException e) {
                    return -1;
                }
            }
        }
        return -1;
    }

}
