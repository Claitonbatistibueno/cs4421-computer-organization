// Class made by Eoin for displaying info

import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

public class display {
    public static void main(String[] args) throws Exception { 

        JFrame frame = new JFrame();
        frame.setSize(1000, 1000);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        frame.setBounds(null);

        String[] columns = {"Name", "Data"};
        Object[][] data = { {"Model", cpuInfo.getModel()}, {"Cores", cpuInfo.getCount()}};
        DefaultTableModel model = new DefaultTableModel(data, columns);
        JTable table = new JTable(model);

        frame.add(table);
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

}
