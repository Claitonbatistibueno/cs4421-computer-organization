// Class made by Claiton for the HTML export (extension: presentation)
// Run: java -cp out display --html report.html

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Comparator;

public class htmlReport {

    // Flourish: Allow the HTML report to receive info from the hardware obj
public static void write(String fileName, hardwareData data) throws Exception {
    PrintWriter out = new PrintWriter(fileName);

        out.println("<!DOCTYPE html>");
        out.println("<html><head><meta charset=\"utf-8\">");
        out.println("<title>Hardware Inspector</title>");
        out.println("<style>");
        out.println("body { font-family: Arial, sans-serif; margin: 30px; color: #222; }");
        out.println("h2 { border-bottom: 2px solid #444; padding-bottom: 4px; }");
        out.println("table { border-collapse: collapse; margin-bottom: 20px; }");
        out.println("th, td { border: 1px solid #ccc; padding: 4px 10px; text-align: left; }");
        out.println("th { background: #eee; }");
        out.println(".bar { background: #4a90d9; height: 14px; }");
        out.println("</style></head><body>");
        out.println("<h1>Hardware Inspector</h1>");

// Flourish: Pass the collected CPU data to the HTML section
cpuSection(out, data.coreLoads);

// Flourish: Pass the collected memory data to the HTML section
memSection(out, data.memory[0], data.memory[1]);   
// Flourish: Pass the stored PCI devices to the HTML report
pciSection(out, data.pciDevices);  
// Flourish: Pass collected USB data to the HTML report
usbSection(out, data.usbDevices);
        out.println("</body></html>");
        out.close();

        System.out.println("HTML report saved to " + fileName);
    }
    // Flourish: overloading the write method to keep existing calls to htmlReport.write() working
public static void write(String fileName) throws Exception {
    hardwareData data = new hardwareData();
    write(fileName, data);
}

 // Flourish: Receive CPU load data instead of measuring it again
public static void cpuSection(PrintWriter out, double[] coreLoads) {
    int coreCount = coreLoads.length - 1;

        out.println("<h2>Processor</h2>");
        out.println("<p>Model: " + escape(cpuInfo.getModel()) + "<br>");
        out.println("Frequency: " + cpuInfo.getFrequency() + "<br>");
        out.println("Logical threads per package: " + cpuInfo.getLogicalThreads() + "<br>");
        out.println("Physical cores per package: " + cpuInfo.getPhysicalCores() + "<br>");
        out.println("Running in a virtual machine (hypervisor): " + cpuInfo.getHypervisor() + "<br>");
        out.println("Cores: " + cpuInfo.getCount() + "</p>");

        if (coreCount == 0) {   // no cores, so only the average slot exists
            out.println("<p>CPU load couldn't be measured (no cores found in /proc/cpuinfo).</p>");
            return;
        }

        out.println("<table><tr><th>Core</th><th>Load</th><th></th></tr>");
        for (int i = 0; i < coreCount; i++) {
            String load = String.format("%.0f", coreLoads[i]);
            out.println("<tr><td>Core " + i + "</td><td>" + load + "%</td>"
                    + "<td style=\"width:200px\"><div class=\"bar\" style=\"width:" + load + "%\"></div></td></tr>");
        }
        out.println("</table>");
        out.println("<p>Average load: " + String.format("%.2f", coreLoads[coreCount]) + "%</p>");
    }

       // Flourish: Receive memory data instead of reading it again
        public static void memSection(PrintWriter out, double memTotal, double memUsed) {
        double memPer = (memUsed / memTotal) * 100;
        
        out.println("<h2>Memory</h2>");
        // same check as the text report: -1 means /proc/meminfo could not be read
        if (memTotal <= 0 || memUsed < 0) {
            out.println("<p>Memory couldn't be accessed!</p>");
            return;
        }
        out.println("<table><tr><th>Total</th><th>Used</th><th>Percentage used</th></tr>");
        out.println("<tr><td>" + String.format("%.2f", memTotal) + " GiB</td><td>"
                + String.format("%.2f", memUsed) + " GiB</td><td>"
                + String.format("%.2f", memPer) + "%</td></tr>");
        out.println("</table>");
        // pie chart: the used part is memPer% of the circle (360 degrees), so memPer * 3.6 degrees
        double usedDeg = memPer * 3.6;
        out.println("<h3>Memory Chart</h3>");
        out.println("<div style=\"width:200px; height:200px; border-radius:50%; background: conic-gradient(red 0deg "
                + usedDeg + "deg, green " + usedDeg + "deg 360deg)\"></div>");
        out.println("<p>Red = Used<br> Green = Available</p>");
    }


