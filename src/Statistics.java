/**
 * Sequential calculation helpers for computing statistics over array ranges.
 * It contains no concurrency code, so it can be tested and reasoned about independently.
 */
public final class Statistics {
    private Statistics() {}

    /**
     * Sums the values in a range of an array.
     *
     * @param values Array to read
     * @param start  First index (inclusive)
     * @param end    Last index (exclusive)
     * @return Sum of {@code values[start..end)}
     */
    public static double sum(double[] values, int start, int end) {
        double total = 0;
        for (int i = start; i < end; i++) {
            total += values[i];
        }
        return total;
    }

    /**
     * Sums all the elements of an array.
     *
     * @param values Array to read
     * @return Sum of all elements in {@code values}
     */
    public static double sum(double[] values) {
        return sum(values, 0, values.length);
    }

    /**
     * Sums the squared deviations from a given mean over a range of an array.
     *
     * @param values Array to read
     * @param start  First index (inclusive)
     * @param end    Last index (exclusive)
     * @param mean   Mean to measure deviations from (&mu;)
     * @return Sum of {@code (values[i] - mean)^2} for {@code i} in the range {@code [start, end)}
     */
    public static double sumOfSquaredDeviations(double[] values, int start, int end, double mean) {
        double total = 0;
        for (int i = start; i < end; i++) {
            double deviation = values[i] - mean;
            total += deviation * deviation;
        }
        return total;
    }

    /**
     * Replaces each value in a range by its standardized z-score in place:
     * {@code (value - mean) / stdDev}.
     *
     * @param values Array to modify
     * @param start  First index (inclusive)
     * @param end    Last index (exclusive)
     * @param mean   Mean of the whole dataset (&mu;)
     * @param stdDev Standard deviation of the whole dataset (&sigma;, must not be zero)
     */
    public static void standardize(double[] values, int start, int end, double mean, double stdDev) {
        for (int i = start; i < end; i++) {
            values[i] = (values[i] - mean) / stdDev;
        }
    }
}