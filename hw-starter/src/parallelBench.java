public class parallelBench {

    static final int LIMIT = 5_000_000;
    static final int RUNS = 3;
    static final int CHUNK = 10_000;
    static final int[] THREADS = {1, 2, 4, 8};

    static boolean isPrime(int n) {
        if (n < 2) return false;
        for (int d = 2; d * d <= n; d++)
            if (n % d == 0) return false;
        return true;
    }

    static int countPrimes(int threads) throws InterruptedException {
        int[] counts = new int[threads];
        Thread[] workers = new Thread[threads];

        for (int t = 0; t < threads; t++) {
            int id = t;
            workers[t] = new Thread(() -> {
                for (int start = id * CHUNK; start < LIMIT; start += threads * CHUNK)
                    for (int n = start; n < Math.min(start + CHUNK, LIMIT); n++)
                        if (isPrime(n)) counts[id]++;
            });
            workers[t].start();
        }
        for (Thread w : workers) w.join();

        int total = 0;
        for (int c : counts) total += c;
        return total;
    }

    static double averageTime(int threads, int expected) throws InterruptedException {
        double total = 0;
        for (int r = 0; r < RUNS; r++) {
            long start = System.nanoTime();
            if (countPrimes(threads) != expected)
                throw new IllegalStateException("Wrong result with " + threads + " threads");
            total += (System.nanoTime() - start) / 1e9;
        }
        return total / RUNS;
    }

    static String coreLoad(long[] busy, long[] idle) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < busy.length; i++) {
            double b = cpuInfo.getBusy(i) - busy[i];
            double t = b + cpuInfo.getIdle(i) - idle[i];
            s.append(String.format("%4.0f%%", t > 0 ? 100 * b / t : 0));
        }
        return s.toString();
    }

    public static void main(String[] args) throws InterruptedException {
        int cores = cpuInfo.getCount();
        int primes = countPrimes(1);

        double[] time = new double[THREADS.length];
        String[] load = new String[THREADS.length];

        for (int i = 0; i < THREADS.length; i++) {
            long[] busy = new long[cores];
            long[] idle = new long[cores];
            for (int c = 0; c < cores; c++) {
                busy[c] = cpuInfo.getBusy(c);
                idle[c] = cpuInfo.getIdle(c);
            }
            time[i] = averageTime(THREADS[i], primes);
            load[i] = coreLoad(busy, idle);
        }

        int ref = 0;
        for (int i = 0; i < THREADS.length; i++)
            if (THREADS[i] <= cores) ref = i;
        int n = THREADS[ref];
        double s = time[0] / time[ref];
        double p = n > 1 ? (1 - 1 / s) / (1 - 1.0 / n) : 1;

        System.out.println("===== MEASURED PERFORMANCE =====");
        System.out.printf("Task: count primes below %,d (%,d found), average of %d runs%n", LIMIT, primes, RUNS);
        System.out.printf("Logical cores: %d%n", cores);
        System.out.printf("Estimated parallel fraction p = %.3f (from %d threads)%n%n", p, n);
        System.out.printf("%-8s %-9s %-8s %-11s %-8s %s%n",
                "Threads", "Time (s)", "Speedup", "Efficiency", "Amdahl", "Load per core");

        for (int i = 0; i < THREADS.length; i++) {
            int t = THREADS[i];
            double speedup = time[0] / time[i];
            double amdahl = 1 / ((1 - p) + p / Math.min(t, cores));
            System.out.printf("%-8d %-9.2f %-8.2f %-11s %-8.2f %s%n",
                    t, time[i], speedup, String.format("%.0f%%", 100 * speedup / t), amdahl, load[i]);
        }

        System.out.printf("%nMaximum speedup predicted by Amdahl with unlimited cores: %.1fx%n", 1 / (1 - p));
        System.out.println("With more threads than cores, the extra threads only share the same cores.");
    }
}