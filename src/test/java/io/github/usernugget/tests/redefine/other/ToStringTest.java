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

package io.github.usernugget.tests.redefine.other;

import io.github.usernugget.redefiner.util.asm.ClassFile;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ToStringTest {
  @Test
  void testMethod() {
    assertEquals(
      "a/b/c::test(Ltest;)V",
      tmpClass().findMethod("test", "(Ltest;)V").toString()
    );
  }

  @Test
  void testField() {
    assertEquals(
      "Ltest; a/b/c.test",
      tmpClass().findField("test", "Ltest;").toString()
    );
  }

  @Test
  void testClass() {
    assertEquals("a/b/c", tmpClass().toString());
  }

  private static ClassFile tmpClass() {
    ClassFile file = new ClassFile(
      Opcodes.ACC_PUBLIC, "a/b/c"
    );

    file.visitMethod(Opcodes.ACC_PUBLIC, "test", "(Ltest;)V");
    file.visitField(Opcodes.ACC_PUBLIC, "test", "Ltest;");

    return file;
  }
}
