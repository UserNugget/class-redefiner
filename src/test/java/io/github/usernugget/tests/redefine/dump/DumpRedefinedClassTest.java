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

package io.github.usernugget.tests.redefine.dump;

import io.github.usernugget.redefiner.util.asm.ClassFile;
import io.github.usernugget.redefiner.util.asm.ClassMethod;
import io.github.usernugget.redefiner.util.asm.instruction.Insns;
import io.github.usernugget.tests.redefine.AbstractRedefineTest;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DumpRedefinedClassTest extends AbstractRedefineTest {
  public static final class TestClass {
    public static int test() {
      return 1;
    }
  }

  @Test
  void testDumpClass() throws IOException {
    REDEFINER.getAgent().rewriteClass(TestClass.class, (classData, classLoader) -> {
      ClassFile klass = REDEFINER.getClassSerializer().readClass(
        classData, ClassReader.SKIP_FRAMES
      );
      ClassMethod method = klass.findMethod("test", "()I");
      Insns insns = method.getInstructions();

      insns.clear();
      insns.op(Opcodes.ICONST_0);
      insns.op(Opcodes.IRETURN);

      return REDEFINER.getClassSerializer().writeClass(
         klass, classLoader, ClassWriter.COMPUTE_FRAMES
      );
    });

    assertEquals(0, TestClass.test());

    ClassFile newClass = REDEFINER.getClassSerializer().readClass(
      TestClass.class,
      ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES
    );
    ClassMethod method = newClass.findMethod("test", "()I");

    assertEquals(Opcodes.ICONST_0, method.instructions.getLast().getPrevious().getOpcode());
    assertEquals(Opcodes.IRETURN, method.instructions.getLast().getOpcode());
  }
}
