// Class made by Eoin for displaying info
import java.util.Arrays;
import java.util.Comparator;
import java.util.Scanner;

public class display {

    public static hardwareData data;

    public static void main(String[] args) throws Exception {
    // Generate HTML report directly

        data = new hardwareData();

        if (args.length == 2 && args[0].equals("--html")) {
            htmlReport.write(args[1], data);
            return;
        }

        // Generate airport report directly
        if (args.length == 2 && args[0].equals("--airport")) {
            airportReport.write(args[1]);
            return;
        }

        // Display system information and run benchmarks
        if (args.length > 0 && args[0].equalsIgnoreCase("all")) {
            hardwareData data = new hardwareData();
            parallelBench.main(null);
            return;
        }

        // Show interactive menu
        interact();

        }

    public static void interact() {

        Scanner input = new Scanner(System.in);

        while (true) {
            System.out.println("==== Options ====");
            System.out.println("1. System Stats");
            System.out.println("2. Parallelism & Verdict");
            System.out.println("3. Export HTML (report and airport page)");
            //from eoins review to create an option in display to reset hardwareData and collect more data.
            System.out.println("4. Refresh Hardware Data");
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
                    clear();
                    basicDisplay(data);

                } else if (choice == 2) {
                    clear();
                    parallelBench.main(null);

                } else if (choice == 3) {
                    clear();
                    // Reuse the existing hardware data
                    htmlReport.write("report.html", data);
                    airportReport.write("airport.html");

                } else if (choice == 4) {
                    clear();
                    data = new hardwareData();
                    System.out.println("Hardware data refreshed!");

                } else if (choice == 5) {
                    System.out.println("Bye!");
                    return;

                } else {
                    System.out.println("Invalid Selection, try again.");
                }
            } catch (Exception e) {
                System.out.println("An error occurred: " + e.getMessage());
                e.printStackTrace(); //Flourish: tells us exactly where the error occured
            }
        }
    }
   // accept the object to gurantee identical results beyween terminsl and html file
    public static void basicDisplay(hardwareData data) throws Exception {

        System.out.println("==== System Info ====");
        System.out.println("Model: " + cpuInfo.getModel());
        System.out.println("Frequency: " + cpuInfo.getFrequency());
        System.out.println("Logical threads per package: " + cpuInfo.getLogicalThreads());
        System.out.println("Physical cores per package: " + cpuInfo.getPhysicalCores());
        System.out.println("Running in a virtual machine (hypervisor): " + cpuInfo.getHypervisor());
        diskDisplay();

// Flourish: Use memory info stored in the obj
memDisplay(data.memory[0], data.memory[1]);
// Flourish: Use CPU info stored in obj
cpuDisplay(data.coreLoads);
// Flourish: Use PCI devices info stored in obj
pciDisplay(data.pciDevices);
// Flourish: use collected USB data stored in obj
usbDisplay(data.usbDevices);
    }
    //Flourish: receives the values as params
    public static void memDisplay(double memTotal, double memUsed) {
        double memPer = (memUsed / memTotal) * 100;
        String ftTotal = String.format("%.2f", memTotal);
        String ftUsed = String.format("%.2f", memUsed);
        String ftPer = String.format("%.2f", memPer);

        if (memTotal <= 0 || memUsed < 0 ) {
            System.out.println("Memory couldn't be accessed!");
            return;
        }
        System.out.printf("%n%-15s %-15s %-15s %n", "Memory Total", "Memory Used", "Percentage Used");
        System.out.printf("%-15s %-15s %-15s %n", ftTotal + " GiB", ftUsed + " GiB", ftPer + "%");
    }

       // Flourish: Receive CPU load data instead of measuring it here
    public static void cpuDisplay(double[] coreLoads) {
        int coreCount = coreLoads.length - 1;

        System.out.printf("%n%s%n","==== CPU ====");
        System.out.println("No. of Cores: " + coreCount);
        if (coreCount == 0) {
            System.out.println("CPU load couldn't be measured (no cores found in /proc/cpuinfo).");
            System.out.println();
        } else {
            System.out.printf("%-10s %s%n", "Core", "Load");
            for (int i = 0; i < coreCount; i++) {
                System.out.printf("%-10d %.2f%%%n", i, coreLoads[i]);
            }
            System.out.printf("%-10s %.2f%%%n%n", "Average", coreLoads[coreCount]);
        }
        // the benchmark takes a few seconds the first time, then the result is kept
        System.out.println("Benchmarking CPU (SHA-256 and zip on 16 MiB of data, a few seconds the first time)...");
        try {
            System.out.printf(" Raw score: %.2f MiB/s%n", cpuBench.getRawScore());
            System.out.printf(" CPU Score: %.2f (1.00 = reference laptop)%n", cpuBench.getScore());
        } catch (Exception e) {
            System.out.println("Error, the CPU benchmark could not run.");
        }
        
    }

    public static void diskDisplay() {
        String diskName = diskInfo.getDiskName();
        if (diskName.isEmpty()) {
            System.out.println("Disk Type: unknown (no disk found in /sys/block)");
            return;
        }
        System.out.println("Disk Type: " + diskInfo.getDriveType() + " (" + diskName + ")");
    }
