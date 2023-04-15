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
import io.github.usernugget.redefiner.util.asm.node.ClassFile;
import io.github.usernugget.redefiner.util.asm.node.ClassMethod;
import java.util.Objects;
import org.objectweb.asm.tree.MethodInsnNode;

public class MethodInfo {
  private final String name, desc;

  public MethodInfo(String name, String desc) {
    this.name = name;
    this.desc = desc;
  }

  public String getName() {
    return this.name;
  }

  public String getDesc() {
    return this.desc;
  }

  public static MethodInfo parse(String desc) {
    String methodName = desc;
    String methodDesc = null;
    if (methodName.contains("(")) {
      methodDesc = methodName.substring(methodName.indexOf('('));
      methodName = methodName.substring(0, methodName.indexOf('('));
    }

    return new MethodInfo(methodName, methodDesc);
  }

  public static MethodInfo parse(AnnotationValues args) {
    return parse(args.getString("method"));
  }

  public static MethodInfo parse(ClassMethod method, AnnotationValues args) {
    String rawMethod = args.getString("method");
    if (rawMethod == null || rawMethod.isEmpty()) {
      rawMethod = method.name;
    }

    return parse(rawMethod);
  }

  public static boolean matches(MethodInsnNode first, MethodInsnNode second) {
    return matches(first.name, first.desc, second.name, second.desc);
  }

  public static boolean matches(MethodInsnNode first, ClassMethod second) {
    return matches(first.name, first.desc, second.name, second.desc);
  }

  public static boolean matches(ClassMethod first, ClassMethod second) {
    return matches(first.name, first.desc, second.name, second.desc);
  }

  public static boolean matches(String firstName, String firstDesc, String secondName, String secondDesc) {
    return Objects.equals(firstName, secondName) && Objects.equals(firstDesc, secondDesc);
  }

  public ClassMethod findMethod(ClassFile classNode) {
    return classNode.findDeclaredMethod(this.name, this.desc);
  }

  public ClassMethod findMethodOrThrow(ClassFile classNode) {
    ClassMethod classMethod = classNode.findDeclaredMethod(this.name, this.desc);
    if (classMethod == null) {
      throw new IllegalStateException("method " + this.name + " not found in class " + classNode.name);
    }
    return classMethod;
  }

  public boolean matches(MethodInsnNode ClassMethod) {
    return (this.name == null || ClassMethod.name.equals(this.name)) &&
           (this.desc == null || ClassMethod.name.equals(this.desc));
  }
}
