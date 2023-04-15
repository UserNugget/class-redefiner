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

package io.github.usernugget.redefiner.util.asm.info;

import io.github.usernugget.redefiner.util.asm.AnnotationValues;
import io.github.usernugget.redefiner.util.asm.node.ClassField;
import io.github.usernugget.redefiner.util.asm.node.ClassFile;
import org.objectweb.asm.tree.FieldInsnNode;

public class FieldInfo {
  private final String owner, name, desc;

  public FieldInfo(String owner, String name, String desc) {
    this.owner = owner;
    this.name = name;
    this.desc = desc;
  }

  public String getOwner() {
    return this.owner;
  }

  public String getName() {
    return this.name;
  }

  public String getDesc() {
    return this.desc;
  }

  public static FieldInfo parse(String desc) {
    String classOwner = null;

    String methodName = desc;
    String methodDesc = null;

    int spaceIndex = methodName.indexOf(' ');
    if (spaceIndex != -1) {
      if (spaceIndex != methodName.lastIndexOf(' ')) {
        classOwner = methodName.substring(spaceIndex);
        methodName = methodName.substring(0, spaceIndex);
      }

      spaceIndex = methodName.indexOf(' ');
      methodDesc = methodName.substring(spaceIndex);
      methodName = methodName.substring(0, spaceIndex);
    }

    return new FieldInfo(classOwner, methodName, methodDesc);
  }

  public static FieldInfo parse(AnnotationValues args) {
    return parse(args.getString("field"));
  }

  public ClassField findField(ClassFile classNode) {
    return classNode.findField(this.name, this.desc);
  }

  public ClassField findFieldOrThrow(ClassFile classNode) {
    ClassField classField = classNode.findField(this.name, this.desc);
    if (classField == null) {
      throw new IllegalStateException("field " + this.name + " not found in class " + classNode.name);
    }
    return classField;
  }

  public boolean matches(FieldInsnNode fieldNode) {
    return (this.owner == null || fieldNode.owner.equals(this.owner)) &&
           (this.name == null || fieldNode.name.equals(this.name)) &&
           (this.desc == null || fieldNode.desc.equals(this.desc));
  }
}
