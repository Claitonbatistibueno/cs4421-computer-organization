/**
 * A short example that shows how to call each provided class. It prints raw
 * numbers and a few names. It is NOT a solution to the assignment: you still
 * have to measure load, store the devices, sort them, analyse them and present
 * the results.
 *
 * Build:  javac *.java
 * Run:    java demo        (run it from the folder that contains the ids folder)
 */
public class demo
{
    public static void main(String[] args) throws Exception
    {
        printInfo(); // Method provided by Mark
        cpuBusyIdle();
    }

    public static void printInfo() throws Exception
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

    public static void cpuBusyIdle() throws Exception //Gets the busy & idle time intervals for all cores.
    {
        int coreCount = cpuInfo.getCount();
        int coreBusy0[] = new int[coreCount];
        int coreIdle0[] = new int[coreCount];
        int coreBusy1[] = new int[coreCount];
        int coreIdle1[] = new int[coreCount];

        for (int i = 0; i < coreCount; i++) {
            coreBusy0[i] = (int) cpuInfo.getBusy(i);
            coreIdle0[i] = (int) cpuInfo.getIdle(i);
        }

        Thread.sleep(1000); // waits for 1 second

        for (int i = 0; i < coreCount; i++) {
            coreBusy1[i] = (int) cpuInfo.getBusy(i);
            coreIdle1[i] = (int) cpuInfo.getIdle(i);
            System.out.print("Core " + (i) + " : ");
            System.out.print("Busy = " + (coreBusy0[i] - coreBusy1[i]) + " || ");
            System.out.println("Idle = " + (coreIdle0[i] - coreIdle1[i]) );
        }
    }
}
