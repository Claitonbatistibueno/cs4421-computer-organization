
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.io.IOException;
import java.util.ArrayList;

public class demo {
    public static void main(String[] args) throws Exception {
        System.out.println("cores:  " + cpuInfo.getCount());
        System.out.println("model:  " + cpuInfo.getModel());
        System.out.println("core 0: busy=" + cpuInfo.getBusy(0)
                + " idle=" + cpuInfo.getIdle(0));

        System.out.println("memory: total=" + memInfo.getTotal()
                + " KiB, used=" + memInfo.getUsed() + " KiB");

        System.out.printf("Percentage of RAM used: %.2f%%%n", (double) memInfo.getUsed() / memInfo.getTotal() * 100);

        idLookup pci = idLookup.pci();
        System.out.println("PCI devices: " + pciInfo.getCount());


        PCIeDevices.arrayBuilderPCIe();

        System.out.println("PCI devices sorted by vendor name:");
        PCIeDevices.sortByVendorName();
        PCIeDevices.displayInfo();

        System.out.println("PCI devices sorted by bus, device and function:");
        PCIeDevices.sortByBusDeviceFunction();
        PCIeDevices.displayInfo();

        idLookup usb = idLookup.usb();
        System.out.println("USB devices: " + usbInfo.getCount());

        USBdevices.arrayBuilderUSB();
        System.out.println("USB devices:");
        USBdevices.displayUSBInfo();

        PCIeDevices.displayUniqueVendors();

        System.out.println("CPU Load per core:");
        for (int i = 0; i < cpuInfo.getCount(); i++) {
            double load = ProcesserLoad.perCoreLoad(i);
            System.out.printf("  Core %d: %.2f%%%n", (i + 1), load);
        }   
        System.out.printf("Total CPU Load: %.2f%%%n", ProcesserLoad.allCoresLoad());
    }
}

class PCIeDevices {
    private static String[][] pcieDevices =
            new String[pciInfo.getCount()][5];

    // 0 - Address, 1 - Vendor ID, 2 - Device ID, 3 - Vendor Name, 4 - Device Name

    public static void arrayBuilderPCIe() throws IOException {
        idLookup pci = idLookup.pci();

        pcieDevices = new String[pciInfo.getCount()][5];

        for (int i = 0; i < pciInfo.getCount(); i++) {
            int vendor = pciInfo.getVendorId(i);
            int device = pciInfo.getDeviceId(i);

            pcieDevices[i][0] = pciInfo.getAddress(i);
            pcieDevices[i][1] = String.format("%04x", vendor);
            pcieDevices[i][2] = String.format("%04x", device);
            pcieDevices[i][3] = pci.vendorName(vendor);
            pcieDevices[i][4] = pci.deviceName(vendor, device);
        }
    }

    public static void sortByVendorName() {
        Arrays.sort(pcieDevices,Comparator.comparing(row -> row[3], Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
    }

    public static void sortByBusDeviceFunction() {
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

    public static void displayInfo() {
        for (int i = 0; i < pcieDevices.length; i++) {
            System.out.printf(
                "  %s  %s:%s  %s %s%n",
                pcieDevices[i][0],
                pcieDevices[i][1],
                pcieDevices[i][2],
                pcieDevices[i][3],
                pcieDevices[i][4]
            );
        }
    }

    public static void displayUniqueVendors() {
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

        System.out.println("Unique PCIe Vendors:");
        for (int i = 0; i < uniqueVendors.size(); i++) {
            System.out.println("  " + uniqueVendors.get(i) + " (" + quantity.get(i) + ")");
        }
    }
}

class ProcesserLoad{
    public static double perCoreLoad(int core){
        try {
            long busy = cpuInfo.getBusy(core);
            long idle = cpuInfo.getIdle(core);
            TimeUnit.SECONDS.sleep(1); 
            busy = (cpuInfo.getBusy(core) + busy)/2;
            idle = (cpuInfo.getIdle(core) + idle)/2;
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
}

class USBdevices{
    private static String[][] usbDevices = new String[usbInfo.getCount()][6];
    // 0 - Bus Number, 1 - Device Number, 2 - Vendor ID, 3 - Product ID, 4 - Vendor Name, 5 - Product Name
    public static void arrayBuilderUSB() throws IOException {
        idLookup usb = idLookup.usb();

        usbDevices = new String[usbInfo.getCount()][6];

        for (int i = 0; i < usbInfo.getCount(); i++) {
            int vendor = usbInfo.getVendorId(i);
            int product = usbInfo.getProductId(i);

            usbDevices[i][0] = String.format("%03d", usbInfo.getBusNumber(i));
            usbDevices[i][1] = String.format("%03d", usbInfo.getDeviceNumber(i));
            usbDevices[i][2] = String.format("%04x", vendor);
            usbDevices[i][3] = String.format("%04x", product);
            usbDevices[i][4] = usb.vendorName(vendor);
            usbDevices[i][5] = usb.deviceName(vendor, product);
        }
    }        
    
    public static void displayUSBInfo() {
        for (int i = 0; i < usbDevices.length; i++) {
            System.out.printf(
                "  Bus %s Device %s: ID %s:%s  %s %s%n",
                usbDevices[i][0],
                usbDevices[i][1],
                usbDevices[i][2],
                usbDevices[i][3],
                usbDevices[i][4],
                usbDevices[i][5]
            );
        }
    }
}