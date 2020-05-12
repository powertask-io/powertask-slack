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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collector;

public class FutureOps {

  public static <T> CompletionStage<List<T>> sequence(List<CompletionStage<T>> list) {
    return traverse(list, Function.identity());
  }

  public static <T, U> CompletionStage<List<U>> traverse(
      List<T> list, Function<T, CompletionStage<U>> fn) {
    return list.stream().map(fn).collect(completionStageListCollector());
  }

  public static <X>
      Collector<
              CompletionStage<X>,
              AtomicReference<CompletionStage<ArrayList<X>>>,
              CompletionStage<List<X>>>
          completionStageListCollector() {
    return Collector.of(
        () -> new AtomicReference<>(CompletableFuture.completedFuture(new ArrayList<>())),
        (ref, cse) ->
            ref.updateAndGet(
                csl ->
                    csl.thenCombine(
                        cse,
                        (l, e) -> {
                          l.add(e);
                          return l;
                        })),
        (ref1, ref2) -> {
          ref1.updateAndGet(
              csl1 ->
                  csl1.thenCombine(
                      ref2.get(),
                      (l1, l2) -> {
                        l1.addAll(l2);
                        return l1;
                      }));
          return ref1;
        },
        ref3 -> ref3.get().thenApply(Function.identity()),
        Collector.Characteristics.CONCURRENT);
  }
}
