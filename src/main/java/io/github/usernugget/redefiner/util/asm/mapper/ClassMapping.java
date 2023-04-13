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

package io.github.usernugget.redefiner.util.asm.mapper;

import io.github.usernugget.redefiner.util.asm.node.ClassFile;
import io.github.usernugget.redefiner.util.asm.node.ClassMethod;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.RecordComponentNode;
import java.util.HashMap;
import java.util.Map;

public class ClassMapping {
  public final Map<String, String> classMapping = new HashMap<>();
  public final Map<String, String> recordComponentMapping = new HashMap<>();
  public final Map<String, String> methodMapping = new HashMap<>();
  public final Map<String, String> fieldMapping = new HashMap<>();
  public final Map<String, String> stringMapping = new HashMap<>();

  public ClassMapping classMapping(String from, String to) {
    this.classMapping.put(from, to);
    return this;
  }

  public ClassMapping classMapping(Class<?> from, String to) {
    return classMapping(from.getName().replace(".", "/"), to);
  }

  public ClassMapping classMapping(ClassFile from, String to) {
    return classMapping(from.name, to);
  }

  public ClassMapping recordComponentMapping(String from, String to) {
    this.recordComponentMapping.put(from, to);
    return this;
  }

  public ClassMapping recordComponentMapping(ClassFile node, RecordComponentNode from, String to) {
    return recordComponentMapping(node.name + from.name + from.descriptor, to);
  }

  public ClassMapping methodMapping(ClassFile owner, ClassMethod from, String to) {
    if (owner.isAnnotation()) {
      this.methodMapping.put(MappingRemapper.methodDesc(owner.name, from.name, "(annotation)"), to);
    }

    this.methodMapping.put(MappingRemapper.methodDesc(owner, from), to);
    return this;
  }

  public boolean hasMapping(ClassFile owner, ClassMethod method) {
    return this.methodMapping.containsKey(MappingRemapper.methodDesc(owner, method));
  }

  public ClassMapping fieldMapping(ClassFile owner, String from, String to) {
    return fieldMapping(owner, owner.findField(from, null), to);
  }

  public ClassMapping fieldMapping(ClassFile owner, FieldNode from, String to) {
    return fieldMapping(owner, from.desc, from.name, to);
  }

  public boolean hasMapping(ClassFile owner, FieldNode field) {
    return this.fieldMapping.containsKey(MappingRemapper.fieldDesc(owner.name, field.name, field.desc));
  }

  public ClassMapping stringLdcMapping(String from, String to) {
    this.stringMapping.put(from, to);
    return this;
  }

  public ClassMapping fieldMapping(ClassFile owner, String desc, String from, String to) {
    this.fieldMapping.put(MappingRemapper.fieldDesc(owner.name, from, desc), to);
    return this;
  }

  public String findMethodName(ClassFile node, ClassMethod method) {
    return this.methodMapping.get(MappingRemapper.methodDesc(node, method));
  }
}
