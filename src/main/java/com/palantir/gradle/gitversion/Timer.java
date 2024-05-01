/*
 * (c) Copyright 2020 Palantir Technologies Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.palantir.gradle.gitversion;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.annotations.MustBeClosed;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

final class Timer {
    private final ConcurrentMap<String, Long> totalElapsedMillis = new ConcurrentHashMap<>();

    @MustBeClosed
    public Context start(String name) {
        Stopwatch stopwatch = Stopwatch.createStarted();

        return () -> {
            long elapsedMillis = stopwatch.elapsed(TimeUnit.MILLISECONDS);

            totalElapsedMillis.compute(
                    name, (_ignored, previousValue) -> elapsedMillis + (previousValue == null ? 0 : previousValue));
        };
    }

    public String toJson() {
        Map<String, Long> withTotal = ImmutableMap.<String, Long>builder()
                .putAll(totalElapsedMillis)
                .put("total", totalMillis())
                .build();

        return JsonUtils.mapToJson(withTotal);
    }

    public long totalMillis() {
        return totalElapsedMillis.values().stream().mapToLong(time -> time).sum();
    }

    interface Context extends AutoCloseable {
        @Override
        void close();
    }
}
