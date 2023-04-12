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

package kk.redefiner.util.asm.mapper;

import kk.redefiner.util.asm.node.ClassField;
import kk.redefiner.util.asm.node.ClassFile;
import kk.redefiner.util.asm.node.ClassMethod;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Remapper;

public class MappingRemapper extends Remapper {
  private final ClassMapping mapping;

  public MappingRemapper(ClassMapping mapping) {
    this.mapping = mapping;
  }

  public static String methodDesc(String owner, String name, String desc) {
    return owner + '.' + name + desc;
  }

  public static String methodDesc(ClassFile owner, ClassMethod method) {
    return methodDesc(owner.name, method.name, method.desc);
  }

  public static String fieldDesc(String owner, String name, String desc) {
    return owner + ' ' + desc + ' ' + name;
  }

  public static String fieldDesc(ClassFile owner, ClassField field) {
    return fieldDesc(owner.name, field.name, field.desc);
  }

  public String map(String internalName) {
    return this.mapping.classMapping.getOrDefault(internalName, internalName);
  }

  public String mapFieldName(String owner, String name, String descriptor) {
    return this.mapping.fieldMapping.getOrDefault(fieldDesc(owner, name, descriptor), name);
  }

  public String mapMethodName(String owner, String name, String descriptor) {
    return this.mapping.methodMapping.getOrDefault(methodDesc(owner, name, descriptor), name);
  }

  public String mapAnnotationAttributeName(String descriptor, String name) {
    return mapMethodName(Type.getType(descriptor).getInternalName(), name, "(annotation)");
  }

  public String mapSignature(String signature, boolean typeSignature) {
    return super.mapSignature(signature, typeSignature);
  }

  public Object mapValue(Object value) {
    if (value instanceof String) {
      return this.mapping.stringMapping.getOrDefault(value, (String) value);
    } else {
      return super.mapValue(value);
    }
  }

  public String mapRecordComponentName(String owner, String name, String descriptor) {
    return this.mapping.recordComponentMapping.getOrDefault(owner + name + descriptor, name);
  }
}