   // Flourish: Receive collected PCI devices instead of reading them again
public static void pciSection(PrintWriter out, String[][] devices) throws Exception {
 out.println("<h2>PCI devices (" + devices.length + ")</h2>");

        if (devices.length == 0) {
            out.println("<p>No PCI devices could be found.</p>");
            return;
        }

// Flourish: Copy the collected data so sorting doesn't change stored data
String[][] sortedDevices = devices.clone();

// Sort by vendor name
Arrays.sort(sortedDevices,
        Comparator.comparing(row -> row[3],
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

out.println("<h3>Sorted by vendor name</h3>");
pciTable(out, sortedDevices);

// Sort by PCI address
Arrays.sort(sortedDevices, Comparator.comparing(row -> row[0]));

out.println("<h3>Sorted by bus, device and function</h3>");
pciTable(out, sortedDevices);

        out.println("<h3>Devices per vendor</h3>");
        countTable(out, devices, 3, "Vendor");
        out.println("<h3>Devices per class</h3>");
        countTable(out, devices, 6, "Class");   
     }

    // 0 - Address, 1 - Vendor ID, 2 - Device ID, 3 - Vendor Name, 4 - Device Name, 5 - Class code, 6 - Class name
    public static void pciTable(PrintWriter out, String[][] devices) {
        out.println("<table><tr><th>Address</th><th>Vendor:Device ID</th><th>Vendor</th>"
                + "<th>Device</th><th>Class</th></tr>");
        for (String[] d : devices) {
            out.println("<tr><td>" + d[0] + "</td><td>" + d[1] + ":" + d[2] + "</td><td>"
                    + escape(d[3]) + "</td><td>" + escape(d[4]) + "</td><td>"
                    + d[5] + " " + escape(d[6]) + "</td></tr>");
        }
        out.println("</table>");
    }

    // counts how many devices have the same value in one column (vendor or class)
    public static void countTable(PrintWriter out, String[][] devices, int column, String title) {
        String[] names = new String[devices.length];
        int[] counts = new int[devices.length];
        int found = 0;

        for (String[] d : devices) {
            int i = 0;
            while (i < found && !names[i].equals(d[column])) {
                i++;
            }
            if (i == found) {
                names[found] = d[column];
                found++;
            }
            counts[i]++;
        }

        out.println("<table><tr><th>" + title + "</th><th>Devices</th></tr>");
        for (int i = 0; i < found; i++) {
            out.println("<tr><td>" + escape(names[i]) + "</td><td>" + counts[i] + "</td></tr>");
        }
        out.println("</table>");
    }

    // 0 - Bus, 1 - Device, 2 - Vendor ID, 3 - Product ID, 4 - Vendor Name, 5 - Product Name
    // Flourish: Receive collected USB devices instead of reading them again
public static void usbSection(PrintWriter out, String[][] devices) {
out.println("<h2>USB devices (" + devices.length + ")</h2>");
if (devices.length == 0) {
                out.println("<p>No USB devices could be found.</p>");
            return;
        }

        out.println("<table><tr><th>Bus</th><th>Device</th><th>Vendor:Product ID</th>"
                + "<th>Vendor</th><th>Product</th></tr>");
// Flourish: Use stored USB data instead of collecting it again
for (String[] d : devices) {
                out.println("<tr><td>" + d[0] + "</td><td>" + d[1] + "</td><td>" + d[2] + ":" + d[3]
                    + "</td><td>" + escape(d[4]) + "</td><td>" + escape(d[5]) + "</td></tr>");
        }
        out.println("</table>");
    }

    // names from pci.ids can have & < > so we change them, otherwise the page breaks
    public static String escape(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
