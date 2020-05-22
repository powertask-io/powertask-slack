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
package io.powertask.slack.identity;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

// Extremely limited cache implementation.
public class CachingResolver implements UserResolver {

  private final UserResolver underlying;

  // Used for lookups in both directions.
  private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

  public CachingResolver(UserResolver underlying) {
    this.underlying = underlying;
  }

  @Override
  public String toSlackUserId(String engineUserId) {
    return cachedLookup(engineUserId, underlying::toSlackUserId);
  }

  @Override
  public String toEngineUserId(String slackUserId) {
    return cachedLookup(slackUserId, underlying::toEngineUserId);
  }

  private String cachedLookup(String key, Function<String, String> lookup) {
    return Optional.ofNullable(cache.get(key))
        .orElseGet(
            () -> {
              String value = lookup.apply(key);
              cache.put(key, value);
              return value;
            });
  }
}
