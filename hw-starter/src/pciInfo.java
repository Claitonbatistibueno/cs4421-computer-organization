import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * The PCI devices that Linux has found, read from /sys/bus/pci/devices.
 *
 * Each device has a folder named like "0000:00:02.0", which means domain 0000,
 * bus 00, device 02, function 0. The devices are numbered 0, 1, 2 ... in that
 * order. The list is read once, the first time it is needed; call rescan() to
 * read it again.
 *
 * Vendor and device IDs are numbers (for example 0x8086 is Intel). The class code
 * is 24 bits: base class, sub class and interface. See idLookup to turn these
 * numbers into names.
 */
public class pciInfo
{
    private static final Path ROOT = sysFile.path("/sys/bus/pci/devices");
    private static List<Path> devices;

    private static List<Path> list()
    {
        if (devices == null) {
            List<Path> found = new ArrayList<>();
            if (Files.isDirectory(ROOT)) {
                try (DirectoryStream<Path> dir = Files.newDirectoryStream(ROOT)) {
                    for (Path p : dir)
                        found.add(p);
                } catch (IOException e) {
                    // leave the list as it is
                }
            }
            Collections.sort(found);
            devices = found;
        }
        return devices;
    }

    /** Read the list of devices again (useful if hardware may have changed). */
    public static void rescan()
    {
        devices = null;
    }

    /** How many PCI devices there are. */
    public static int getCount()
    {
        return list().size();
    }

    /** The address as text, for example "0000:00:02.0". */
    public static String getAddress(int index)
    {
        return list().get(index).getFileName().toString();
    }

    /** The bus number, taken from the address. */
    public static int getBus(int index)
    {
        return Integer.parseInt(getAddress(index).substring(5, 7), 16);
    }

    /** The device number on the bus, taken from the address. */
    public static int getDevice(int index)
    {
        return Integer.parseInt(getAddress(index).substring(8, 10), 16);
    }

    /** The function number of the device, taken from the address. */
    public static int getFunction(int index)
    {
        return Integer.parseInt(getAddress(index).substring(11, 12), 16);
    }

    /** The vendor ID, for example 0x8086, or -1 if it cannot be read. */
    public static int getVendorId(int index)
    {
        return (int) sysFile.number(list().get(index).resolve("vendor"));
    }

    /** The device ID, or -1 if it cannot be read. */
    public static int getDeviceId(int index)
    {
        return (int) sysFile.number(list().get(index).resolve("device"));
    }

    /** The 24-bit class code, for example 0x060000 for a host bridge, or -1. */
    public static int getClassCode(int index)
    {
        return (int) sysFile.number(list().get(index).resolve("class"));
    }

    private static String[][] pcieDevices =
            new String[pciInfo.getCount()][6];

    // 0 - Address, 1 - Vendor ID, 2 - Device ID, 3 - Vendor Name, 4 - Device Name, 5 - Class
    private static boolean arrayBuilt = false;

    public static void arrayBuilderPCIe() throws IOException {
        idLookup pci = idLookup.pci();

        pcieDevices = new String[pciInfo.getCount()][7];

        for (int i = 0; i < pciInfo.getCount(); i++) {
            int vendor = pciInfo.getVendorId(i);
            int device = pciInfo.getDeviceId(i);

            pcieDevices[i][0] = pciInfo.getAddress(i);
            pcieDevices[i][1] = String.format("%04x", vendor);
            pcieDevices[i][2] = String.format("%04x", device);
            pcieDevices[i][3] = pci.vendorName(vendor);
            pcieDevices[i][4] = pci.deviceName(vendor, device);
            pcieDevices[i][5] = String.format("%06x", pciInfo.getClassCode(i));
            pcieDevices[i][6] = pci.className(pciInfo.getClassCode(i));
          
            if (pcieDevices[i][3] == null){
                pcieDevices[i][3] = "unkown vendor with id: " + (String.format("%04x", vendor));
            }
            if (pcieDevices[i][4] == null){
                pcieDevices[i][4] = "Unknown " +  pci.className(getClassCode(i)) + " (" + String.format("%04x", device) + ")";
            }
        }
        arrayBuilt = true;
    }

