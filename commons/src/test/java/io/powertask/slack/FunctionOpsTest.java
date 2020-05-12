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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class FunctionOpsTest {

  private static final Exception INNER_EXCEPTION = new Exception("BOOM!");
  private static final RuntimeException INNER_RUNTIME_EXCEPTION = new RuntimeException("BOOM!");

  @Test
  void wrapExceptionsSupplierDoesntThrowOnSuccess() {
    FunctionOps.wrapExceptionsSupplier(() -> "OK").get();
  }

  @Test
  void wrapExceptionsSupplierWrapsCheckedExceptions() {
    Supplier<String> supplier =
        FunctionOps.wrapExceptionsSupplier(FunctionOpsTest::exceptionThrowingFunction);

    RuntimeException e = Assertions.assertThrows(RuntimeException.class, supplier::get);
    Assertions.assertEquals(INNER_EXCEPTION, e.getCause());
  }

  @Test
  void wrapExceptionsSupplierDoesntWrapsUncheckedExceptions() {
    Supplier<String> supplier =
        FunctionOps.wrapExceptionsSupplier(FunctionOpsTest::runtimeExceptionThrowingFunction);

    RuntimeException e = Assertions.assertThrows(RuntimeException.class, supplier::get);
    Assertions.assertEquals(INNER_RUNTIME_EXCEPTION, e);
  }

  @Test
  void wrapExceptionsDoesntThrowOnSuccess() {
    FunctionOps.wrapExceptions(() -> "OK");
  }

  @Test
  void wrapExceptionsWrapsCheckedExceptions() {
    RuntimeException e =
        Assertions.assertThrows(
            RuntimeException.class,
            () -> FunctionOps.<String>wrapExceptions(FunctionOpsTest::exceptionThrowingFunction));
    Assertions.assertEquals(INNER_EXCEPTION, e.getCause());
  }

  @Test
  void wrapExceptionsDoesntWrapsUncheckedExceptions() {
    RuntimeException e =
        Assertions.assertThrows(
            RuntimeException.class,
            () ->
                FunctionOps.<String>wrapExceptions(
                    FunctionOpsTest::runtimeExceptionThrowingFunction));
    Assertions.assertEquals(INNER_RUNTIME_EXCEPTION, e);
  }

  @Test
  void wrapExceptionsForFunctionsDoesntThrowOnSuccess() {
    FunctionOps.wrapExceptions((String in) -> "OK");
  }

  @Test
  void wrapExceptionsForFunctionsWrapsCheckedExceptions() {
    Function<String, String> fn =
        FunctionOps.<String, String>wrapExceptions(FunctionOpsTest::exceptionThrowingFunction);

    RuntimeException e = Assertions.assertThrows(RuntimeException.class, () -> fn.apply(""));
    Assertions.assertEquals(INNER_EXCEPTION, e.getCause());
  }

  @Test
  void wrapExceptionsForFunctionsDoesntWrapsUncheckedExceptions() {
    Function<String, String> fn =
        FunctionOps.<String, String>wrapExceptions(
            FunctionOpsTest::runtimeExceptionThrowingFunction);

    RuntimeException e = Assertions.assertThrows(RuntimeException.class, () -> fn.apply(""));
    Assertions.assertEquals(INNER_RUNTIME_EXCEPTION, e);
  }

  private static String exceptionThrowingFunction() throws Exception {
    throw INNER_EXCEPTION;
  }

  private static String exceptionThrowingFunction(String in) throws Exception {
    throw INNER_EXCEPTION;
  }

  private static String runtimeExceptionThrowingFunction() {
    throw INNER_RUNTIME_EXCEPTION;
  }

  private static String runtimeExceptionThrowingFunction(String in) {
    throw INNER_RUNTIME_EXCEPTION;
  }
}
