/*
 * Copyright (C) 2023 UserNugget/class-redefiner
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.usernugget.redefiner.util.collections;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

public class SortedList<T> {
  private static final Comparator<Integer> SORTER =
    Comparator.comparingInt(priority -> -priority); // 1000, 999, 998, 997 ... 0

  private final Map<Integer, T> values;

  public SortedList() {
    this(new TreeMap<>(SORTER));
  }

  public SortedList(Map<Integer, T> values) {
    this.values = values;
  }

  public Map<Integer, T> getValues() {
    return this.values;
  }

  public boolean add(int priority, T value) {
    if (priority < 0) {
      throw new IllegalStateException("priority should be >0");
    }

    return this.values.putIfAbsent(priority, value) == null;
  }

  public boolean remove(int priority, T value) {
    Iterator<Entry<Integer, T>> iterator = this.values.entrySet().iterator();
    while (iterator.hasNext()) {
      Entry<Integer, T> entry = iterator.next();

      if (entry.getKey() == priority && entry.getValue() == value) {
        iterator.remove();
        return true;
      }
    }

    return false;
  }

}
