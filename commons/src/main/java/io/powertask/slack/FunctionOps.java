/*
 * Copyright © 2020 Lunatech Labs B.V. and/or licensed to Lunatech Labs B.V. under
 * one or more contributor license agreements. Lunatech licenses this file to you
 * under the Apache License, Version 2.0; you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package io.powertask.slack;

import java.util.function.Function;
import java.util.function.Supplier;

/** Operations on Functions */
public class FunctionOps {

  /** Supplier that throws Exception */
  @FunctionalInterface
  public interface ThrowingSupplier<T> {
    T get() throws Exception;
  }

  /** Function that throws Exception */
  @FunctionalInterface
  public interface ThrowingFunction<T, U> {
    U apply(T t) throws Exception;
  }

  /**
   * Convert a supplier that throws into one that doesnt.
   *
   * <p>Checked exceptions get wrapped using the supplied wrapper function.
   */
  public static <T> Supplier<T> wrapExceptionsSupplier(
      ThrowingSupplier<T> throwingSupplier, Function<Throwable, RuntimeException> wrapper) {
    return () -> {
      try {
        return throwingSupplier.get();
      } catch (Exception e) {
        throw wrapper.apply(e);
      }
    };
  }

  /**
   * Convert a supplier that throws into one that doesnt.
   *
   * <p>Checked exceptions get wrapped in a RuntimeException, Runtime exceptions thrown as-is.
   */
  public static <T> Supplier<T> wrapExceptionsSupplier(ThrowingSupplier<T> throwingSupplier) {
    return wrapExceptionsSupplier(throwingSupplier, FunctionOps::wrapNonRuntimeException);
  }

  /**
   * Runs a supplier, wrapping checked exceptions.
   *
   * <p>Checked exceptions get wrapped using the given wrapper.
   */
  public static <T> T wrapExceptions(
      ThrowingSupplier<T> throwingSupplier, Function<Throwable, RuntimeException> wrapper) {
    try {
      return throwingSupplier.get();
    } catch (Exception e) {
      throw wrapper.apply(e);
    }
  }

  /**
   * Runs a supplier, wrapping checked exceptions.
   *
   * <p>Checked exceptions get wrapped in a RuntimeException, Runtime exceptions thrown as-is.
   */
  public static <T> T wrapExceptions(ThrowingSupplier<T> throwingSupplier) {
    return wrapExceptions(throwingSupplier, FunctionOps::wrapNonRuntimeException);
  }

  /**
   * Convert a Function that throws Exception into one that doesn't.
   *
   * @param throwingFunction Function with checked exceptions declared
   * @param wrapper Function to wrap an exception
   * @param <T> type of the input to the function
   * @param <U> type of the result of the function
   * @return Function without checked exceptions declared
   */
  public static <T, U> Function<T, U> wrapExceptions(
      ThrowingFunction<T, U> throwingFunction, Function<Throwable, RuntimeException> wrapper) {
    return t -> {
      try {
        return throwingFunction.apply(t);
      } catch (Exception e) {
        throw wrapper.apply(e);
      }
    };
  }

  /**
   * Convert a Function that throws Exception into one that doesn't.
   *
   * <p>Checked exceptions get wrapped in a RuntimeException, runtime exceptions thrown as-is
   *
   * @param throwingFunction Function with checked exceptions declared
   * @param <T> type of the input to the function
   * @param <U> type of the result of the function
   * @return Function without checked exceptions declared
   */
  public static <T, U> Function<T, U> wrapExceptions(ThrowingFunction<T, U> throwingFunction) {
    return wrapExceptions(throwingFunction, FunctionOps::wrapNonRuntimeException);
  }

  private static RuntimeException wrapNonRuntimeException(Throwable t) {
    if (t instanceof RuntimeException) {
      return (RuntimeException) t;
    } else {
      return new RuntimeException(t);
    }
  }
}
