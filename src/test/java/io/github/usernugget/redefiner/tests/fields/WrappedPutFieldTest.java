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

package io.github.usernugget.redefiner.tests.fields;

import io.github.usernugget.redefiner.annotation.Mapping;
import io.github.usernugget.redefiner.annotation.PutField;
import io.github.usernugget.redefiner.tests.AbstractTest;
import io.github.usernugget.redefiner.throwable.RedefineFailedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class WrappedPutFieldTest extends AbstractTest {
  @Test
  void testPutField() throws RedefineFailedException {
    assertTrue(Tmp.invoke(true));
    assertEquals(15, Tmp.invoke_offset(1));
    REDEFINER.applyMapping(TmpMapping.class);
    assertFalse(Tmp.invoke(true));
    assertEquals(-15, Tmp.invoke_offset(1));
  }

  @Mapping(Tmp.class)
  public static final class TmpMapping {
    static boolean field;

    @PutField(field = "field")
    public static boolean invoke(boolean value) {
      return !value;
    }

    @PutField(field = "field_int", offset = 4)
    public static int invoke_offset(int value) {
      return -value;
    }
  }

  public static final class Tmp {
    public static boolean field = true;
    public static int field_int;

    public static boolean invoke(boolean value) {
      field = value;
      return field;
    }

    public static int invoke_offset(int value) {
      field_int = value;
      field_int += value * 2;
      field_int += value * 4;
      field_int += value * 8;
      return field_int;
    }
  }
}
