import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

// Finds the first real disk in /sys/block and reads whether it spins.
// /sys/block/<disk>/queue/rotational holds 1 for a spinning hard drive (HDD)
// and 0 for a solid state drive (SSD).
public class diskInfo {

    static String DISKINFO = "/sys/block";

    // not physical disks: loop, ram and zram (virtual or RAM disks), dm- and md (volumes and RAID
    // built on top of real disks), sr (CD drive), fd (floppy), nbd (network disk), zd (ZFS volume).
    // fd0 and nbd0 come before nvme0n1 and sda in alphabetical order, so they must be skipped.
    static final String[] SKIP = {"loop", "ram", "zram", "dm-", "md", "sr", "fd", "nbd", "zd"};

    static boolean isRealDisk(String name) {
        for (String s : SKIP)
            if (name.startsWith(s)) return false;
        return true;
    }

    // The name of the first real disk, for example "sda", "vda" or "nvme0n1", or "" if none
    public static String getDiskName() {
        File[] filesList = sysFile.path(DISKINFO).toFile().listFiles();
        if (filesList == null) return "";

        java.util.Arrays.sort(filesList);
        for (File f : filesList)
            if (isRealDisk(f.getName())) return f.getName();
        return "";
    }

    // "SSD", "HDD" or "unknown"
    public static String getDriveType() {
        String disk = getDiskName();
        if (disk.isEmpty()) return "unknown";

        Path rotational = sysFile.path(DISKINFO + "/" + disk + "/queue/rotational");
        try {
            String value = Files.readString(rotational).trim();
            if (value.equals("0")) return "SSD";
            if (value.equals("1")) return "HDD";
        } catch (Exception e) {
            // file missing, so the type is unknown
        }
        return "unknown";
    }

    // Disks named vd... are virtio disks, and inside any virtual machine (hypervisor flag in
    // /proc/cpuinfo, also true on WSL2) the disk is a file on the host. In both cases the
    // virtual machine decides what rotational says, not the real drive.
    public static boolean isVirtual() {
        return getDiskName().startsWith("vd") || cpuInfo.getHypervisor().equals("Yes");
    }
}