// Flourish: Display PCI devices using already collected data
    public static void pciDisplay(String[][] devices) {

        System.out.printf("%n%s%n", "==== PCIe Devices ====");
        System.out.println("PCI devices: " + devices.length);

        if (devices.length == 0) {
            System.out.println("No PCI devices could be found.");
            return;
        }

        // Copy the collected data so sorting doesn't change the original
        String[][] sortedDevices = devices.clone();

        // Sort by vendor name
        Arrays.sort(sortedDevices,
            Comparator.comparing(row -> row[3],
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

        System.out.println("PCI devices sorted by vendor name:");
        printPciDevices(sortedDevices);

        // Sort by bus, device and function
        Arrays.sort(sortedDevices, Comparator.comparing(row -> row[0]));

        System.out.println("PCI devices sorted by bus, device and function:");
        printPciDevices(sortedDevices);

        // Count devices by vendor and class
        System.out.println("Unique PCIe Vendors:");
        printPciCounts(devices, 3, false);

        System.out.println("Unique PCIe Classes:");
        printPciCounts(devices, 6, true);
}
// Flourish: Print PCI device details from collected data
    public static void printPciDevices(String[][] devices) {

        for (String[] device : devices) {
            System.out.printf(
                "  %s  %s:%s  %s %s  [%s] %s%n",
                device[0],
                device[1],
                device[2],
                device[3],
                device[4],
                device[5],
                device[6]
        );
    }
}
// Flourish: Count PCI devices by vendor or class
    public static void printPciCounts(String[][] devices, int column, boolean showCode) {

        java.util.LinkedHashMap<String, Integer> counts =
            new java.util.LinkedHashMap<>();

        java.util.LinkedHashMap<String, String> codes =
            new java.util.LinkedHashMap<>();

        for (String[] device : devices) {
            String name = device[column];

            counts.put(name, counts.getOrDefault(name, 0) + 1);

            if (showCode) {
                codes.putIfAbsent(name, device[5]);
            }
        }

        for (String name : counts.keySet()) {
            System.out.print("  " + name + " (" + counts.get(name) + ")");

            if (showCode) {
                System.out.print(" [Code: " + codes.get(name) + "]");
            }

            System.out.println();
    }
}
    
    // Flourish: Display USB devices using collected data
    public static void usbDisplay(String[][] devices) {

        System.out.printf("%n%s%n", "==== USB ====");

        if (devices.length == 0) {
            System.out.println("No USB devices could be found.");
            return;
        }

        System.out.println("USB devices: " + devices.length);

        for (String[] device : devices) {
            System.out.printf(
                "  Bus %s Device %s: ID %s:%s  %s %s%n",
                device[0],
                device[1],
                device[2],
                device[3],
                device[4],
                device[5]
        );
    }
}

    public static void clear() { 
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }  
}
