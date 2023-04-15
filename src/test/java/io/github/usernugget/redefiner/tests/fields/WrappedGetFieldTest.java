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

import io.github.usernugget.redefiner.annotation.GetField;
import io.github.usernugget.redefiner.annotation.Mapping;
import io.github.usernugget.redefiner.tests.AbstractTest;
import io.github.usernugget.redefiner.throwable.RedefineFailedException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WrappedGetFieldTest extends AbstractTest {
  @Test
  void testGetField() throws RedefineFailedException {
    assertTrue(Tmp.invoke());
    assertEquals(15, Tmp.invoke_offset());
    REDEFINER.applyMapping(TmpMapping.class);
    assertFalse(Tmp.invoke());
    assertEquals(-1, Tmp.invoke_offset());
  }

  @Mapping(Tmp.class)
  public static final class TmpMapping {
    @GetField(field = "field")
    public static boolean invoke(boolean value) {
      return !value;
    }

    @GetField(field = "field_int", offset = 4)
    public static int invoke_offset(int value) {
      return -value;
    }
  }

  public static final class Tmp {
    public static boolean field = true;
    public static int field_int = 1;

    public static boolean invoke() {
      return field;
    }

    public static int invoke_offset() {
      return field_int + (field_int * 2) + (field_int * 4) + (field_int * 8);
    }
  }
}
