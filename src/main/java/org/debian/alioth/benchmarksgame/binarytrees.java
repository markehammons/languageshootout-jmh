package org.debian.alioth.benchmarksgame;

/**
 * The Computer Language Benchmarks Game
 * http://benchmarksgame.alioth.debian.org/
 *
 * based on Jarkko Miettinen's Java program
 * contributed by Tristan Dupont
 * *reset*
 */

import java.io.File;
import java.io.PrintStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;


public class binarytrees {

    @State(Scope.Thread)
    public static class binarytreesState {
        public final int MIN_DEPTH = 4;
        public ExecutorService EXECUTOR_SERVICE;

        @TearDown(Level.Invocation)
        public void teardown() {
            try {
                printStream.close();
            } catch (Exception e) {

            }
        }

        @Setup(Level.Invocation)
        public void setup() {
            try {
                EXECUTOR_SERVICE = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
                printStream = new PrintStream(new File("/dev/null"));
            } catch (Exception e) {

            }
        }


        public PrintStream printStream;

    }


    @Benchmark
    @BenchmarkMode({Mode.SingleShotTime, Mode.SampleTime, Mode.AverageTime}) @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void main(final binarytreesState state) throws Exception {
        int n = 21;

        final int maxDepth = n < (state.MIN_DEPTH + 2) ? state.MIN_DEPTH + 2 : n;
        final int stretchDepth = maxDepth + 1;

        state.printStream.println("stretch tree of depth " + stretchDepth + "\t check: "
                + bottomUpTree( stretchDepth).itemCheck());

        final TreeNode longLivedTree = bottomUpTree(maxDepth);

        final String[] results = new String[(maxDepth - state.MIN_DEPTH) / 2 + 1];

        for (int d = state.MIN_DEPTH; d <= maxDepth; d += 2) {
            final int depth = d;
            state.EXECUTOR_SERVICE.execute(() -> {
                int check = 0;

                final int iterations = 1 << (maxDepth - depth + state.MIN_DEPTH);
                for (int i = 1; i <= iterations; ++i) {
                    final TreeNode treeNode1 = bottomUpTree(depth);
                    check += treeNode1.itemCheck();
                }
                results[(depth - state.MIN_DEPTH) / 2] =
                        iterations + "\t trees of depth " + depth + "\t check: " + check;
            });
        }

        state.EXECUTOR_SERVICE.shutdown();
        state.EXECUTOR_SERVICE.awaitTermination(125L, TimeUnit.SECONDS);



        for (final String str : results) {
            state.printStream.println(str);
        }

        state.printStream.println("long lived tree of depth " + maxDepth +
                "\t check: " + longLivedTree.itemCheck());
    }

    private TreeNode bottomUpTree(final int depth) {
        if (0 < depth) {
            return new TreeNode(bottomUpTree(depth - 1), bottomUpTree(depth - 1));
        }
        return new TreeNode();
    }

    private final class TreeNode {

        private final TreeNode left;
        private final TreeNode right;

        private TreeNode(final TreeNode left, final TreeNode right) {
            this.left = left;
            this.right = right;
        }

        private TreeNode() {
            this(null, null);
        }

        private int itemCheck() {
            // if necessary deallocate here
            if (null == left) {
                return 1;
            }
            return 1 + left.itemCheck() + right.itemCheck();
        }

    }

}