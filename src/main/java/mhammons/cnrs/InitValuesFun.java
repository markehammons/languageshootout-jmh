package mhammons.cnrs;

import org.graalvm.polyglot.Value;

@FunctionalInterface
public interface InitValuesFun {
    void execute(double[] arr, long wid_ht);
}
