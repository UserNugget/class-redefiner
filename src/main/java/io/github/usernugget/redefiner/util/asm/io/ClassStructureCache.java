/*
 * Copyright (C) 2024 UserNugget/class-redefiner
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

import io.github.usernugget.redefiner.util.asm.ClassFile;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.objectweb.asm.ClassReader;

public class ClassStructureCache {

  private final Map<Class<?>, ClassFile> cache = new ConcurrentHashMap<>();
  private final ClassSerializer serializer;

  public ClassStructureCache(ClassSerializer serializer) {
    this.serializer = serializer;
  }

  public ClassFile readCached(Class<?> klass) throws IOException {
    ClassFile cached = this.cache.get(klass);
    if (cached != null) {
      return cached;
    }

    cached = this.serializer.readClass(klass, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);
    this.cache.put(klass, cached);
    return cached;
  }
}
