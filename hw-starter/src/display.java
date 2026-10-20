
// Class made by Eoin for displaying info

public class display {
    public static void main(String[] args) throws Exception { 

        basicDisplay();
        memDisplay();
        diskDisplay();
        cpuDisplay();
        pciDisplay();
        usbDisplay();
    }
   
    public static void basicDisplay() {
        System.out.println("----- System Info -----");
        System.out.println(" Cores: " + cpuInfo.getCount());
        System.out.println(" Model: " + cpuInfo.getModel());
    }

    public static void memDisplay() 
    {
        double memTotal = memInfo.getTotal() / 1048576;
        double memUsed = memInfo.getUsed() / 1048576;
        double memPer = (memUsed / memTotal) * 100;
        String ftTotal = String.format("%.2f", memTotal);
        String ftUsed = String.format("%.2f", memUsed);
        String ftPer = String.format("%.2f", memPer);

        System.out.println("----- Memory Info -----");
        if (memTotal < 0 || memUsed < 0 ) {
            System.out.println(" Memory not accessed!");
            return;
        }
        System.out.println(" Memory : Total = " + ftTotal + " GiB || Used = " + ftUsed + " GiB");
        System.out.println(" Percentage Used = " + ftPer + "%");
    }

    public static void cpuDisplay() 
    {
        System.out.println("----- CPU Info -----");

        double[] coreLoads = cpuInfo.fastLoad();
        for (int i = 0; i < coreLoads.length - 1; i++) {
            String ftload = String.format("%.2f", coreLoads[i]);
            String ftNum = String.format("%02d", i);
            System.out.println(" Core " + ftNum + " Load: " + ftload + "%");
        }

        String ftload = String.format("%.2f", coreLoads[coreLoads.length - 1]);
        System.out.println(" Average Load: " + ftload + "%");
    }

    public static void diskDisplay() 
    {
        String driveType = diskInfo.getDriveType();

        System.out.println("----- Disk Info -----");
        System.out.println(" Disk Type: " + driveType);
    }

    public static void pciDisplay() throws Exception {
        System.out.println("----- PCI Info -----");
        System.out.println(" PCI devices: " + pciInfo.getCount());

        pciInfo.arrayBuilderPCIe();
        System.out.println(" PCI devices sorted by vendor name:");
        pciInfo.sortByVendorName();
        pciInfo.displayInfo();

        System.out.println(" PCI devices sorted by bus, device and function:");
        pciInfo.sortByBusDeviceFunction();
        pciInfo.displayInfo();
        
        pciInfo.displayUniqueVendors();
    }
    

    public static void usbDisplay() throws Exception 
    {
        System.out.println("----- USB Info -----");

        idLookup usb = idLookup.usb();
        int usbCount = usbInfo.getCount();

        if (usbCount <= 0) {
            System.out.println(" No USB devices could be found.");
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
