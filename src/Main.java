import java.util.Random;
import java.util.concurrent.CyclicBarrier;

/** Standardizes a synthetic dataset of values */
public class Main {
    /** Dataset size */
    private static final int DATASET_SIZE = 1000000;

    /** Number of threads to use (equal to the number of available processors) */
    private static final int NUM_THREADS = Runtime.getRuntime().availableProcessors();

    public static void main(String[] args) throws InterruptedException {
        // Generates an array containing n random double values in the range [0, 100).
        double[] data = new Random().doubles(DATASET_SIZE, 0, 100).toArray(); // e.g., sensor readings

        // Per-thread intermediate reduction buffers; lock-free since worker t only writes index t
        double[] partialSums = new double[NUM_THREADS];
        double[] partialSquares = new double[NUM_THREADS];

        // Reused across two synchronization points (Phase 1 -> Phase 2 and Phase 2 -> Phase 3)
        CyclicBarrier barrier = new CyclicBarrier(NUM_THREADS);

        // Partition dataset across worker threads
        Thread[] threads = new Thread[NUM_THREADS];
        for (int t = 0; t < NUM_THREADS; t++) {
            int start = t * DATASET_SIZE / NUM_THREADS;
            int end = (t + 1) * DATASET_SIZE / NUM_THREADS;
            threads[t] = new Thread(new Worker(t, start, end, data, partialSums, partialSquares, barrier),
                    "Worker-" + (t + 1));
            threads[t].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        // Check sequentially: a standardized dataset has mean ~0 and std dev ~1
        double mean = Statistics.sum(data) / DATASET_SIZE;
        double stdDev = Math.sqrt(Statistics.sumOfSquaredDeviations(data, 0, DATASET_SIZE, mean) / DATASET_SIZE);
        System.out.printf("Mean: %.2e (expected ~0)%n", mean);
        System.out.printf("Standard deviation: %.4f (expected ~1)%n", stdDev);
    }
}