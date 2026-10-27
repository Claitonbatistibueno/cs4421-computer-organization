// Flourish: Class that stores collected hardware information
public class hardwareData {

    // Stores CPU readings
    public double[] coreLoads;

    // Stores total and used memory
    public double[] memory;
    
    // Flourish: Collect memory information
    public static double[] getMemoryData() {
        double memTotal = memInfo.getTotal() / 1048576.0;
        double memUsed = memInfo.getUsed() / 1048576.0;
        return new double[] {memTotal, memUsed};
}

    // Flourish: Collect CPU load information
    public static double[] getCpuData() {
     return cpuInfo.fastLoad();
}

    // Stores PCI device information
    public String[][] pciDevices;

    // Stores USB device information
    public String[][] usbDevices;

    // Constructor collects hardware information when an object is created
    public hardwareData() throws Exception {

        System.out.println("Collecting CPU...");
        coreLoads = getCpuData();

        System.out.println("Collecting Memory...");
        memory = getMemoryData();

        // Flourish: Collect PCI devices once
        System.out.println("Collecting PCI...");
        pciInfo.arrayBuilderPCIe();
        String[][] devices = pciInfo.getDevices();

        pciDevices = new String[devices.length][];

        for (int i = 0; i < devices.length; i++) {
            pciDevices[i] = devices[i].clone();
        }

        // Flourish: Collect USB devices once
        System.out.println("Collecting USB...");
        String[][] usb = usbInfo.getDevices();

        usbDevices = new String[usb.length][];

        for (int z = 0; z < usb.length; z++) {
            usbDevices[z] = usb[z].clone();
        }

        System.out.println("Finished collecting!");
    }
}