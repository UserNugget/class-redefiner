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

package io.github.usernugget.tests.redefine.methods;

import io.github.usernugget.redefiner.Mapping;
import io.github.usernugget.redefiner.handlers.types.annotations.SetField;
import io.github.usernugget.tests.redefine.AbstractRedefineTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class SetFieldTest extends AbstractRedefineTest {
  public static class PlainClass {
    public static Object staticObject;
    public Object fieldObject;

    public static int staticInt;
    public int fieldInt;

    public int multipleFieldInt() {
      this.fieldInt = 1;
      this.fieldInt += 2;
      this.fieldInt += 4;
      this.fieldInt += 8;
      this.fieldInt += 16;
      return this.fieldInt;
    }

    public int fieldInt() {
      this.fieldInt = 2;
      return this.fieldInt;
    }

    public static int staticInt() {
      staticInt = 2;
      return staticInt;
    }

    public Object fieldObject(Object o) {
      this.fieldObject = o;
      return this.fieldObject;
    }

    public static Object staticObject(Object o) {
      staticObject = o;
      return staticObject;
    }
  }

  @Mapping(targetClass = PlainClass.class)
  public static final class PlainClassMapping {
    @SetField(field = "fieldInt", offset = 3)
    public static int multipleFieldInt(int value) {
      try {
        return 0; // iload + iinc instructions
      } catch (Throwable throwable) {
        throw new Error(throwable);
      } finally { }
    }

    @SetField(field = "fieldInt")
    public static int fieldInt(int value) {
      try {
        return value + ++value; // iload + iinc instructions
      } catch (Throwable throwable) {
        throw new Error(throwable);
      } finally { }
    }

    @SetField(field = "staticInt")
    public static int staticInt(int value) {
      try {
        return value + ++value; // iload + iinc instructions
      } catch (Throwable throwable) {
        throw new Error(throwable);
      } finally { }
    }

    @SetField(field = "fieldObject")
    public static Object fieldObject(Object value) {
      try {
        return new Object();
      } catch (Throwable throwable) {
        throw new Error(throwable);
      } finally { }
    }

    @SetField(field = "staticObject")
    public static Object staticObject(Object value) {
      try {
        return new Object();
      } catch (Throwable throwable) {
        throw new Error(throwable);
      } finally { }
    }
  }

  @Test
  void testInjection() throws ClassNotFoundException {
    REDEFINER.transformClass(PlainClassMapping.class);

    Object o = new Object();
    PlainClass plain = new PlainClass();

    assertEquals(5, PlainClass.staticInt()); // Redefined
    assertNotEquals(o, PlainClass.staticObject(o)); // Redefined

    assertEquals(24, plain.multipleFieldInt()); // Redefined
    assertEquals(5, plain.fieldInt()); // Redefined
    assertNotEquals(o, plain.fieldObject(o)); // Redefined
  }
}
