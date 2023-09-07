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
import io.github.usernugget.redefiner.util.asm.desc.ParsedField;
import io.github.usernugget.redefiner.util.asm.desc.ParsedMethod;
import io.github.usernugget.redefiner.util.asm.instruction.Insns;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.function.Consumer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.TraceClassVisitor;

public class ClassFile extends ClassNode implements AccessFlags {
  public ClassFile() {
    super(Opcodes.ASM9);
  }

  public ClassFile(int access, String name) {
    this(JavaInternals.CLASS_VERSION, access, name, null, "java/lang/Object", null);
  }

  public ClassFile(int access, String name, String superClass, String... interfaces) {
    this(JavaInternals.CLASS_VERSION, access, name, null, superClass, interfaces);
  }

  public ClassFile(int version, int access, String name,
     String signature, String superClass, String[] interfaces) {
    this();
    visit(version, access, name, signature, superClass, interfaces);
  }

  public static String generateClassEnding() {
    // 0123456789abcdefghijklmnopqrstuvwxyz
    return Long.toString(System.nanoTime(), 36);
  }

  public ClassField visitField(int access, String name, String descriptor) {
    return visitField(access, name, descriptor, null, null);
  }

  public ClassField visitField(int access, String name, String descriptor, Object value) {
    return visitField(access, name, descriptor, null, value);
  }

  @Override
  public ClassField visitField(int access, String name, String descriptor, String signature, Object value) {
    ClassField field = new ClassField(
      this, access, name, descriptor, signature, value
    );
    this.fields.add(field);
    return field;
  }

  public ClassMethod visitMethod(int access, String name, String descriptor) {
    return visitMethod(access, name, descriptor, null, null);
  }

  @Override
  public ClassMethod visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
    ClassMethod method = new ClassMethod(
      this, access, name, descriptor, signature, exceptions
    );
    this.methods.add(method);
    return method;
  }

  public String toReadableBytecode() {
    StringWriter writer = new StringWriter();
    accept(new TraceClassVisitor(null, new PrintWriter(writer)));
    return writer.toString();
  }

  public ClassMethod findMethod(String name, String desc) {
    for (MethodNode method : this.methods) {
      if ((name == null || method.name.equals(name)) &&
          (desc == null || method.desc.equals(desc))) {
        return (ClassMethod) method;
      }
    }

    return null;
  }

  public ClassMethod findMethod(ParsedMethod parsed) {
    for (MethodNode method : this.methods) {
      if ((parsed.getName() == null || method.name.equals(parsed.getName())) &&
          (parsed.getDesc() == null || method.desc.equals(parsed.getDesc()))) {
        return (ClassMethod) method;
      }
    }

    return null;
  }

  public ClassField findField(String name, String desc) {
    for (FieldNode field : this.fields) {
      if (field.name.equals(name) &&
          field.desc.equals(desc)) {
        return (ClassField) field;
      }
    }

    return null;
  }

  public ClassField findField(ParsedField parsed) {
    for (FieldNode field : this.fields) {
      if ((parsed.getName() == null || field.name.equals(parsed.getName())) &&
          (parsed.getDesc() == null || field.desc.equals(parsed.getDesc()))) {
        return (ClassField) field;
      }
    }

    return null;
  }

  public boolean addInterface(String internalName) {
    if (this.interfaces == null) {
      this.interfaces = new ArrayList<>(1);
    }

    if (this.interfaces.contains(internalName)) {
      return false;
    }

    this.interfaces.add(internalName);
    return true;
  }

  public boolean removeInterface(String internalName) {
    if (this.interfaces == null) {
      return false;
    }

    return this.interfaces.remove(internalName);
  }

  @Override
  public int access() {
    return this.access;
  }

  public ClassMethod getOrCreateMethod(int newMethodAccess, String name, String desc) {
    ClassMethod method = this.findMethod(name, desc);
    if (method != null) {
      return method;
    }

    return this.visitMethod(newMethodAccess, name, desc);
  }

  public ClassMethod visitSimpleInitializer() {
    ClassMethod method = this.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V");
    Insns insns = method.getInstructions();

    // super.<init>();
    insns.varOp(Opcodes.ALOAD, 0);
    insns.methodOp(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
    insns.op(Opcodes.RETURN);

    return method;
  }

  public void eachAnnotation(Consumer<AnnotationNode> function) {
    Ops.eachAnnotation(function, this.visibleAnnotations, this.invisibleAnnotations);
  }

  public AnnotationNode getAnnotation(Class<?> klass) {
    String desc = Type.getInternalName(klass);
    if (this.visibleAnnotations != null) {
      for (AnnotationNode annotation : this.visibleAnnotations) {
        if (annotation != null && annotation.desc.equals(desc)) {
          return annotation;
        }
      }
    }

    if (this.invisibleAnnotations != null) {
      for (AnnotationNode annotation : this.invisibleAnnotations) {
        if (annotation != null && annotation.desc.equals(desc)) {
          return annotation;
        }
      }
    }

    return null;
  }

  @Override
  public String toString() {
    return this.name;
  }
}
