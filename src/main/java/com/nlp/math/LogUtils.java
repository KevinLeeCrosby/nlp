package com.nlp.math;

import com.google.common.collect.ImmutableList;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.lang.Double.isInfinite;
import static java.lang.Math.exp;
import static java.lang.Math.expm1;
import static java.lang.Math.log;
import static java.lang.Math.log1p;

/**
 * Logarithm Utilities.
 *
 * @author Kevin Crosby.
 */
public class LogUtils {
  private LogUtils() {
  }

  public static Collection<Double> expNormalize(Collection<Double> logarithms) {
    double sumExp = sumExp(logarithms);
    return logarithms.stream()
        .map(Math::exp)
        .map(v -> v / sumExp)
        .collect(ImmutableList.toImmutableList()); // normal space
  }

  public static Collection<Double> logSumExpNormalize(Collection<Double> logarithms) {
    double logSumExp = logSumExp(logarithms);
    return logarithms.stream()
        .map(v -> v - logSumExp)
        .collect(ImmutableList.toImmutableList()); // log space
  }

  // based on or(p, q) = p + q - p * q in normal space
  public static double logFuzzyOr(double log1, double log2) {
    if (log1 < log2) {
      return logFuzzyOr(log2, log1);
    }
    return log2 == Double.NEGATIVE_INFINITY ? log1 : log1p(exp(log2) * expm1(-log1)) + log1;
  }

  public static double logFuzzyOr(List<Double> logarithms) {
    int n = logarithms.size();
    switch (n) {
      case 0:
        return 0;
      case 1:
        return logarithms.get(0);
      case 2:
        return logFuzzyOr(logarithms.get(0), logarithms.get(1));
      default:
        return logFuzzyOr(logFuzzyOr(logarithms.subList(0, n / 2)), logFuzzyOr(logarithms.subList(n / 2, n)));
    }
  }

  public static double logDot(List<Double> logarithms1, List<Double> logarithms2) {
    assert logarithms1.size() == logarithms2.size() : "Vectors must be the same size!";
    List<Double> summands = IntStream.range(0, logarithms1.size())
        .mapToDouble(i -> logarithms1.get(i) + logarithms2.get(i))
        .boxed()
        .collect(Collectors.toList());
    return logSumExp(summands); // log space
  }

  public static double logSumExp(double log1, double log2) {
    if (log1 < log2) {
      return logSumExp(log2, log1);
    }
    // log1 ≥ log2
    return isInfinite(log2) ? log1 : log1p(exp(log2 - log1)) + log1; // log space
  }

  public static double logSumExp(Collection<Double> logarithms) {
    double max = max(logarithms);
    double sumExp = sumExp(logarithms, max);
    return log(sumExp) + max; // log space
  }

  private static double sumExp(Collection<Double> logarithms) {
    return sumExp(logarithms, max(logarithms)); // normal space
  }

  private static double sumExp(Collection<Double> logarithms, double max) {
    if (isInfinite(max)) {
      return exp(max);
    }
    return logarithms.stream()
        .mapToDouble(v -> v - max)
        .map(Math::exp)
        .sum(); // normal space
  }

  private static double max(double value1, double value2) {
    return Math.max(value1, value2);
  }

  private static double max(Collection<Double> values) {
    return Collections.max(values);
  }
}
