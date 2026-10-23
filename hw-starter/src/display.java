// Class made by Eoin for displaying info

import java.util.Scanner;

public class display {
    public static void main(String[] args) { 
        interact();
    }

    public static void interact() {
        Scanner input = new Scanner(System.in);

        while (true) {
            System.out.println("----- Choice -----");
            System.out.println("1. System Stats");
            System.out.println("2. Parallel Bench");
            System.out.println("3. Verdict");
            System.out.println("4.");
            System.out.println("5. Exit");
            System.out.print("->");

            if (!input.hasNext()) {
                return;
            }
            String choiceString = input.next();
            int choice;

            try {
                choice = Integer.parseInt(choiceString);
            } catch (Exception e) {
                System.out.println("Invalid Selection, try again.");
                continue;
            }

            try {
                if (choice == 1) {
                    basicDisplay();
                }
                else if (choice == 2) {
                    parallelBench.main(null);
                }
                else if (choice == 3) {
                    if (parallelBench.bestSpeedup == 0) {
                        System.out.println("Running the benchmark first to measure the speedup...");
                        parallelBench.main(null);
                    } else {
                        verdict.print(parallelBench.bestSpeedup);
                    }
                }
                else if (choice == 5) {
                    System.out.println("Bye!");
                    return;
                }
                else {
                    System.out.println("Invalid Selection, try again.");
                }
            } catch (Exception e) {
                System.out.println("An error occured, try again!");
            }
        }
    }
   
    public static void basicDisplay() throws Exception{
        System.out.println("==== System Info ====");
        System.out.println("Model: " + cpuInfo.getModel());
        memDisplay();
        diskDisplay();
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
        double[] coreLoads = cpuInfo.fastLoad();
        String ftLoads = "";

        for (int i = 0; i < coreLoads.length -1; i++) {
            String ftLoad = String.format("%.0f", coreLoads[i]);
            ftLoads += "   " + ftLoad + "%"; 
        }

        String ftLoad = String.format("%.2f", coreLoads[coreLoads.length - 1]);

        System.out.println();
        System.out.println("No. of Cores: " + cpuInfo.getCount());
        System.out.println("Load per Core: ");
        System.out.println(ftLoads);
        System.out.println("Average Load:");
        System.out.println("   " + ftLoad + "%");
        System.out.println();
    }  

    public static void diskDisplay() {
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

        try {
            usb = idLookup.usb();
        } catch (Exception e) {
            System.out.println("Error, couldn't look up usb devices!");
            return;
        }

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

    public static void clear() {  
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }  
}
