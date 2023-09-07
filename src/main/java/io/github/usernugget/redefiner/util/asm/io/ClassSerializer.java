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

package io.github.usernugget.redefiner.util.asm.io;

import io.github.usernugget.redefiner.ClassRedefiner;
import io.github.usernugget.redefiner.agent.AbstractAgent;
import io.github.usernugget.redefiner.util.JavaInternals;
import io.github.usernugget.redefiner.util.asm.ClassFile;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

public class ClassSerializer {
  private ClassRedefiner redefiner;

  public ClassSerializer(ClassRedefiner redefiner) {
    this.redefiner = redefiner;
  }

  public byte[] dumpClass(Class<?> klass) throws IOException {
    AbstractAgent agent = this.redefiner.getAgent();
    if (agent.isAccessible(klass)) {
      return agent.dumpClass(klass);
    }

    ClassLoader classLoader = klass.getClassLoader();
    if (classLoader == null) {
      classLoader = ClassLoader.getPlatformClassLoader();
    }

    ByteArrayOutputStream classBytes = new ByteArrayOutputStream(8192);
    InputStream classResource = classLoader.getResourceAsStream(
       "/" + klass.getName().replace('.', '/') + ".class"
    );

    int count;
    byte[] buffer = new byte[8192];
    while ((count = classResource.read(buffer, 0, buffer.length)) >= 0) {
      classBytes.write(buffer, 0, count);
    }

    return classBytes.toByteArray();
  }

  public ClassFile readClass(Class<?> klass, int parseOptions) throws IOException {
    return readClass(dumpClass(klass), parseOptions);
  }

  public ClassFile readClass(byte[] classBytes, int parseOptions) {
    return readClass(new ClassReader(classBytes), parseOptions);
  }

  public ClassFile readClass(InputStream in, int parseOptions) throws IOException {
    return readClass(new ClassReader(in), parseOptions);
  }

  public ClassFile readClass(ClassReader classReader, int parseOptions) {
    ClassFile classFile = new ClassFile();
    classReader.accept(classFile, parseOptions);
    return classFile;
  }

  public byte[] writeClass(ClassFile classFile, ClassLoader classLoader, int writeOptions) {
    return writeClass(classFile, new CustomClassWriter(writeOptions, classLoader));
  }

  public byte[] writeClass(ClassFile classFile, ClassWriter writer) {
    classFile.accept(writer);
    return writer.toByteArray();
  }

  public Class<?> defineClass(ClassFile classFile, ClassLoader classLoader) {
    return JavaInternals.defineClass(
       classFile, classLoader, writeClass(classFile, classLoader, ClassWriter.COMPUTE_FRAMES)
    );
  }

  public static class CustomClassWriter extends ClassWriter {
    protected ClassLoader classLoader;

    public CustomClassWriter(int flags, ClassLoader classLoader) {
      super(flags);
      this.classLoader = Objects.requireNonNullElse(
        classLoader, ClassLoader.getPlatformClassLoader()
      );
    }

    @Override
    protected ClassLoader getClassLoader() {
      return this.classLoader;
    }
  }
}
