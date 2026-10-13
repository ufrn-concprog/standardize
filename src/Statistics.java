/**
 * Sequential helpers for computing statistics over a range of an array.
 * Contains no concurrency code, so it can be tested and reasoned about on its own.
 */
public final class Statistics {
    /**
     * Sums the values in a range.
     *
     * @param values the array to read
     * @param start  first index (inclusive)
     * @param end    last index (exclusive)
     * @return the sum of {@code values[start..end)}
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
     * @param values the array to read
     * @return the sum of {@code values}
     */
    public static double sum(double[] values) {
        return sum(values, 0, values.length);
    }

    /**
     * Sums the squared deviations from a mean over a range.
     *
     * @param values the array to read
     * @param start  first index (inclusive)
     * @param end    last index (exclusive)
     * @param mean   the mean to measure deviations from
     * @return the sum of {@code (values[i] - mean)^2} for {@code i} in the range
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
     * Replaces each value in a range by its z-score, in place.
     *
     * @param values the array to modify
     * @param start  first index (inclusive)
     * @param end    last index (exclusive)
     * @param mean   the mean of the whole dataset
     * @param stdDev the standard deviation of the whole dataset (must not be zero)
     */
    public static void standardize(double[] values, int start, int end, double mean, double stdDev) {
        for (int i = start; i < end; i++) {
            values[i] = (values[i] - mean) / stdDev;
        }
    }
}