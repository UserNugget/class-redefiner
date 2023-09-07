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

package io.github.usernugget.redefiner.util.asm.reflect;

import io.github.usernugget.redefiner.util.JavaInternals;
import io.github.usernugget.redefiner.util.asm.AccessFlags;
import io.github.usernugget.redefiner.util.asm.ClassField;
import io.github.usernugget.redefiner.util.asm.ClassFile;
import io.github.usernugget.redefiner.util.asm.ClassMethod;
import io.github.usernugget.redefiner.util.asm.instruction.Insns;
import io.github.usernugget.redefiner.util.asm.io.ClassSerializer;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class Reflection {
  private static final Pattern ILLEGAL_NAME_CHARACTERS = Pattern.compile("[.;\\[/<>]");

  private static final String CURRENT_CLASS_NAME = Type.getInternalName(Reflection.class);
  private static final MethodHandle WRAP_CLASS_LOADER;

  static {
    try {
      WRAP_CLASS_LOADER = JavaInternals.TRUSTED.findConstructor(
        Class.forName(
          "jdk.internal.reflect.DelegatingClassLoader",
          false,
          ClassLoader.getPlatformClassLoader()
        ),
        MethodType.methodType(void.class, ClassLoader.class)
      );
    } catch (Throwable throwable) {
      throw new ExceptionInInitializerError(throwable);
    }
  }

  private final Map<AccessFlags, ClassMethod> reflections = new HashMap<>();

  private final ClassFile targetClass;
  private final ClassFile targetInterface;
  private final ClassField implField;

  public Reflection() {
    String classId = ClassFile.generateClassEnding();

    this.targetInterface = new ClassFile(
      Opcodes.ACC_PUBLIC | Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT,
      CURRENT_CLASS_NAME + "$InterfaceGen_" + classId
    );

    this.targetClass = new ClassFile(
      Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL,
      CURRENT_CLASS_NAME + "$ClassGen_" + classId,
      "jdk/internal/reflect/MagicAccessorImpl", this.targetInterface.name
    );

    this.targetClass.visitSimpleInitializer();

    this.implField = this.targetInterface.visitField(
      Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL,
      "IMPL", 'L' + this.targetInterface.name + ';'
    );
  }

  public Map<AccessFlags, ClassMethod> getReflections() {
    return this.reflections;
  }

  public ClassFile getTargetClass() {
    return this.targetClass;
  }

  public ClassFile getTargetInterface() {
    return this.targetInterface;
  }

  public ClassField getImplField() {
    return this.implField;
  }

  protected ClassLoader wrapClassLoader(ClassLoader classLoader) {
    try {
      return (ClassLoader) WRAP_CLASS_LOADER.invoke(classLoader);
    } catch (Throwable e) {
      throw new IllegalStateException("unable to create DelegatingClassLoader", e);
    }
  }

  public void defineClasses(
    ClassSerializer serializer,
    ClassLoader interfaceClassLoader,
    ClassLoader accessorClassLoader
  ) {
    Class<?> interfaceClass = serializer.defineClass(
      this.targetInterface, interfaceClassLoader
    );

    Class<?> accessorClass = serializer.defineClass(
      this.targetClass, wrapClassLoader(accessorClassLoader)
    );

    try {
      Field field = interfaceClass.getDeclaredField("IMPL");
      if (!Modifier.isStatic(field.getModifiers())) {
        throw new IllegalStateException("(internal) IMPL field should be static");
      }

      if (field.getType() != interfaceClass) {
        throw new IllegalStateException("(internal) IMPL has an invalid type");
      }

      JavaInternals.UNSAFE.putObject(
        JavaInternals.UNSAFE.staticFieldBase(field),
        JavaInternals.UNSAFE.staticFieldOffset(field),
        accessorClass.getDeclaredConstructor().newInstance()
      );
    } catch (InstantiationException | NoSuchFieldException | NoSuchMethodException |
             IllegalAccessException | InvocationTargetException e) {
      throw new IllegalStateException(e);
    }
  }

  public static String escapeName(String name) {
    return ILLEGAL_NAME_CHARACTERS.matcher(name).replaceAll("\\$");
  }

  protected String wrapName(AccessFlags accessFlags, ClassFile owner, String name) {
    StringBuilder newName = new StringBuilder(owner.name).append('_').append(name);

    if (accessFlags.isStatic()) {
      newName.append("$STATIC");
    }

    return escapeName(newName.toString());
  }

  protected String wrapGetterDesc(ClassField field, ClassFile owner, String desc) {
    return !field.isStatic() ? "(L" + owner.name + ";)" + desc : "()" + desc;
  }

  protected String wrapSetterDesc(ClassField field, ClassFile owner, String desc) {
    return !field.isStatic() ? "(L" + owner.name + ';' + desc + ")V" : '(' + desc + ")V";
  }

  protected String wrapMethodDesc(ClassMethod method, ClassFile owner, String desc) {
    if (!method.isStatic()) {
      return "(L" + owner.name + ';' + desc.substring(1);
    }

    return desc;
  }

  protected ClassMethod createWrapper(AccessFlags owner, String name, String desc) {
    ClassMethod interfaceMethod = this.targetInterface.visitMethod(
      Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT | (owner.access() & Opcodes.ACC_VARARGS),
      name + "$WRAPPED", desc
    );

    ClassMethod wrapper = this.targetInterface.visitMethod(
      Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | (owner.access() & Opcodes.ACC_VARARGS),
      name, desc
    );

    Insns insns = wrapper.getInstructions();
    insns.fieldGetter(this.implField);
    this.loadAndInvoke(wrapper, interfaceMethod);

    this.reflections.put(owner, wrapper);
    return this.targetClass.visitMethod(
      Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | (owner.access() & Opcodes.ACC_VARARGS),
      name + "$WRAPPED", desc
    );
  }

  protected void loadAndInvoke(ClassMethod wrapper, ClassMethod targetMethod) {
    Insns insns = wrapper.getInstructions();
    int varOffset = wrapper.isStatic() ? 0 : 1;
    for (Type type : Type.getArgumentTypes(wrapper.desc)) {
      insns.loadOp(type, varOffset);
      varOffset += type.getSort() == Type.LONG ||
                   type.getSort() == Type.DOUBLE ? 2 : 1;
    }

    insns.invoke(targetMethod);
    insns.returnOp(Type.getReturnType(targetMethod.desc));
  }

  protected ClassMethod findWrapper(String name, String desc) {
    return this.targetInterface.findMethod(name, desc);
  }

  public ClassMethod wrapSetter(ClassField field) {
    ClassMethod wrapper = this.reflections.get(field);
    if (wrapper != null) {
      return wrapper;
    }

    String name = wrapName(field, field.owner, field.name + "$SETTER");
    String desc = wrapSetterDesc(field, field.owner, field.desc);

    wrapper = this.findWrapper(name, desc);
    if (wrapper != null) {
      return wrapper;
    }

    wrapper = this.createWrapper(field, name, desc);
    Insns insns = wrapper.getInstructions();

    Type type = Type.getType(field.desc);
    if (!field.isStatic()) {
      int varOffset = wrapper.isStatic() ? 0 : 1;
      insns.varOp(Opcodes.ALOAD, varOffset);
      insns.loadOp(type, varOffset + 1);
    }
    insns.fieldSetter(field);
    insns.returnOp(type);

    return this.findWrapper(name, desc);
  }

  public ClassMethod wrapGetter(ClassField field) {
    ClassMethod wrapper = this.reflections.get(field);
    if (wrapper != null) {
      return wrapper;
    }

    String name = wrapName(field, field.owner, field.name + "$GETTER");
    String desc = wrapGetterDesc(field, field.owner, field.desc);

    wrapper = this.findWrapper(name, desc);
    if (wrapper != null) {
      return wrapper;
    }

    ClassMethod internalWrapper = this.createWrapper(field, name, desc);
    Insns insns = internalWrapper.getInstructions();

    if (!field.isStatic()) {
      insns.varOp(Opcodes.ALOAD, internalWrapper.isStatic() ? 0 : 1);
    }
    insns.fieldGetter(field);
    insns.returnOp(field.desc);

    return this.findWrapper(name, desc);
  }
  
  public ClassMethod wrapMethod(ClassMethod method) {
    ClassMethod wrapper = this.reflections.get(method);
    if (wrapper != null) {
      return wrapper;
    }

    String name = wrapName(method, method.owner, method.name);
    String desc = wrapMethodDesc(method, method.owner, method.desc);

    wrapper = this.findWrapper(name, desc);
    if (wrapper != null) {
      return wrapper;
    }

    ClassMethod internalWrapper = this.createWrapper(method, name, desc);
    Insns insns = internalWrapper.getInstructions();

    this.loadAndInvoke(internalWrapper, method);

    return this.findWrapper(name, desc);
  }
}
