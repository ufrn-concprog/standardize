import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
 * The task executed by each worker thread to standardize a chunk of the dataset across three distinct phases:
 * partial sums, squared deviations, and normalization.
 */
public class Worker implements Runnable {
    /** Worker index identifier */
    private final int id;

    /** Starting index of the assigned data chunk (inclusive). */
    private final int start;

    /** Ending index of the assigned data chunk (exclusive). */
    private final int end;

    /** Shared array of numerical values to be standardized in place. */
    private final double[] data;

    /** Array storing the partial sum computed by each worker thread. */
    private final double[] partialSums;

    /** Array storing the partial sum of squared deviations computed by each worker thread. */
    private final double[] partialSquares;

    /** Synchronization barrier coordinating the worker threads across calculation phases. */
    private final CyclicBarrier barrier;

    /**
     * Constructs a worker task to process a chunk of the dataset.
     *
     * @param id             Worker ID
     * @param start          Starting index of the chunk (inclusive)
     * @param end            Ending index of the chunk (exclusive)
     * @param data           Shared data array to standardize in place
     * @param partialSums    Array collecting partial sums across workers
     * @param partialSquares Array collecting partial sums of squared deviations across workers
     * @param barrier        Synchronization barrier coordinating execution phases
     */
    public Worker(int id, int start, int end, double[] data, double[] partialSums,
                  double[] partialSquares, CyclicBarrier barrier) {
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
            // Phase 1 (partial sums): Compute sum of assigned data chunk.
            // Disjoint write to partialSums[id] requires no lock.
            partialSums[id] = Statistics.sum(data, start, end);

            // Barrier synchronization 1: Wait until every thread has finished Phase 1,
            // making all partial sums visible before anyone computes the global mean.
            barrier.await();

            // Phase 2 (squared deviations): Compute squared deviations using the global mean.
            double mean = Statistics.sum(partialSums) / data.length;
            partialSquares[id] = Statistics.sumOfSquaredDeviations(data, start, end, mean);

            // Barrier synchronization 2: Wait until every thread has finished Phase 2,
            // making all partial square deviations visible before anyone computes stdDev.
            // Reusing the same barrier demonstrates the cyclic capability of CyclicBarrier.
            barrier.await();

            // Phase 3 (normalization): Normalize local chunk in place as (value - mean) / stdDev.
            double stdDev = Math.sqrt(Statistics.sum(partialSquares) / data.length);
            Statistics.standardize(data, start, end, mean, stdDev);
        } catch (InterruptedException | BrokenBarrierException e) {
            Thread.currentThread().interrupt();
        }
    }
}