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

package io.github.usernugget.redefiner.util.asm.node;

import io.github.usernugget.redefiner.util.JavaInternals;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceClassVisitor;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ASM9;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.RETURN;

public class ClassFile extends ClassNode {
  // Hide superclass fields
  private byte fields, methods;

  public ClassFile() {
    super(ASM9);
  }

  public ClassFile(int access, String name) {
    this(access, name, "java/lang/Object");
  }

  public ClassFile(int access, String name, String superName) {
    this(access, name, superName, new String[0]);
  }

  public ClassFile(int access, String name, String superName, String... interfaces) {
    this();
    visit(JavaInternals.CLASS_MAJOR_VERSION, access, name, null, superName, interfaces);
    visitSource("generated", null);
  }

  public List<ClassMethod> methods() {
    return (List) super.methods;
  }

  public void removeMethod(ClassMethod method) {
    methods().remove(method);
  }

  public List<ClassField> fields() {
    return (List) super.fields;
  }

  public void removeField(ClassField field) {
    fields().remove(field);
  }

  @Override
  public ClassField visitField(int access, String name, String descriptor, String signature, Object value) {
    ClassField field = new ClassField(access, name, descriptor, signature, value);
    super.fields.add(field);
    return field;
  }

  public ClassField visitField(int access, String name, String descriptor) {
    return visitField(access, name, descriptor, null, null);
  }

  @Override
  public ClassMethod visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
    ClassMethod method = new ClassMethod(access, name, descriptor, signature, exceptions);
    super.methods.add(method);
    return method;
  }

  public ClassMethod visitMethod(int access, String name, String descriptor) {
    return visitMethod(access, name, descriptor, null, null);
  }

  public ClassMethod findMethod(String methodName) {
    return findMethod(methodName, (String) null);
  }

  public ClassMethod findMethod(String methodName, Type methodDesc) {
    return findMethod(methodName, methodDesc.getDescriptor());
  }

  public ClassMethod findMethod(String methodName, String methodDesc) {
    for (ClassMethod method : methods()) {
      if ((methodName == null || method.name.equals(methodName)) &&
          (methodDesc == null || method.desc.equals(methodDesc))) {
        return method;
      }
    }
    return null;
  }

  public ClassField findField(String fieldName) {
    return findField(fieldName, null);
  }

  public ClassField findField(String fieldName, String descriptor) {
    for (ClassField field : fields()) {
      if ((fieldName == null || field.name.equals(fieldName)) &&
          (descriptor == null || field.desc.equals(descriptor))) {
        return field;
      }
    }

    return null;
  }

  public void visitInitializer() {
    ClassMethod method = visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V");
    method.visitVarInsn(ALOAD, 0);
    method.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
    method.visitInsn(RETURN);
  }

  public boolean isPackagePrivate() {
    return (this.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED | Opcodes.ACC_PRIVATE)) == 0;
  }

  public boolean isPublic() {
    return (this.access & Opcodes.ACC_PUBLIC) != 0;
  }

  public boolean isPrivate() {
    return (this.access & Opcodes.ACC_PRIVATE) != 0;
  }

  public boolean isProtected() {
    return (this.access & Opcodes.ACC_PROTECTED) != 0;
  }

  public boolean isStatic() {
    return (this.access & Opcodes.ACC_STATIC) != 0;
  }

  public boolean isSuper() {
    return (this.access & Opcodes.ACC_SUPER) != 0;
  }

  public boolean isInterface() {
    return (this.access & Opcodes.ACC_INTERFACE) != 0;
  }

  public boolean isAbstract() {
    return (this.access & Opcodes.ACC_ABSTRACT) != 0;
  }

  public boolean isSynthetic() {
    return (this.access & Opcodes.ACC_SYNTHETIC) != 0;
  }

  public boolean isAnnotation() {
    return (this.access & Opcodes.ACC_ANNOTATION) != 0;
  }

  public boolean isEnum() {
    return (this.access & Opcodes.ACC_ENUM) != 0;
  }

  public boolean isModule() {
    return (this.access & Opcodes.ACC_MODULE) != 0;
  }

  public boolean isRecord() {
    return (this.access & Opcodes.ACC_RECORD) != 0;
  }

  public boolean isDeprecated() {
    return (this.access & Opcodes.ACC_DEPRECATED) != 0;
  }

  public String toBytecodeString() {
    StringWriter writer = new StringWriter();
    accept(new TraceClassVisitor(null, new Textifier(), new PrintWriter(writer)));
    return writer.toString();
  }
}
