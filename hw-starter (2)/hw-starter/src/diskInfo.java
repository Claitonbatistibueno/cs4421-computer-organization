import java.io.File;

public class diskInfo {

    static String DISKINFO = "/sys/block";

    public static String getDriveType() {
        File directory = new File(DISKINFO);
        File[] filesList = directory.listFiles();
        
        if (filesList == null) return "Error, couldn't find directory in -> " + DISKINFO;

        for (File f : filesList) {
            if (f.getName().matches("sdd")) return "sdd";
            else if (f.getName().matches("hdd")) return "hdd";
        }
        return "Error, directory found but couldn't locate ";
    }
}
