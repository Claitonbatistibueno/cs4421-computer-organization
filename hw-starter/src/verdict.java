public class verdict {

    record Need(String name, double cores, double ramGiB, boolean ssd, boolean gpu, double speedup, String why) {}

    static final Need[] NEEDS = {
        new Need("Study", 2, 4, false, false, 0,
                "A browser, documents and video calls run well on 2 cores and 4 GiB."),
        new Need("Programming", 4, 8, true, false, 0,
                "An IDE, a browser and a compiler at the same time need about 8 GiB; an SSD makes builds faster."),
        new Need("Video editing", 6, 16, true, true, 4,
                "Rendering uses every core, so the measured speedup must be high; 16 GiB holds the footage; a GPU speeds up effects."),
        new Need("Gaming", 4, 16, true, true, 0,
                "Modern games need a dedicated GPU; without one they do not run well, whatever the CPU.")
    };

    static final double TOLERANCE = 0.9;

    static String rate(Need n, int cores, double ramGiB, String disk, boolean hasGpu, double speedup) {
        if (n.gpu() && !hasGpu) return "NO";
        double worst = Math.min(cores / n.cores(), ramGiB / n.ramGiB());
        if (n.speedup() > 0) worst = Math.min(worst, speedup / n.speedup());
        if (n.ssd() && disk.equals("HDD")) worst = Math.min(worst, 0.5);
        if (worst >= TOLERANCE) return "YES";
        if (worst >= TOLERANCE / 2) return "PARTLY";
        return "NO";
    }

    static final String[] VIRTUAL_GPUS = {"virtual", "hyper-v", "vmware", "virtualbox", "qxl", "virtio", "bochs", "cirrus",
            "1234:", "1af4:", "1b36:", "15ad:", "80ee:", "1414:"};

    static String gpuType() throws Exception {
        String gpu = pciInfo.getGPU();
        if (gpu.startsWith("No GPUs")) return "none";
        String lower = gpu.toLowerCase();
        for (String v : VIRTUAL_GPUS)
            if (lower.contains(v)) return "virtual";
        return "real";
    }

    static String diskType() {
        String d = diskInfo.getDriveType().toLowerCase();
        if (d.equals("ssd") || d.equals("sdd")) return "SSD";
        if (d.equals("hdd")) return "HDD";
        return "unknown";
    }

    static void print(double speedup) throws Exception {
        int cores = cpuInfo.getCount();
        double ramGiB = memInfo.getTotal() / 1048576;
        String disk = diskType();
        String gpu = gpuType();
        boolean hasGpu = gpu.equals("real");

        System.out.println("===== THIS COMPUTER IS GOOD FOR... =====");
        System.out.printf("Based on: %d cores, %.1f GiB RAM, disk %s, %s, measured speedup %.2fx%n%n",
                cores, ramGiB, disk,
                gpu.equals("real") ? "GPU found" : gpu.equals("virtual") ? "virtual GPU only" : "no GPU", speedup);

        for (Need n : NEEDS) {
            System.out.printf("%-14s %-7s needs %.0f cores, %.0f GiB%s%s%s%n",
                    n.name(), rate(n, cores, ramGiB, disk, hasGpu, speedup), n.cores(), n.ramGiB(),
                    n.ssd() ? ", SSD" : "", n.gpu() ? ", GPU" : "",
                    n.speedup() > 0 ? String.format(", speedup %.0fx", n.speedup()) : "");
            System.out.println("               " + n.why());
        }

        System.out.printf("%nYES = meets every need (within %.0f%%, as the OS reserves some RAM)%n", 100 * (1 - TOLERANCE));
        System.out.println("PARTLY = at least half of every need. NO = less, or a required GPU is missing.");
        if (disk.equals("unknown")) System.out.println("Disk type could not be read, so it was not counted.");
        if (gpu.equals("virtual")) System.out.println("The only display adapter is a virtual one (from the virtual machine), so it does not count as a GPU.");
    }

    public static void main(String[] args) throws Exception {
        print(args.length > 0 ? Double.parseDouble(args[0]) : 1);
    }
}
