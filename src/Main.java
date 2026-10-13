import java.util.Random;
import java.util.concurrent.CyclicBarrier;

/**
 * Entry point for concurrent dataset standardization using a {@link CyclicBarrier}.
 *
 * <p>Standardizing data (<em>z-score</em> normalization) rescales each value in the dataset,
 * a common preprocessing step before statistical analysis or machine learning:
 * <pre>
 *   z = (x - &mu;) / &sigma;
 * </pre>
 *
 * Phase breakdown and synchronization:
 * <ul>
 *   <li><strong>Phase 1 (partial sums):</strong> Each thread calculates the sum of its assigned data chunk
 *       and stores the result in its dedicated index of the set of partial sums. Threads synchronize at the
 *       first barrier because computing the global mean (&mu;) requires every chunk's sum.</li>
 *   <li><strong>Phase 2 (squared deviations):</strong> After passing the first barrier, all threads read the
 *       set of partial sums to determine the global mean (&mu;) and compute the sum of squared deviations
 *       for their chunk, writing the result into a set of partial squares. Threads synchronize at the second
 *       barrier because computing standard deviation (&sigma;) requires every chunk's squared deviations.</li>
 *   <li><strong>Phase 3 (normalization):</strong> After passing the second barrier, each thread computes the
 *       global standard deviation (&sigma;) and normalizes its chunk in place as {@code (value - mean) / stdDev}.</li>
 * </ul>
 */
public class Main {
    /** Dataset size (total number of elements) */
    private static final int DATASET_SIZE = 1000000;

    /** Number of worker threads to use (equal to the number of available processors) */
    private static final int NUM_THREADS = Runtime.getRuntime().availableProcessors();

    private Main() {}

    /**
     * Application main entry point.
     *
     * <p>Generates a synthetic dataset of values, coordinates parallel standardization
     * across worker threads using a {@link CyclicBarrier}, and sequentially verifies
     * that the normalized dataset has a mean of ~0 and standard deviation of ~1.
     *
     * @param args command-line arguments (not used)
     * @throws InterruptedException if the main thread is interrupted while waiting for worker
     *                              threads to finish
     */
    public static void main(String[] args) throws InterruptedException {
        // Generates an array containing DATASET_SIZE random double values in the range [0, 100).
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

        // Await completion of all phases across all worker threads
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