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
import io.github.usernugget.redefiner.handlers.Op;
import io.github.usernugget.redefiner.handlers.types.annotations.Head;
import io.github.usernugget.tests.redefine.AbstractRedefineTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class OpTest extends AbstractRedefineTest {
  public static class PlainClass {
    public static int field;

    public static void   testVoid  () { field = 2;           }
    public static boolean testBoolean() { return false;      }
    public static byte   testByte()   { return (byte) 2;     }
    public static short  testShort()  { return (short) 2;    }
    public static char   testChar()   { return '2';          }
    public static int    testInt   () { return 2;            }
    public static float  testFloat () { return 2;            }
    public static long   testLong  () { return 2;            }
    public static double testDouble() { return 2;            }
    public static Object testObject() { return new Object(); }
  }

  @Mapping(targetClass = PlainClass.class)
  public static final class PlainClassMapping {
    public static int field;

    @Head public static void testVoid  () { field = 4; Op.returnOp(); }
    @Head public static void testBoolean() { Op.returnOp(true);       }
    @Head public static void testByte()   { Op.returnOp((byte) 4);    }
    @Head public static void testShort()  { Op.returnOp((short) 4);   }
    @Head public static void testChar()   { Op.returnOp('4');         }
    @Head public static void testInt   () { Op.returnOp(4);           }
    @Head public static void testFloat () { Op.returnOp(4);           }
    @Head public static void testLong  () { Op.returnOp(4);           }
    @Head public static void testDouble() { Op.returnOp(4);           }
    @Head public static void testObject() { Op.returnOp(null);        }
  }

  @Test
  void testInjection() throws ClassNotFoundException {
    REDEFINER.transformClass(PlainClassMapping.class);

    PlainClass.testVoid();
    assertEquals(4, PlainClass.field);
    assertNull(PlainClass.testObject());

    assertEquals(true, PlainClass.testBoolean());
    assertEquals((byte) 4, PlainClass.testByte());
    assertEquals((short) 4, PlainClass.testShort());
    assertEquals('4', PlainClass.testChar());
    assertEquals(4, PlainClass.testInt());
    assertEquals(4, PlainClass.testFloat());
    assertEquals(4, PlainClass.testLong());
    assertEquals(4, PlainClass.testDouble());
  }
}
