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

package kk.tests.impl;

import kk.redefiner.annotation.GetField;
import kk.redefiner.annotation.Mapping;
import kk.redefiner.annotation.PutField;
import kk.redefiner.throwable.RedefineFailedException;
import kk.tests.AbstractTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestFields extends AbstractTest {
  @Test
  void test() throws RedefineFailedException {
    assertEquals(1, Tmp.getFieldAccess());
    assertEquals(0, Tmp.getFieldMultipleAccess());

    assertEquals(2, Tmp.putFieldAccess(2));
    assertEquals(2, Tmp.putFieldMultipleAccess(5));

    REDEFINER.applyMapping(TmpMapping.class);
    assertEquals(1, Tmp.getFieldAccess());
    assertEquals(-1, Tmp.getFieldMultipleAccess());

    assertEquals(1, Tmp.putFieldAccess(2));
    assertEquals(1, Tmp.putFieldMultipleAccess(5));
  }

  @Mapping(Tmp.class)
  public static final class TmpMapping {
    @GetField(field = "FIELD")
    public static int getFieldAccess(int currentValue) {
      return currentValue - 1;
    }

    @GetField(field = "FIELD", offset = 2)
    public static int getFieldMultipleAccess(int currentValue) {
      return currentValue - 1;
    }

    @PutField(field = "FIELD")
    public static int putFieldAccess(int currentValue) {
      return currentValue - 1;
    }

    @PutField(field = "FIELD", offset = 3)
    public static int putFieldMultipleAccess(int currentValue) {
      return currentValue - 1;
    }
  }

  public static final class Tmp {
    public static int FIELD = 1;

    public static int getFieldAccess() {
      return FIELD;
    }

    public static int getFieldMultipleAccess() {
      int field = FIELD + FIELD;
      return field - (FIELD + FIELD);
    }

    public static int putFieldAccess(int v) {
      FIELD = v;
      return FIELD;
    }

    public static int putFieldMultipleAccess(int v) {
      FIELD = v;
      FIELD = FIELD - 1;
      FIELD = FIELD - 2;

      return FIELD;
    }
  }
}
