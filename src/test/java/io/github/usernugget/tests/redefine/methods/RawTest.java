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
import io.github.usernugget.redefiner.changes.MethodChange;
import io.github.usernugget.redefiner.handlers.types.annotations.Raw;
import io.github.usernugget.redefiner.util.asm.ClassMethod;
import io.github.usernugget.redefiner.util.asm.instruction.Insns;
import io.github.usernugget.tests.redefine.AbstractRedefineTest;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class RawTest extends AbstractRedefineTest {
  public static class PlainClass {
    public int field;

    public int test() {
      return 0;
    }

    public static int testStatic() {
      return 0;
    }
  }

  @Mapping(targetClass = PlainClass.class)
  public static final class PlainClassMapping {
    public int field;

    @Raw(method = "<init>")
    public static void initializer(MethodChange change) {
      ClassMethod target = change.findTargetMethod();
      Insns targetCode = target.getInstructions();

      targetCode.clear();

      // super();
      // this.field = 2;
      // return;
      targetCode.varOp(Opcodes.ALOAD, 0);
      targetCode.methodOp(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
      targetCode.varOp(Opcodes.ALOAD, 0);
      targetCode.ldc(2);
      targetCode.fieldOp(Opcodes.PUTFIELD, target.owner.findField("field", "I"));
      targetCode.op(Opcodes.RETURN);
    }

    @Raw
    public static void test(MethodChange change) {
      testStatic(change);
    }

    @Raw
    public static void testStatic(MethodChange change) {
      ClassMethod target = change.findTargetMethod();
      Insns targetCode = target.getInstructions();

      targetCode.clear();

      // return 2;
      targetCode.ldc(2);
      targetCode.op(Opcodes.IRETURN);
    }
  }

  @Test
  void testInjection() throws ClassNotFoundException {
    REDEFINER.transformClass(PlainClassMapping.class);

    assertEquals(2, PlainClass.testStatic());

    PlainClass value = new PlainClass();
    assertEquals(2, value.field);
    assertEquals(2, value.test());
  }
}
