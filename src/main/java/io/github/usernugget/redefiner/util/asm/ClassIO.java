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

package io.github.usernugget.redefiner.util.asm;

import io.github.usernugget.redefiner.util.JavaInternals;
import io.github.usernugget.redefiner.util.asm.node.ClassFile;
import java.io.IOException;
import java.io.InputStream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

public class ClassIO {
  private static final class CustomClassWriter extends ClassWriter {
    private final ClassLoader classLoader;

    CustomClassWriter(int flags, ClassLoader classLoader) {
      super(flags);
      this.classLoader = classLoader;
    }

    @Override
    protected ClassLoader getClassLoader() {
      return this.classLoader;
    }
  }

  public static ClassFile fromClass(ClassLoader classLoader, String className) {
    try {
      return fromClass(Class.forName(className, false, classLoader));
    } catch (Throwable throwable) {
      throw new IllegalStateException("failed to read " + className, throwable);
    }
  }

  public static ClassFile fromClass(Class<?> klass) {
    return fromClass(klass, ClassReader.SKIP_FRAMES);
  }

  public static ClassFile fromClass(Class<?> klass, int options) {
    try {
      ClassLoader classLoader = klass.getClassLoader();
      if (classLoader == null) {
        classLoader = ClassLoader.getSystemClassLoader();
      }

      InputStream classResource = classLoader.getResourceAsStream(klass.getName().replace('.', '/') + ".class");
      return fromInputStream(classResource, options);
    } catch (Throwable throwable) {
      throw new IllegalStateException("failed to read " + klass.getName(), throwable);
    }
  }

  public static ClassFile fromByteArray(byte[] bytes) {
    return fromClassReader(new ClassReader(bytes));
  }

  public static ClassFile fromInputStream(InputStream is) throws IOException {
    return fromInputStream(is, ClassReader.SKIP_FRAMES);
  }

  public static ClassFile fromInputStream(InputStream is, int options) throws IOException {
    return fromClassReader(new ClassReader(is), options);
  }

  public static ClassFile fromClassReader(ClassReader classReader) {
    return fromClassReader(classReader, ClassReader.SKIP_FRAMES);
  }

  public static ClassFile fromClassReader(ClassReader classReader, int options) {
    ClassFile classFile = new ClassFile();
    classReader.accept(classFile, options);
    return classFile;
  }

  public static byte[] writeClass(ClassFile classFile, ClassLoader classLoader, int writeFlags) {
    CustomClassWriter classWriter = new CustomClassWriter(writeFlags, classLoader);
    classFile.accept(classWriter);
    return classWriter.toByteArray();
  }

  public static Class<?> define(ClassFile classFile, ClassLoader classLoader) {
    return define(classFile, classLoader, ClassWriter.COMPUTE_FRAMES);
  }

  public static Class<?> define(ClassFile classFile, ClassLoader classLoader, int writeFlags) {
    byte[] classBytes = writeClass(classFile, classLoader, writeFlags);
    try {
      return JavaInternals.defineClass(classFile.name, classBytes, 0, classBytes.length, classLoader, null);
    } catch (Throwable throwable) {
      throw new Error("failed to define class:\n" + classFile.toBytecodeString(), throwable) {
        @Override
        public synchronized Throwable fillInStackTrace() {
          return this;
        }
      };
    }
  }
}