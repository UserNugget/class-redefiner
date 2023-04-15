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

package io.github.usernugget.redefiner.util.cache;

import io.github.usernugget.redefiner.util.asm.ClassIO;
import io.github.usernugget.redefiner.util.asm.node.ClassFile;
import java.util.IdentityHashMap;
import java.util.Map;
import org.objectweb.asm.ClassReader;

public class ClassCache {
  private final Map<Class<?>, ClassFile> classMap = new IdentityHashMap<>();
  private ClassLoader classLoader;

  public ClassCache setClassLoader(ClassLoader classLoader) {
    this.classLoader = classLoader;
    return this;
  }

  public ClassFile findClass(String name) {
    if (name == null) {
      return null;
    }

    try {
      // TODO: test it with ProtectionDomain?
      return findClass(Class.forName(name, false, this.classLoader));
    } catch (ClassNotFoundException throwable) {
      return null;
    }
  }


  public ClassFile findClass(Class<?> klass) {
    if (klass == null) {
      return null;
    }

    return this.classMap.computeIfAbsent(
       klass, key -> ClassIO.fromClass(key, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG)
    );
  }
}
