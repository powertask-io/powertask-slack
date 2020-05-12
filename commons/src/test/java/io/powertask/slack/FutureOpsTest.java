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

import static io.powertask.slack.FunctionOps.wrapExceptionsSupplier;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class FutureOpsTest {

  @Test
  public void sequenceSuccessful() throws ExecutionException, InterruptedException {
    List<String> out =
        FutureOps.sequence(
                Arrays.asList(
                    // Make the first one slow, to test ordering stays correct.
                    CompletableFuture.supplyAsync(
                        wrapExceptionsSupplier(
                            () -> {
                              Thread.sleep(30);
                              return "foo";
                            })),
                    CompletableFuture.completedFuture("bar"),
                    CompletableFuture.completedFuture("baz")))
            .toCompletableFuture()
            .get();

    Assertions.assertEquals(Arrays.asList("foo", "bar", "baz"), out);
  }

  @Test
  public void sequenceFailing() throws ExecutionException, InterruptedException {
    ExecutionException e =
        Assertions.assertThrows(
            ExecutionException.class,
            () ->
                FutureOps.sequence(
                        Arrays.asList(
                            CompletableFuture.completedFuture("foo"),
                            CompletableFuture.<String>supplyAsync(
                                () -> {
                                  throw new RuntimeException("BOOM!");
                                }),
                            CompletableFuture.completedFuture("baz")))
                    .toCompletableFuture()
                    .get());

    Assertions.assertEquals("BOOM!", e.getCause().getMessage());
  }

  @Test
  public void traverseSuccessful() throws ExecutionException, InterruptedException {
    List<String> out =
        FutureOps.traverse(
                Arrays.asList("foo", "bar", "baz"),
                in -> CompletableFuture.completedFuture(in.toUpperCase()))
            .toCompletableFuture()
            .get();

    Assertions.assertEquals(Arrays.asList("FOO", "BAR", "BAZ"), out);
  }

  @Test
  public void traverseFailing() throws ExecutionException, InterruptedException {
    ExecutionException e =
        Assertions.assertThrows(
            ExecutionException.class,
            () ->
                FutureOps.traverse(
                        Arrays.asList("foo", "bar", "baz"),
                        in ->
                            CompletableFuture.supplyAsync(
                                () -> {
                                  if (in.equals("bar")) {
                                    throw new RuntimeException("BOOM!");
                                  } else {
                                    return in.toUpperCase();
                                  }
                                }))
                    .toCompletableFuture()
                    .get());

    Assertions.assertEquals("BOOM!", e.getCause().getMessage());
  }

  @Test
  public void collectorSuccessful() throws ExecutionException, InterruptedException {
    List<Integer> out =
        Stream.of(1, 2, 3)
            .map(i -> CompletableFuture.completedFuture(i * i))
            .collect(FutureOps.completionStageListCollector())
            .toCompletableFuture()
            .get();

    Assertions.assertEquals(Arrays.asList(1, 4, 9), out);
  }

  @Test
  public void collectorFailing() throws ExecutionException, InterruptedException {
    ExecutionException e =
        Assertions.assertThrows(
            ExecutionException.class,
            () ->
                Stream.of(1, 2, 3)
                    .map(
                        i ->
                            CompletableFuture.supplyAsync(
                                () -> {
                                  if (i == 2) {
                                    throw new RuntimeException("BOOM!");
                                  } else {
                                    return i;
                                  }
                                }))
                    .collect(FutureOps.completionStageListCollector())
                    .toCompletableFuture()
                    .get());

    Assertions.assertEquals("BOOM!", e.getCause().getMessage());
  }
}
