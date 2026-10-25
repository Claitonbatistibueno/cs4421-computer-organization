// Class made by Eoin for displaying info

import java.util.Scanner;

public class display {
    public static void main(String[] args) throws Exception { 
        if (args.length == 2 && args[0].equals("--html")) {
            htmlReport.write(args[1]);
            return;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("all")) {
            basicDisplay();
            parallelBench.main(null);
            return;
        }
        interact();
    }

    public static void interact() {

        Scanner input = new Scanner(System.in);

        while (true) {
            System.out.println("==== Options ====");
            System.out.println("1. System Stats");
            System.out.println("2. Parallelism & Verdict");
            System.out.println("3. Export HTML");
            System.out.println("4. Exit");
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
                    clear();
                    basicDisplay();
                } else if (choice == 2) {
                    clear();
                    parallelBench.main(null);
                }  else if (choice == 3) {
                    clear();
                    htmlReport.write("report.html");
                } else if (choice == 4) {
                    System.out.println("Bye!");
                    return;
                } else {
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
        System.out.println("Frequency: " + cpuInfo.getFrequency());
        System.out.println("Logical threads per package: " + cpuInfo.getLogicalThreads());
        System.out.println("Physical cores per package: " + cpuInfo.getPhysicalCores());
        System.out.println("Are using a hypervisor ? " + cpuInfo.getHypervisor());
        diskDisplay();
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
        System.out.printf("%n%-15s %-15s %-15s %n", "Memory Total", "Memory Used", "Percentage Used");
        System.out.printf("%-15s %-15s %-15s %n", ftTotal + " GiB", ftUsed + " GiB", ftPer + "%");
    }

    public static void cpuDisplay() {
        double[] coreLoads = cpuInfo.fastLoad();
        int coreCount = cpuInfo.getCount();   // coreLoads[coreCount] is the average load

        System.out.printf("%n%s%n","==== CPU ====");
        System.out.println("No. of Cores: " + coreCount);
        System.out.printf("%-10s %s%n", "Core", "Load");
        for (int i = 0; i < coreCount; i++) {
            System.out.printf("%-10d %.2f%%%n", i, coreLoads[i]);
        }
        System.out.printf("%-10s %.2f%%%n%n", "Average", coreLoads[coreCount]);
        System.out.println("Benchmarking CPU:");
        try {
            System.out.printf(" Raw score: %.2f%n", cpuBench.getRawScore());
            System.out.printf(" CPU Score: %.2f%n", cpuBench.getScore());
        } catch (Exception e) {
            System.out.println("Error!");
        }
        
    }

    public static void diskDisplay() {
        String driveType = diskInfo.getDriveType();
        System.out.println("Disk Type: " + driveType);
    }

    public static void pciDisplay() {
        System.out.printf("%n%s%n","==== PCIe Devices ====");
        System.out.println("PCI devices: " + pciInfo.getCount());

        try {
            pciInfo.arrayBuilderPCIe();
            System.out.println("PCI devices sorted by vendor name:");
            pciInfo.sortByVendorName();
            pciInfo.displayInfo();

            System.out.println("PCI devices sorted by bus, device and function:");
            pciInfo.sortByBusDeviceFunction();
            pciInfo.displayInfo();
        
            System.out.println("Unique PCIe Vendors:");
            pciInfo.displayUniqueVendors();
        } catch (Exception e) {
            System.out.println("Error, couldn't find PCIe devices.");
        }
        
    }
    
    public static void usbDisplay() throws Exception {
        idLookup usb = idLookup.usb();
        int usbCount = usbInfo.getCount();

        System.out.printf("%n%s%n","==== USB ====");
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
