// Class made by Claiton for the airport page (extension: presentation)
// It reads web/airport-template.html, replaces every marker such as {{CORES}} with a value
// read from this machine, and saves the result as a new page.
// Run: java -cp out display --airport airport.html

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class airportReport {

    static final Path TEMPLATE = Path.of("web", "airport-template.html");

    public static void write(String fileName) throws Exception {
        if (!Files.isReadable(TEMPLATE)) {
            System.out.println("Airport page not saved: cannot find " + TEMPLATE + " (run from the hw-starter folder).");
            return;
        }
        String page = Files.readString(TEMPLATE);

        // ---- header
        String host = sysFile.text(sysFile.path("/proc/sys/kernel/hostname"));
        if (host.isEmpty()) host = "Linux";
        page = page.replace("{{HOST}}", htmlReport.escape(host));
        page = page.replace("{{CODE}}", airportCode(host));
        page = page.replace("{{DATE}}", LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("d MMM yyyy 'at' HH:mm", Locale.UK)));

        // ---- 8 runways and 7 control tower (processor)
        int cores = cpuInfo.getCount();
        page = page.replace("{{MODEL}}", htmlReport.escape(cpuInfo.getModel()));
        page = page.replace("{{CORES}}", String.valueOf(cores));
        page = page.replace("{{FREQ}}", htmlReport.escape(cpuInfo.getFrequency()));
        page = page.replace("{{PHYSICAL}}", htmlReport.escape(cpuInfo.getPhysicalCores()));
        page = page.replace("{{LOGICAL}}", htmlReport.escape(cpuInfo.getLogicalThreads()));
        page = page.replace("{{SMT}}", htmlReport.escape(cpuInfo.getSmt()));
        page = cpuParts(page, cores);

        // ---- 4 departure lounge (memory)
        page = memoryParts(page);

        // ---- 1 car park (disk)
        String disk = diskInfo.getDriveType();
        String diskName = diskInfo.getDiskName();
        page = page.replace("{{DISK}}", diskName.isEmpty() ? disk : disk + " (" + diskName + ")");

        // ---- 2 taxi rank (USB)
        page = page.replace("{{USB_COUNT}}", String.valueOf(usbInfo.getCount()));
        page = page.replace("{{USB_LIST}}", usbList());

        // ---- 3 security (Amdahl's law): uses the speedup from option 2, if it was run
        double speedup = parallelBench.bestSpeedup;
        if (speedup > 0) {
            page = page.replace("{{SPEEDUP}}", String.format(
                    "Measured: with more runways the same work finished <b>%.2f times faster</b> than with one.", speedup));
            page = page.replace("{{SPEEDUP_SHORT}}", String.format("Best speedup %.2fx", speedup));
        } else {
            page = page.replace("{{SPEEDUP}}",
                    "Not measured yet: choose option 2 in the menu first, then export the page again.");
            page = page.replace("{{SPEEDUP_SHORT}}", "Not measured (run option 2 first)");
        }

        // ---- 5 departures board and 6 gates (PCI)
        page = pciParts(page);

        PrintWriter out = new PrintWriter(fileName);
        out.print(page);
        out.close();
        System.out.println("Airport page saved to " + fileName);
    }

    // Like an airport code: the first three letters of the computer's name, in capitals.
    static String airportCode(String host) {
        String letters = host.replaceAll("[^A-Za-z]", "").toUpperCase();
        if (letters.isEmpty()) return "PC";
        return letters.length() > 3 ? letters.substring(0, 3) : letters;
    }

    // Reads /proc/stat twice, one second apart, for every core. The same method as cpuInfo.fastLoad(),
    // done here so the page can also show the raw ticks of core 0 in the control tower log.
    static String cpuParts(String page, int cores) {
        long[] busy1 = new long[cores];
        long[] idle1 = new long[cores];
        for (int i = 0; i < cores; i++) {
            busy1[i] = cpuInfo.getBusy(i);
            idle1[i] = cpuInfo.getIdle(i);
        }
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            // read the second time anyway
        }

        StringBuilder runways = new StringBuilder();
        String tower = "<p>The log could not be read (no cores found in /proc/cpuinfo).</p>";
        double total = 0;

        for (int i = 0; i < cores; i++) {
            long busy2 = cpuInfo.getBusy(i);
            long idle2 = cpuInfo.getIdle(i);
            long busy = busy2 - busy1[i];
            long idle = idle2 - idle1[i];
            double load = (busy + idle > 0) ? 100.0 * busy / (busy + idle) : 0;
            total += load;

            int width = (int) Math.round(load);
            runways.append("<div class=\"runway\"><span>Runway ").append(i)
                   .append("</span><div class=\"strip\"><div class=\"fill\" style=\"width:").append(width)
                   .append("%\"></div></div><b>").append(width).append("%</b></div>\n");

            if (i == 0) {
                tower = "<table class=\"log\"><tr><th></th><th>Busy ticks</th><th>Idle ticks</th></tr>"
                      + "<tr><td>First reading</td><td>" + busy1[0] + "</td><td>" + idle1[0] + "</td></tr>"
                      + "<tr><td>One second later</td><td>" + busy2 + "</td><td>" + idle2 + "</td></tr>"
                      + "<tr><td>Difference</td><td>" + busy + "</td><td>" + idle + "</td></tr></table>"
                      + String.format("<p class=\"calc\">Load = %d &divide; (%d + %d) = %.1f%%</p>", busy, busy, idle, load);
            }
        }
        if (cores == 0) runways.append("<p>No cores could be read.</p>");

        page = page.replace("{{RUNWAYS}}", runways.toString());
        page = page.replace("{{TOWER_LOG}}", tower);
        page = page.replace("{{AVG_LOAD}}", String.format("%.1f", cores > 0 ? total / cores : 0));
        return page;
    }

    // One seat for every 1% of memory: taken seats are the memory in use.
    static String memoryParts(String page) {
        double total = memInfo.getTotal() / 1048576;
        double used = memInfo.getUsed() / 1048576;

        if (total <= 0 || used < 0) {
            page = page.replace("{{MEM_TOTAL}}", "unknown");
            page = page.replace("{{MEM_USED}}", "unknown");
            page = page.replace("{{MEM_PERCENT}}", "unknown");
            return page.replace("{{SEATS}}", "<i></i>".repeat(100));
        }

        double percent = used / total * 100;
        int taken = (int) Math.round(percent);
        page = page.replace("{{MEM_TOTAL}}", String.format("%.2f", total));
        page = page.replace("{{MEM_USED}}", String.format("%.2f", used));
        page = page.replace("{{MEM_PERCENT}}", String.format("%.1f", percent));
        return page.replace("{{SEATS}}", "<i class=\"taken\"></i>".repeat(taken) + "<i></i>".repeat(100 - taken));
    }

    // 0 - Bus, 1 - Device, 2 - Vendor ID, 3 - Product ID, 4 - Vendor Name, 5 - Product Name
    static String usbList() throws Exception {
        if (usbInfo.getCount() <= 0) {
            return "<p>There is no taxi rank here: Linux found no USB devices. This is normal on a virtual machine "
                 + "or a cloud server, which have no ports to plug things into.</p>";
        }
        StringBuilder s = new StringBuilder();
        s.append("<table class=\"small\"><tr><th>Bus</th><th>Device</th><th>ID</th><th>Vendor and product</th></tr>\n");
        for (String[] d : usbInfo.getDevices()) {
            s.append("<tr><td>").append(d[0]).append("</td><td>").append(d[1]).append("</td><td>")
             .append(d[2]).append(":").append(d[3]).append("</td><td>")
             .append(htmlReport.escape(d[4])).append(", ").append(htmlReport.escape(d[5])).append("</td></tr>\n");
        }
        return s.append("</table>").toString();
    }

    // 0 - Address, 1 - Vendor ID, 2 - Device ID, 3 - Vendor Name, 4 - Device Name, 5 - Class code, 6 - Class name
    static String pciParts(String page) throws Exception {
        int count = pciInfo.getCount();
        page = page.replace("{{PCI_COUNT}}", String.valueOf(count));

        String vmNote = cpuInfo.getHypervisor().equals("Yes")
                ? "This computer is a virtual machine. It is like a terminal rented inside a bigger airport: "
                  + "the host (the hypervisor) runs the real building, and many of the gates here are virtual ones it provides."
                : "This computer is not a virtual machine, so these are the real devices on its PCI bus.";
        page = page.replace("{{VM_NOTE}}", vmNote);

        if (count == 0) {
            String none = "<tr><td colspan=\"7\">Linux found no PCI devices.</td></tr>";
            page = page.replace("{{BOARD_BY_GATE}}", none);
            page = page.replace("{{BOARD_BY_AIRLINE}}", none);
            page = page.replace("{{AIRLINES}}", "0");
            page = page.replace("{{AIRLINE_COUNT}}", "<p>No devices.</p>");
            return page.replace("{{GATE_EXAMPLE}}",
                    "<p>Linux found no PCI devices here. This is normal on WSL2, where the virtual machine hides the real hardware.</p>");
        }

        // The array is sorted in place, so each list is turned into rows straight after its sort.
        pciInfo.sortByVendorName();
        String byAirline = boardRows(pciInfo.getDevices());

        // devices per vendor, in vendor name order (the array is still sorted by vendor here)
        ArrayList<String> vendors = new ArrayList<>();
        ArrayList<Integer> amounts = new ArrayList<>();
        for (String[] d : pciInfo.getDevices()) {
            int i = vendors.indexOf(d[3]);
            if (i < 0) {
                vendors.add(d[3]);
                amounts.add(1);
            } else {
                amounts.set(i, amounts.get(i) + 1);
            }
        }
        StringBuilder airlines = new StringBuilder("<table><tr><th>Airline (vendor)</th><th>Devices</th></tr>\n");
        for (int i = 0; i < vendors.size(); i++) {
            airlines.append("<tr><td>").append(htmlReport.escape(vendors.get(i))).append("</td><td>")
                    .append(amounts.get(i)).append("</td></tr>\n");
        }
        airlines.append("</table>");

        pciInfo.sortByBusDeviceFunction();
        String[][] devices = pciInfo.getDevices();
        String byGate = boardRows(devices);

        // the example of how an address is read: the first device in bus order that has a known name
        String[] first = devices[0];
        for (String[] d : devices) {
            if (!d[4].startsWith("Unknown")) {
                first = d;
                break;
            }
        }
        String[] part = first[0].split("[:.]");   // domain, bus, device, function
        String example = "<p>Example: <code>" + first[0] + "</code> means road (bus) <b>" + part[1]
                + "</b>, gate (device) <b>" + part[2] + "</b>, door (function) <b>" + part[3]
                + "</b>. The first part, " + part[0] + ", is the domain (which airport), almost always 0000.</p>"
                + "<p>At that gate: " + htmlReport.escape(first[3]) + ", " + htmlReport.escape(first[4]) + ".</p>";

        page = page.replace("{{BOARD_BY_AIRLINE}}", byAirline);
        page = page.replace("{{BOARD_BY_GATE}}", byGate);
        page = page.replace("{{AIRLINES}}", String.valueOf(vendors.size()));
        page = page.replace("{{AIRLINE_COUNT}}", airlines.toString());
        return page.replace("{{GATE_EXAMPLE}}", example);
    }

    // One row of the departures board per PCI device; the address is split into bus, device and function.
    // It is split at the : and . signs, not at fixed positions, because the domain is not always
    // 4 digits (machines with Intel VMD have a domain such as 10000).
    static String boardRows(String[][] devices) {
        StringBuilder s = new StringBuilder();
        for (String[] d : devices) {
            String[] part = d[0].split("[:.]");   // domain, bus, device, function
            s.append("<tr><td class=\"tile\">").append(part[1])
             .append("</td><td class=\"tile\">").append(part[2])
             .append("</td><td class=\"tile\">").append(part[3])
             .append("</td><td>").append(htmlReport.escape(d[3]))
             .append("</td><td>").append(d[1]).append(":").append(d[2])
             .append("</td><td>").append(htmlReport.escape(d[4]))
             .append("</td><td>").append(htmlReport.escape(d[6])).append("</td></tr>\n");
        }
        return s.toString();
    }
}
