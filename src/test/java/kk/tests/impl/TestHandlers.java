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

import kk.redefiner.annotation.Head;
import kk.redefiner.annotation.Inject;
import kk.redefiner.annotation.Mapping;
import kk.redefiner.annotation.Raw;
import kk.redefiner.annotation.Replace;
import kk.redefiner.annotation.Tail;
import kk.redefiner.annotation.Var;
import kk.redefiner.op.Op;
import kk.redefiner.throwable.RedefineFailedException;
import kk.redefiner.util.asm.CodeGenerator;
import kk.redefiner.util.asm.node.Insts;
import kk.tests.AbstractTest;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestHandlers extends AbstractTest {
  @Test
  void redefineVariable() throws RedefineFailedException {
    assertEquals(6, Tmp.var(1, 1));

    REDEFINER.applyMapping(VarMapping.class);
    assertEquals(-2, Tmp.var(1, 1));
  }

  @Test
  void redefineMultipleHandlers() throws RedefineFailedException {
    REDEFINER.applyMapping(TmpMapping.class);

    assertTrue(Tmp.head());
    assertTrue(Tmp.tail());
    assertTrue(Tmp.replace());
    assertTrue(Tmp.insert());
    assertTrue(Tmp.raw());
    assertTrue(Tmp.varargs(false));
  }

  @Test
  void redefineMultipleMappings() throws RedefineFailedException {
    REDEFINER.applyMapping(TrueMapping.class);
    assertTrue(Tmp.multiple());

    REDEFINER.applyMapping(FalseMapping.class);
    assertFalse(Tmp.multiple());
  }

  @Test
  void testRawName() throws RedefineFailedException {
    REDEFINER.applyMapping(TmpStringMapping.class);
    assertTrue(Tmp.type());
  }

  @Mapping(value = Tmp.class, verifyCode = true)
  public static final class VarMapping {
    @Tail
    public static void var(int a, @Var int b, @Var(name = "c") int s, @Var(raw = 3) int o) {
      int d = 0;
      d += s; d += o;
      Op.returnValue(a + b - d);
    }
  }

  @Mapping(raw = "kk.tests.impl.TestHandlers$Tmp", verifyCode = true)
  public static final class TmpStringMapping {
    @Head
    public static void type() {
      Op.returnValue(true);
    }
  }

  @Mapping(value = Tmp.class, verifyCode = true)
  public static final class TmpMapping {
    @Head
    public static void head() {
      Op.returnValue(true);
    }

    @Tail
    public static void tail() {
      Op.returnValue(true);
    }

    @Head
    public static void insert() {
      if (newMethod()) {
        Op.returnValue(true);
      }
    }

    @Inject
    public static boolean newMethod() {
      return true;
    }

    @Replace
    public static boolean replace() {
      return true;
    }

    @Raw
    public static void raw(CodeGenerator generator, Insts insts) {
      insts.clear();

      insts.op(Opcodes.ICONST_1);
      insts.op(Opcodes.IRETURN);
    }

    @Head
    public static void varargs(boolean... varargs) {
      Op.returnValue(!varargs[0]);
    }
  }

  @Mapping(value = Tmp.class, verifyCode = true)
  public static final class TrueMapping {
    @Replace
    public static boolean multiple() {
      return true;
    }
  }

  @Mapping(value = Tmp.class, verifyCode = true)
  public static final class FalseMapping {
    @Replace
    public static boolean multiple() {
      return false;
    }
  }

  public static final class Tmp {
    public static boolean type() { return false; }
    public static boolean head() { return false; }
    public static boolean tail() { return false; }
    public static boolean replace() { return false; }
    public static boolean insert() { return false; }
    public static boolean raw() { return false; }
    public static boolean multiple() { return false; }
    public static boolean varargs(boolean... varargs) { return varargs[0]; }

    public static int var(int a, int b) {
      int c = a + 1;
      int d = b + 1;

      return a + b + c + d;
    }
  }
}