    private static void arrayBuiltChecker() throws IOException {
                if (arrayBuilt == false) {
                    arrayBuilderPCIe();
                }
                //System.out.println("Array built: " + arrayBuilt);
    }

    public static void sortByVendorName() throws IOException {
        arrayBuiltChecker();
        Arrays.sort(pcieDevices,Comparator.comparing(row -> row[3], Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
    }

    public static void sortByBusDeviceFunction() throws IOException {
        arrayBuiltChecker();
        Arrays.sort(pcieDevices, (a, b) -> {
            String[] addrA = a[0].split("[:.]");
            String[] addrB = b[0].split("[:.]");

            for (int i = 1; i <= 3; i++) {
                int numA = Integer.parseInt(addrA[i], 16);
                int numB = Integer.parseInt(addrB[i], 16);

                int result = Integer.compare(numA, numB);

                if (result != 0) {
                    return result;
                }
            }

            return 0;
        });
    }

    public static String[][] getDevices() throws IOException {
        arrayBuiltChecker();
        return pcieDevices;
    }

    public static void displayInfo() throws IOException {
        arrayBuiltChecker();
        for (int i = 0; i < pcieDevices.length; i++) {
            System.out.printf(
                "  %s  %s:%s  %s %s  [%s] %s%n",
                pcieDevices[i][0],
                pcieDevices[i][1],
                pcieDevices[i][2],
                pcieDevices[i][3],
                pcieDevices[i][4],
                pcieDevices[i][5],
                pcieDevices[i][6]
            );
        }
    }

    public static void displayUniqueVendors() throws IOException {
        arrayBuiltChecker();
        ArrayList<String> uniqueVendors = new ArrayList<>();
        ArrayList<Integer> quantity = new ArrayList<>();
        for (String[] device : pcieDevices) {
            String vendorName = device[3];
            if (!uniqueVendors.contains(vendorName)) {
                uniqueVendors.add(vendorName);
                quantity.add(1);
            } else {
                int index = uniqueVendors.indexOf(vendorName);
                quantity.set(index, quantity.get(index) + 1);
            }
        }

        for (int i = 0; i < uniqueVendors.size(); i++) {
            System.out.println("  " + uniqueVendors.get(i) + " (" + quantity.get(i) + ")");
        }
    }

    public static String getGPU() throws IOException {
        arrayBuiltChecker();
        StringBuilder GPU = new StringBuilder();
        idLookup pci = idLookup.pci();

        for (int i = 0; i < pcieDevices.length; i++) {
            if (pcieDevices[i][5].startsWith("03")) {
                GPU.append(String.format(
                    "  %s  %s:%s  %s %s  [%s] %s%n",
                    pcieDevices[i][0],
                    pcieDevices[i][1],
                    pcieDevices[i][2],
                    pcieDevices[i][3],
                    pcieDevices[i][4],
                    pcieDevices[i][5],
                    pcieDevices[i][6]
                ));
            }
        }

        return GPU.isEmpty() ? "No GPUs Found" : GPU.toString();
    }

    public static void displayUniqueClasses() throws IOException {
        arrayBuiltChecker();
        ArrayList<String> uniqueClasses = new ArrayList<>();
        ArrayList<String> classCodes = new ArrayList<>();
        ArrayList<Integer> quantity = new ArrayList<>();
        for (String[] device : pcieDevices) {
            String className = device[6];
            if (!uniqueClasses.contains(className)) {
                uniqueClasses.add(className);
                quantity.add(1);
                classCodes.add(device[5]);
            } else {
                int index = uniqueClasses.indexOf(className);
                quantity.set(index, quantity.get(index) + 1);
            }
        }

        System.out.println("Unique PCIe Classes:");
        for (int i = 0; i < uniqueClasses.size(); i++) {
            System.out.println("  " + uniqueClasses.get(i) + " (" + quantity.get(i) + ") [Code: " + classCodes.get(i) + "]");
        }
    }
}
