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

package io.github.usernugget.redefiner.transformer;

import io.github.usernugget.redefiner.util.asm.ClassIO;
import io.github.usernugget.redefiner.util.asm.CodeGenerator;
import io.github.usernugget.redefiner.util.asm.node.ClassFile;
import org.objectweb.asm.ClassWriter;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public class ClassTransformer implements ClassFileTransformer {
  public static final ClassTransformer I = new ClassTransformer();

  private final Map<String, List<ClassTransformWrapper>> pending = new ConcurrentHashMap<>();

  @Override
  public byte[] transform(ClassLoader classLoader, String className, Class<?> javaClass,
                          ProtectionDomain protectionDomain, byte[] classFile) {
    return rewriteClass(classLoader, className, classFile);
  }

  public ClassTransformWrapper enqueue(String internalName, BiConsumer<ClassFile, CodeGenerator> transform) {
    ClassTransformWrapper wrapper = new ClassTransformWrapper(transform);
    this.pending.computeIfAbsent(internalName, key -> new ArrayList<>()).add(wrapper);
    return wrapper;
  }

  private byte[] rewriteClass(ClassLoader classLoader, String className, byte[] classBytes) {
    if (className == null) {
      return null;
    }

    List<ClassTransformWrapper> wrappers = this.pending.remove(className);
    if (wrappers == null) {
      return null;
    }

    try {
      CodeGenerator generator = new CodeGenerator();
      ClassFile owner = ClassIO.fromByteArray(classBytes);
      for (ClassTransformWrapper wrapper : wrappers) {
        wrapper.transform(owner, generator);
      }

      generator.define(classLoader);

      return ClassIO.writeClass(owner, classLoader, ClassWriter.COMPUTE_FRAMES);
    } catch (Throwable throwable) {
      for (ClassTransformWrapper wrapper : wrappers) {
        if(wrapper.getThrowable() != null) {
          wrapper.setThrowable(throwable);
        }
      }

      return new byte[2];
    }
  }
}
