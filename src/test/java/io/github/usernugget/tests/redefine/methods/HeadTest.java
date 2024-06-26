/*
 * Copyright (C) 2024 UserNugget/class-redefiner
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

package io.github.usernugget.tests.redefine.methods;

import io.github.usernugget.redefiner.Mapping;
import io.github.usernugget.redefiner.handlers.Op;
import io.github.usernugget.redefiner.handlers.types.annotations.Head;
import io.github.usernugget.tests.redefine.AbstractRedefineTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class HeadTest extends AbstractRedefineTest {
  public static class PlainClass {
    public int field;

    public int test(boolean value) {
      return 0;
    }

    public static int testStatic(boolean value) {
      return 0;
    }

    public static Object testReturn(boolean value) {
      return new Object();
    }

    public static long testBigStack(boolean value) {
      return 0;
    }
  }

  @Mapping(targetClass = PlainClass.class)
  public static final class PlainClassMapping {
    public int field;

    @Head(method = "<init>")
    public void initializer() {
      try {
          this.field = 4;
      } catch (Throwable throwable) {
        throw new Error(throwable);
      } finally { }
    }

    @Head
    public void test(boolean value) {
      try {
        if (value) {
          Op.returnOp(1);
        }
      } catch (Throwable throwable) {
        throw new Error(throwable);
      } finally { }
    }

    @Head
    public static void testStatic(boolean value) {
      try {
        if (value) {
          Op.returnOp(1);
        }
      } catch (Throwable throwable) {
        throw new Error(throwable);
      } finally { }
    }

    @Head
    public static Object testReturn(boolean value) {
      try {
        if (value) {
          Op.returnOp(null);
        }
      } catch (Throwable throwable) {
        throw new Error(throwable);
      } finally { }

      return null;
    }

    @Head
    public static long testBigStack(boolean value) {
      try {
        if (value) {
          Op.returnOp(1);
        }
      } catch (Throwable throwable) {
        throw new Error(throwable);
      } finally { }

      return 0;
    }
  }

  @Test
  void testInjection() throws ClassNotFoundException {
    REDEFINER.transformClass(PlainClassMapping.class);

    assertEquals(1, PlainClass.testStatic(true)); // Redefined
    assertEquals(0, PlainClass.testStatic(false)); // Original

    assertNull(PlainClass.testReturn(true)); // Redefined
    assertNotNull(PlainClass.testReturn(false)); // Original

    assertEquals(1, PlainClass.testBigStack(true)); // Redefined
    assertEquals(0, PlainClass.testBigStack(false)); // Original

    PlainClass value = new PlainClass();

    assertEquals(4, value.field); // Redefined
    assertEquals(1, value.test(true)); // Redefined
    assertEquals(0, value.test(false)); // Original
  }
}
