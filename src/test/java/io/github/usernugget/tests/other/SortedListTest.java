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

package io.github.usernugget.tests.other;

import io.github.usernugget.redefiner.util.collections.SortedList;
import java.util.Iterator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SortedListTest {
  @Test
  void testOrder() {
    SortedList<Integer> sortedList = new SortedList<>();

    Integer[] values = new Integer[1001];
    for (int i = 0; i <= 1000; i++) {
      values[i] = i;
      sortedList.add(i, i);
    }

    Iterator<Integer> iterator = sortedList.getValues().values().iterator();
    for (int i = 1000; i >= 0; i--) {
      assertEquals(values[i], iterator.next(), "value " + i);
    }
  }
}
