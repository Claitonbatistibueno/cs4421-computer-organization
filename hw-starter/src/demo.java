import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import javax.swing.JFrame;
import java.io.IOException;
import java.util.ArrayList;

public class demo {
    public static void main(String[] args) throws Exception { 
        System.out.println("Cores: " + cpuInfo.getCount());
        System.out.println("Model: " + cpuInfo.getModel());
        System.out.println("core 0: busy=" + cpuInfo.getBusy(0)
                + " idle=" + cpuInfo.getIdle(0));
        System.out.println("memory: total=" + memInfo.getTotal()
                + " KiB, used=" + memInfo.getUsed() + " KiB");

        //Eoin's methods
        // memDisplay();
        // cpuDisplay();
        // frame();
        diskDisplay();

        System.out.println("PCI devices: " + pciInfo.getCount());

        pciInfo.arrayBuilderPCIe();

        System.out.println("PCI devices sorted by vendor name:");
        pciInfo.sortByVendorName();
        pciInfo.displayInfo();

        System.out.println("PCI devices sorted by bus, device and function:");
        pciInfo.sortByBusDeviceFunction();
        pciInfo.displayInfo();
        System.out.println("GPU: ");
        System.out.println(pciInfo.getGPU());

        System.out.println("USB devices: " + usbInfo.getCount());

        usbInfo.arrayBuilderUSB();
        System.out.println("USB devices:");
        usbInfo.displayUSBInfo();

        pciInfo.displayUniqueVendors();
        pciInfo.displayUniqueClasses();

        System.out.println("CPU Load per core:");
        for (int i = 0; i < cpuInfo.getCount(); i++) {
            double load = cpuInfo.perCoreLoad(i);
            System.out.printf("  Core %d: %.2f%%%n", (i + 1), load);
        }   
        System.out.printf("Total CPU Load: %.2f%%%n", cpuInfo.allCoresLoad());
    }

    public static void cpuDisplay() throws Exception //Gets the busy & idle time intervals for all cores.
    {
        System.out.println("----- CPU Info -----");

        int coreCount = cpuInfo.getCount();
        int coreBusyStart[] = new int[coreCount];
        int coreIdleStart[] = new int[coreCount];
        double averageLoad = 0;

        for (int i = 0; i < coreCount; i++) {
            coreBusyStart[i] = (int) cpuInfo.getBusy(i);
            coreIdleStart[i] = (int) cpuInfo.getIdle(i);
        }

        Thread.sleep(1000); // waits for 1 second

        for (int i = 0; i < coreCount; i++) {
            double busy = ( cpuInfo.getBusy(i) + coreBusyStart[i] ) /2;
            double idle = ( cpuInfo.getIdle(i) + coreIdleStart[i] ) /2;
            double load = 100.0 * busy / (busy + idle);
            String ftNum = String.format("%02d", i);
            String ftLoad = String.format("%.2f", load);

            System.out.print("Core " + ftNum + ": ");
            System.out.println("Load = " + ftLoad + "%");
            averageLoad += load;
        }
        averageLoad /= coreCount;
        String ftLoad = String.format("%.2f", averageLoad);
        System.out.println("Average Load = " + ftLoad + "%");
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

    public static void diskDisplay() 
    {
        String driveType = diskInfo.getDriveType();

        System.out.println("----- Disk Info -----");
        System.out.println(" Disk Type: " + driveType);
    }

    public static void usbDisplay() throws Exception 
    {

        idLookup usb = idLookup.usb();
        int usbCount = usbInfo.getCount();


    System.out.println("----- USB Info -----");
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

    public static void Mark() throws Exception // Method provided by Mark
    {
        System.out.println("----- System Info -----");
        System.out.println("Cores:  " + cpuInfo.getCount());
        System.out.println("Model:  " + cpuInfo.getModel());
        System.out.println("Core 0: busy=" + cpuInfo.getBusy(0) + " idle=" + cpuInfo.getIdle(0));
        System.out.println("Memory: total=" + memInfo.getTotal() + " KiB, used=" + memInfo.getUsed() + " KiB");

        idLookup pci = idLookup.pci();
        System.out.println("PCI devices: " + pciInfo.getCount());
        for (int i = 0; i < pciInfo.getCount(); i++) {
            int vendor = pciInfo.getVendorId(i);
            int device = pciInfo.getDeviceId(i);
            System.out.printf("  %s  %04x:%04x  %s %s%n", pciInfo.getAddress(i), vendor, device,
                              pci.vendorName(vendor), pci.deviceName(vendor, device));
        }

        idLookup usb = idLookup.usb();
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
