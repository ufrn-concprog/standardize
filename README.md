# Concurrent Dataset Standardization

This project implements a concurrent solution in the Java programming language to standardize a dataset (*z-score* normalization), a common preprocessing step before statistical analysis or machine learning:

$$z = \frac{x - \mu}{\sigma}$$

where each value $x$ is rescaled to $(x - \text{mean}) / \text{standard deviation}$.

This repository serves a pedagogical purpose, demonstrating how to coordinate multiple concurrently running threads with a real barrier dependency using Java's [`CyclicBarrier`](https://docs.oracle.com/en/java/javase/26/docs/api/java.base/java/util/concurrent/CyclicBarrier.html) synchronization mechanism.

## 📃 About

The standardization operates on arrays of numbers, where the dependency between phases is clear:

* No thread can compute deviations until every thread has contributed to the mean.
* No thread can normalize values until every thread has contributed to the standard deviation.

The implementation reuses the same barrier twice across three distinct computational phases, showcasing the cyclic capability of [`CyclicBarrier`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/CyclicBarrier.html).

### Execution Workflow

The dataset is partitioned into contiguous chunks, one per worker thread ([`Worker`](src/Worker.java)):

1. **Phase 1 (partial sums)**: Each thread computes the sum of its assigned data chunk and stores the result in its dedicated index of the set of partial sums.
2. **Barrier synchronization 1**: Threads wait at `barrier.await()`. No thread can proceed to compute deviations until all threads have completed their partial sum calculations, ensuring the global mean can be computed.
3. **Phase 2 (squared deviations)**: All threads read the set of partial sums to determine the global mean ($\mu$) and compute the sum of squared deviations for their chunk, writing the result into a set of partial squares.
4. **Barrier synchronization 2**: Threads wait at the same `barrier.await()`. No thread can normalize its chunk until all threads have finished Phase 2, ensuring the global standard deviation ($\sigma$) can be computed.
5. **Phase 3 (normalization)**: Each thread computes the global standard deviation and normalizes its chunk as $(x - \mu) / \sigma$.

**Why are barriers required?** 
The global mean depends on every chunk, and the standard deviation depends on the global mean. A thread that finished its own chunk cannot continue until all the others have finished too, so the algorithm has two synchronization points, and the same [`CyclicBarrier`](https://docs.oracle.com/en/java/javase/26/docs/api/java.base/java/util/concurrent/CyclicBarrier.html) serves both.

**Why would not a `join()` be enough?**
The threads could be joined after each phase, and new ones started, but then each phase would pay for creating new threads and would lose the per-thread state (chunk and mean). A barrier lets the same threads continue to the next phase. This matters more as the number of phases grows, as in iterative algorithms.

Note that no locks are needed here, because each thread writes only its own slot of the set of partial sums and squares, and only its own data chunk. The barrier makes those writes visible to the other threads before anyone reads them.

## 📂 Repository Structure

```
standardize/
├── README.md
└── src/
    ├── Main.java          # Main application entry point and thread orchestration
    ├── Statistics.java    # Sequential calculation helpers (sum, deviations, standardization)
    └── Worker.java        # Worker thread task executing the three phases with CyclicBarrier
```

## 🚀 Getting Started

### Prerequisites

* Java Development Kit (JDK) 8 or newer
* A terminal or IDE

This program uses Java's standard library, so it requires no additional dependencies.

### Running

Output example:

```text
Mean: -8.93e-15 (expected ~0)
Standard deviation: 1.0000 (expected ~1)
```
