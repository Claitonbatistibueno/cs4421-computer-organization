import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

// Finds the first real disk in /sys/block and reads whether it spins.
// /sys/block/<disk>/queue/rotational holds 1 for a spinning hard drive (HDD)
// and 0 for a solid state drive (SSD).
public class diskInfo {

    static String DISKINFO = "/sys/block";

    // loop, ram, zram, dm- and sr are not physical disks (virtual devices, RAM disks, CD drives)
    static final String[] SKIP = {"loop", "ram", "zram", "dm-", "sr", "md"};

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

    // Disks named vd... are virtio disks: the virtual machine decides what rotational says
    public static boolean isVirtual() {
        return getDiskName().startsWith("vd");
    }
}
