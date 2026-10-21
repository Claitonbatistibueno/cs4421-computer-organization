import java.security.MessageDigest;
import java.io.*;
import java.util.Random;
import java.util.zip.*;

public class cpuBench {
    private static final double REFERENCE_SCORE = 578.28;
    private static final int RUNS = 5;
    private static final int SIZE = 16 * 1024 * 1024;
    private static double score = 0;
    private static double rawScore = 0;
    private static boolean benchmarkRan = false;
    private static byte[] data = new byte[SIZE];

    private static double sha() throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        long start = System.nanoTime();
        md.update(data);
        byte[] hash = md.digest();
        long end = System.nanoTime();
        return SIZE / 1048576.0 / ((end - start) / 1e9);
    }

    private static byte[] compress() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("data"));
            zip.write(data);
            zip.closeEntry();
        }
        return out.toByteArray();
    }

    private static double decompress(byte[] compressed) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        long start = System.nanoTime();
        try (ZipInputStream zip = new ZipInputStream(
                new ByteArrayInputStream(compressed))) {
            zip.getNextEntry();
            zip.transferTo(out);
        }
        long end = System.nanoTime();
        if (!java.util.Arrays.equals(data, out.toByteArray()))
            throw new RuntimeException("Verification failed");
        return SIZE / 1048576.0 / ((end - start) / 1e9);
    }

    private static double[] zip() throws Exception {
        long start = System.nanoTime();
        byte[] compressed = compress();
        long end = System.nanoTime();
        double compression = SIZE / 1048576.0
                / ((end - start) / 1e9);
        double decompression = decompress(compressed);
        return new double[]{compression, decompression};
    }

    private static void benchmark() throws Exception {
        new Random(42).nextBytes(data);

        for (int i = 0; i < 2; i++) {
            sha();
            zip();
        }

        double shaTotal = 0, compTotal = 0, decompTotal = 0;

        for (int i = 0; i < RUNS; i++) {
            shaTotal += sha();
            double[] z = zip();
            compTotal += z[0];
            decompTotal += z[1];
        }

        double shaAvg = shaTotal / RUNS;
        double zipAvg = (compTotal / RUNS + decompTotal / RUNS) / 2;
        double score = (shaAvg + zipAvg) / 2;

        cpuBench.rawScore = score;
        cpuBench.score = score / REFERENCE_SCORE;
        benchmarkRan = true;
    }

    private static void benchmarkRan(){
        if (!benchmarkRan) {
            try {
                benchmark();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static double getScore() throws Exception {
        benchmarkRan();
        return score;
    }

    public static double getRawScore() throws Exception {
        benchmarkRan();
        return rawScore;
    }
}
