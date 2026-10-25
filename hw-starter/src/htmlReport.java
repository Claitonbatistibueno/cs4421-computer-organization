// Class made by Claiton for the HTML export (extension: presentation)
// Run: java -cp out display --html report.html

import java.io.PrintWriter;

public class htmlReport {

    public static void write(String fileName) throws Exception {
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

        cpuSection(out);
        memSection(out);
        pciSection(out);
        usbSection(out);

        out.println("</body></html>");
        out.close();

        System.out.println("HTML report saved to " + fileName);
    }

    public static void cpuSection(PrintWriter out) {
        double[] coreLoads = cpuInfo.fastLoad();
        int last = coreLoads.length - 1;   // the last value is the average load

        out.println("<h2>Processor</h2>");
        out.println("<p>Model: " + escape(cpuInfo.getModel()) + "<br>");
        out.println("Frequency: " + cpuInfo.getFrequency() + "<br>");
        out.println("Logical threads per package: " + cpuInfo.getLogicalThreads() + "<br>");
        out.println("Physical cores per package: " + cpuInfo.getPhysicalCores() + "<br>");
        out.println("Are using a hypervisor ? " + cpuInfo.getHypervisor() + "<br>");
        out.println("Cores: " + cpuInfo.getCount() + "</p>");

        out.println("<table><tr><th>Core</th><th>Load</th><th></th></tr>");
        for (int i = 0; i < last; i++) {
            String load = String.format("%.0f", coreLoads[i]);
            out.println("<tr><td>Core " + i + "</td><td>" + load + "%</td>"
                    + "<td style=\"width:200px\"><div class=\"bar\" style=\"width:" + load + "%\"></div></td></tr>");
        }
        out.println("</table>");
        out.println("<p>Average load: " + String.format("%.2f", coreLoads[last]) + "%</p>");
    }

    public static void memSection(PrintWriter out) {
        double memTotal = memInfo.getTotal() / 1048576;
        double memUsed = memInfo.getUsed() / 1048576;
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

    public static void pciSection(PrintWriter out) throws Exception {
        out.println("<h2>PCI devices (" + pciInfo.getCount() + ")</h2>");

        if (pciInfo.getCount() == 0) {
            out.println("<p>No PCI devices could be found.</p>");
            return;
        }

        pciInfo.sortByVendorName();
        out.println("<h3>Sorted by vendor name</h3>");
        pciTable(out, pciInfo.getDevices());

        pciInfo.sortByBusDeviceFunction();
        out.println("<h3>Sorted by bus, device and function</h3>");
        pciTable(out, pciInfo.getDevices());

        out.println("<h3>Devices per vendor</h3>");
        countTable(out, pciInfo.getDevices(), 3, "Vendor");

        out.println("<h3>Devices per class</h3>");
        countTable(out, pciInfo.getDevices(), 6, "Class");
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
    public static void usbSection(PrintWriter out) throws Exception {
        out.println("<h2>USB devices (" + usbInfo.getCount() + ")</h2>");

        if (usbInfo.getCount() <= 0) {
            out.println("<p>No USB devices could be found.</p>");
            return;
        }

        out.println("<table><tr><th>Bus</th><th>Device</th><th>Vendor:Product ID</th>"
                + "<th>Vendor</th><th>Product</th></tr>");
        for (String[] d : usbInfo.getDevices()) {
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
