
// Class made by Eoin for displaying info

public class display {
    public static void main(String[] args) throws Exception { 
        basicDisplay();
    }
   
    public static void basicDisplay() throws Exception{
        System.out.println("==== System Info ====");
        System.out.println("Model: " + cpuInfo.getModel());
        System.out.println("Cores: " + cpuInfo.getCount());
        memDisplay();
        cpuDisplay();
        pciDisplay();
        usbDisplay();
    }

    public static void memDisplay() {
        double memTotal = memInfo.getTotal() / 1048576;
        double memUsed = memInfo.getUsed() / 1048576;
        double memPer = (memUsed / memTotal) * 100;
        String ftTotal = String.format("%.2f", memTotal);
        String ftUsed = String.format("%.2f", memUsed);
        String ftPer = String.format("%.2f", memPer);

        if (memTotal < 0 || memUsed < 0 ) {
            System.out.println("Memory couldn't be accessed!");
            return;
        }
        System.out.printf("%-15s %-15s %-15s %n", "Memory Total", "Memory Used", "Percentage Used");
        System.out.printf("%-15s %-15s %-15s %n", ftTotal + " GiB", ftUsed + " Gib", ftPer + "%");
    }

    public static void cpuDisplay() {

        String ftLine = "%-10s %-15s %n";
        
        System.out.printf(ftLine, "Core (s)", "Load");

        double[] coreLoads = cpuInfo.fastLoad();
        for (int i = 0; i < coreLoads.length - 1; i++) {
            String ftLoad = String.format("%.2f", coreLoads[i]);
            System.out.printf(ftLine, " " + i, ftLoad + "%");
        }
        
        String ftLoad = String.format("%.2f", coreLoads[coreLoads.length - 1]);

        System.out.printf(ftLine, " Average", ftLoad +"%");
    }

    public static void diskDisplay() {
        System.out.println("----- Disk Info -----");

        String driveType = diskInfo.getDriveType();
        System.out.println(" Disk Type: " + driveType);
    }

    public static void pciDisplay() throws Exception {
        System.out.println("PCI devices: " + pciInfo.getCount());

        pciInfo.arrayBuilderPCIe();
        System.out.println("PCI devices sorted by vendor name:");
        pciInfo.sortByVendorName();
        pciInfo.displayInfo();

        System.out.println("PCI devices sorted by bus, device and function:");
        pciInfo.sortByBusDeviceFunction();
        pciInfo.displayInfo();
        
        System.out.println("Unique PCIe Vendors:");
        pciInfo.displayUniqueVendors();
    }
    
    public static void usbDisplay() throws Exception {
        idLookup usb = idLookup.usb();
        int usbCount = usbInfo.getCount();

        if (usbCount <= 0) {
            System.out.println("No USB devices could be found.");
            return;
        }

        System.out.println("USB devices: " + usbInfo.getCount());
        for (int i = 0; i < usbInfo.getCount(); i++) {
            int vendor = usbInfo.getVendorId(i);
            int product = usbInfo.getProductId(i);

            System.out.printf("  Bus %03d Device %03d: ID %04x:%04x  %s %s%n", usbInfo.getBusNumber(i),
                              usbInfo.getDeviceNumber(i), vendor, product,
                              usb.vendorName(vendor), usb.deviceName(vendor, product));
        }
    }
}
