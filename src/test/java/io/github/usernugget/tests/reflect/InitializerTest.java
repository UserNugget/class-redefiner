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

package io.github.usernugget.tests.reflect;

import io.github.usernugget.redefiner.util.JavaInternals;
import io.github.usernugget.redefiner.util.asm.ClassFile;
import io.github.usernugget.redefiner.util.asm.ClassMethod;
import io.github.usernugget.redefiner.util.asm.instruction.Insns;
import io.github.usernugget.redefiner.util.asm.reflect.Reflection;
import io.github.usernugget.tests.redefine.AbstractRedefineTest;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.RETURN;

public class InitializerTest extends AbstractRedefineTest {
  @Test
  void testInitializer() throws Throwable {
    ClassFile klass = new ClassFile(ACC_PUBLIC, "a/b/c/d/Cla$$");
    ClassMethod initializer = klass.visitSimpleInitializer();

    ClassMethod method = klass.visitMethod(
      ACC_PRIVATE | ACC_STATIC,
      "STATIC_METHOD", "()V"
    );

    method.getInstructions().op(RETURN);

    JavaInternals.defineClass(
      klass, InitializerTest.class.getClassLoader(),
      REDEFINER.getClassSerializer().writeClass(
        klass, InitializerTest.class.getClassLoader(), ClassWriter.COMPUTE_FRAMES
      )
    );

    Reflection generator = new Reflection();
    ClassMethod wrapper = generator.wrapMethod(method);

    ClassFile invoker = new ClassFile(ACC_PUBLIC, "a/b/c/d/Class");
    ClassMethod invokeMethod = invoker.visitMethod(
      ACC_PUBLIC | ACC_STATIC, "invoke", "()V"
    );
    Insns invoke = invokeMethod.getInstructions();

    invoke.typeOp(NEW, klass.name);
    invoke.op(Opcodes.DUP);
    invoke.invoke(initializer);
    invoke.invoke(wrapper);
    invoke.op(RETURN);

    generator.defineClasses(
      REDEFINER.getClassSerializer(),
      InitializerTest.class.getClassLoader(),
      InitializerTest.class.getClassLoader()
    );

    Class<?> definedInvoker = JavaInternals.defineClass(
      invoker, InitializerTest.class.getClassLoader(),
      REDEFINER.getClassSerializer().writeClass(
        invoker, InitializerTest.class.getClassLoader(), ClassWriter.COMPUTE_FRAMES
      )
    );

    definedInvoker.getDeclaredMethod("invoke").invoke(null);
  }
}
