import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
 * The task executed by each worker thread to standardize a slice of the dataset across three distinct phases.
 *
 * <p><strong>Phase Breakdown & Synchronization:</strong>
 * <ul>
 *   <li><strong>Phase 1 (Partial Sums):</strong> Each thread calculates the sum of its own chunk of {@code data}.
 *       Threads rendezvous at the barrier because computing the global mean requires every chunk's sum.</li>
 *   <li><strong>Phase 2 (Squared Deviations):</strong> After passing the first barrier, all threads read all partial sums to
 *       determine the global mean and compute squared deviations for their chunk. Threads rendezvous at the second barrier
 *       because computing standard deviation requires every chunk's squared deviations.</li>
 *   <li><strong>Phase 3 (Standardization):</strong> After passing the second barrier, threads compute standard deviation and
 *       standardize their local chunk in place into z-scores: {@code (value - mean) / stdDev}.</li>
 * </ul>
 *
 * <p><strong>Key Concurrency Principles:</strong>
 * <ul>
 *   <li><strong>Why barriers are required:</strong> No thread can compute deviations until every thread contributes to the mean,
 *       and no thread can normalize until every thread contributes to the standard deviation.</li>
 *   <li><strong>Why join is insufficient:</strong> Using {@code Thread.join()} between phases would require destroying and recreating
 *       threads, losing per-thread state and incurring high lifecycle overhead. {@code CyclicBarrier} reuses the exact same threads
 *       across multiple rendezvous points.</li>
 *   <li><strong>No locks needed:</strong> Thread writes are disjoint (each thread writes strictly to index {@code id} of
 *       {@code partialSums}/{@code partialSquares} and range {@code [start, end)} of {@code data}). The barrier's memory
 *       visibility guarantees ensure writes are visible to all threads before subsequent phases read them.</li>
 * </ul>
 */
public class Worker implements Runnable {
    private final int id;
    private final int start;
    private final int end;

    /** The array of numerical values to be standardized in place */
    private final double[] data;

    /** Array storing the partial sum computed by each worker thread */
    private final double[] partialSums;

    /** Array storing the partial sum of squared deviations computed by each worker thread */
    private final double[] partialSquares;

    /** The synchronization barrier coordinating the worker threads across calculation phases */
    private final CyclicBarrier barrier;

    /**
     * Constructs a worker task to process a chunk of the dataset.
     *
     * @param id             worker index identifier
     * @param start          starting index of the chunk (inclusive)
     * @param end            ending index of the chunk (exclusive)
     * @param data           the shared data array to standardize
     * @param partialSums    array collecting partial sums across workers
     * @param partialSquares array collecting partial sum of squared deviations
     * @param barrier        the synchronization barrier coordinating execution phases
     */
    public Worker(int id, int start, int end, double[] data, double[] partialSums, double[] partialSquares, CyclicBarrier barrier) {
        this.id = id;
        this.start = start;
        this.end = end;
        this.data = data;
        this.partialSums = partialSums;
        this.partialSquares = partialSquares;
        this.barrier = barrier;
    }

    @Override
    public void run() {
        try {
            // Phase 1: Compute sum of local chunk.
            // Disjoint write to partialSums[id] requires no lock.
            partialSums[id] = Statistics.sum(data, start, end);

            // Barrier Rendezvous 1: Wait until every thread has finished Phase 1,
            // making all partial sums visible before anyone computes the global mean.
            barrier.await();

            // Phase 2: Compute squared deviations using the global mean.
            double mean = Statistics.sum(partialSums) / data.length;
            partialSquares[id] = Statistics.sumOfSquaredDeviations(data, start, end, mean);

            // Barrier Rendezvous 2: Wait until every thread has finished Phase 2,
            // making all partial squared deviations visible before anyone computes stdDev.
            // Reusing the same barrier demonstrates the "cyclic" capability of CyclicBarrier.
            barrier.await();

            // Phase 3: Normalize local chunk in place to z-score: (value - mean) / stdDev.
            double stdDev = Math.sqrt(Statistics.sum(partialSquares) / data.length);
            Statistics.standardize(data, start, end, mean, stdDev);
        } catch (InterruptedException | BrokenBarrierException e) {
            Thread.currentThread().interrupt();
        }
    }
}