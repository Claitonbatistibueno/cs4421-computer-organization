
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


        pciInfo.arrayBuilderPCIe();

        System.out.println("PCI devices sorted by vendor name:");
        pciInfo.sortByVendorName();
        pciInfo.displayInfo();

        System.out.println("PCI devices sorted by bus, device and function:");
        pciInfo.sortByBusDeviceFunction();
        pciInfo.displayInfo();

        idLookup usb = idLookup.usb();
        System.out.println("USB devices: " + usbInfo.getCount());

        usbInfo.arrayBuilderUSB();
        System.out.println("USB devices:");
        usbInfo.displayUSBInfo();

        pciInfo.displayUniqueVendors();

        System.out.println("CPU Load per core:");
        for (int i = 0; i < cpuInfo.getCount(); i++) {
            double load = cpuInfo.perCoreLoad(i);
            System.out.printf("  Core %d: %.2f%%%n", (i + 1), load);
        }   
        System.out.printf("Total CPU Load: %.2f%%%n", cpuInfo.allCoresLoad());
    }
}

class PCIeDevices {
    
}