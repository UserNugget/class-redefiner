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
import io.github.usernugget.redefiner.util.asm.node.ClassField;
import io.github.usernugget.redefiner.util.asm.node.ClassFile;
import io.github.usernugget.redefiner.util.asm.node.ClassMethod;
import io.github.usernugget.redefiner.util.asm.node.Insts;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import static org.objectweb.asm.Opcodes.ACC_ABSTRACT;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_INTERFACE;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ACC_VARARGS;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.RETURN;

public class CodeGenerator {
  private static final String GET_FIELD = "_getfield", PUT_FIELD = "_putfield", INVOKE = "_invoke", STATIC = "_static";

  private static final String CLASS_NAME = Type.getInternalName(CodeGenerator.class).replace('.', '/');
  private static final Pattern ESCAPE_PATTERN = Pattern.compile("[.;\\[/<>()]");

  private static final MethodHandle CLASS_LOADER_CONSTRUCTOR;

  private static final Map<ClassLoader, ClassLoader> WRAPPED_CLASS_LOADERS = new WeakHashMap<>();
  private static final AtomicInteger CLASS_COUNTER = new AtomicInteger();
  private static final String ID = Long.toString(System.nanoTime(), 36);

  static {
    try {
      String delegatingClassLoader;
      if (JavaInternals.CLASS_MAJOR_VERSION >= 53) {
        delegatingClassLoader = "jdk.internal.reflect.DelegatingClassLoader";
      } else {
        delegatingClassLoader = "sun.reflect.DelegatingClassLoader";
      }
      CLASS_LOADER_CONSTRUCTOR = JavaInternals.TRUSTED_LOOKUP.findConstructor(
         Class.forName(delegatingClassLoader),
         MethodType.methodType(void.class, ClassLoader.class)
      );
    } catch (NoSuchMethodException | IllegalAccessException | ClassNotFoundException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  public static String newClassName() {
    return CLASS_NAME + "$Gen" + CLASS_COUNTER.getAndIncrement() + "_" + ID;
  }

  private static synchronized ClassLoader wrapClassLoader(ClassLoader classLoader) {
    return WRAPPED_CLASS_LOADERS.computeIfAbsent(classLoader, _classLoader -> {
      try {
        return (ClassLoader) CLASS_LOADER_CONSTRUCTOR.invoke(_classLoader);
      } catch (Throwable throwable) {
        throw new IllegalStateException("unable to create DelegatingClassLoader", throwable);
      }
    });
  }

  private boolean forceCreate;

  private ClassFile magicClass, magicInterface;
  private ClassField impl;

  public CodeGenerator() {
    this(false);
  }

  public CodeGenerator(boolean forceCreate) {
    this.forceCreate = forceCreate;
  }

  public ClassFile getGeneratedClass() { return this.magicClass; }
  public ClassFile getGeneratedInterface() { return this.magicInterface; }
  public ClassField getGeneratedImplField() { return this.impl; }

  public void ensureGenerating() {
    if (this.magicClass == null) {
      String magicAccessor = JavaInternals.CLASS_MAJOR_VERSION >= 53 ?
                             "jdk/internal/reflect/MagicAccessorImpl" :
                             "sun/reflect/MagicAccessorImpl";

      this.magicClass = new ClassFile(
         ACC_PRIVATE | ACC_FINAL | ACC_SUPER,
         newClassName(), magicAccessor
      );
      this.magicClass.visitInitializer();

      this.magicInterface = new ClassFile(
         ACC_PUBLIC | ACC_ABSTRACT | ACC_INTERFACE,
         this.magicClass.name + "_interface"
      );
      this.magicClass.interfaces = Collections.singletonList(this.magicInterface.name);

      this.impl = this.magicInterface.visitField(ACC_PUBLIC | ACC_STATIC | ACC_FINAL, "IMPL", "L" + this.magicInterface.name + ";");
    }
  }

  private ClassMethod findInterfaceMethod(String methodName, String methodDesc) {
    ClassMethod previousGenerated = this.magicInterface.findDeclaredMethod(methodName, methodDesc);
    if (previousGenerated != null) {
      return previousGenerated;
    }

    return this.magicInterface.findDeclaredMethod(methodName + STATIC, methodDesc);
  }

  private String newMethodName(String owner, String desc, String name) {
    return ESCAPE_PATTERN.matcher(owner + desc).replaceAll("_") + name;
  }

  public ClassMethod fieldGetter(ClassFile owner, ClassField field) {
    ensureGenerating();

    String methodName = newMethodName(owner.name, field.desc, field.name + GET_FIELD);
    String methodDesc = (!field.isStatic() ? "(L" + owner.name + ";)" : "()") + field.desc;

    ClassMethod previousGenerated = findInterfaceMethod(methodName, methodDesc);
    if (previousGenerated != null) {
      return previousGenerated;
    }

    ClassMethod generated = create(methodName, methodDesc, field.access);
    Insts insts = generated.insts();

    insts.load(generated);
    insts.get(field);
    insts.returnOp(Type.getType(field.desc));

    return findInterfaceMethod(methodName, methodDesc);
  }

  public ClassMethod fieldSetter(ClassFile owner, ClassField field) {
    ensureGenerating();

    String methodName = newMethodName(owner.name, field.desc, field.name + PUT_FIELD);
    String methodDesc = "(" + (!field.isStatic() ? "L" + owner.name + ";" : "") + field.desc + ")V";

    ClassMethod previousGenerated = findInterfaceMethod(methodName, methodDesc);
    if (previousGenerated != null) {
      return previousGenerated;
    }

    ClassMethod generated = create(methodName, methodDesc, field.access);
    Insts insts = generated.insts();

    insts.load(generated);
    insts.put(field);
    insts.op(RETURN);

    return findInterfaceMethod(methodName, methodDesc);
  }

  public ClassMethod methodInvoker(ClassFile owner, ClassMethod method) {
    ensureGenerating();

    String methodName = newMethodName(owner.name, method.desc, method.name + INVOKE);
    String methodDesc = !method.isStatic() ? "(L" + owner.name + ";" + method.desc.substring(1) : method.desc;

    ClassMethod previousGenerated = findInterfaceMethod(methodName, methodDesc);
    if (previousGenerated != null) {
      return previousGenerated;
    }

    ClassMethod generated = create(methodName, methodDesc, method.access);
    Insts insts = generated.insts();

    insts.load(generated);
    insts.invoke(method);
    insts.returnOp(Type.getReturnType(method.desc));

    return findInterfaceMethod(methodName, methodDesc);
  }

  private ClassMethod create(String methodName, String methodDesc, int access) {
    int varargs = access & ACC_VARARGS;
    if (Modifier.isPublic(access) && !this.forceCreate) {
      return this.magicInterface.visitMethod(ACC_PUBLIC | ACC_STATIC | varargs, methodName + STATIC, methodDesc);
    }

    this.magicInterface.visitMethod(ACC_PUBLIC | ACC_ABSTRACT | varargs, methodName, methodDesc);
    ClassMethod delegateMethod = this.magicClass.visitMethod(ACC_PUBLIC | ACC_FINAL | varargs, methodName, methodDesc);

    ClassMethod method = this.magicInterface.visitMethod(ACC_PUBLIC | ACC_STATIC | varargs, methodName + STATIC, methodDesc);
    Insts insts = method.insts();

    insts.get(this.impl);
    insts.load(methodDesc, method.access);
    insts.invoke(this.magicInterface, delegateMethod.name, delegateMethod.desc);
    insts.returnOp(Type.getReturnType(delegateMethod.desc));

    return delegateMethod;
  }

  public static Class<?> defineReflectionClass(ClassLoader classLoader, Class<?> interfaceClass, ClassFile classFile) {
    return ClassIO.define(classFile, wrapClassLoader(classLoader), ClassWriter.COMPUTE_FRAMES);
  }

  public void define(ClassLoader classLoader) {
    define(classLoader, classLoader);
  }

  public void define(ClassLoader magicInterfaceLoader, ClassLoader magicClassLoader) {
    if (this.magicClass == null) {
      return;
    }

    if (this.magicClass.methods().size() == 1) {
      this.magicInterface.removeField(this.impl);
    }

    Class<?> magicInterface = ClassIO.define(this.magicInterface, magicInterfaceLoader, ClassWriter.COMPUTE_FRAMES);
    if (this.magicClass.methods().size() > 1) {
      Class<?> accessKlass = ClassIO.define(this.magicClass, wrapClassLoader(magicClassLoader), ClassWriter.COMPUTE_FRAMES);
      try {
        Field impl = magicInterface.getDeclaredField("IMPL");

        Constructor<?> constructor = accessKlass.getConstructor();
        constructor.setAccessible(true);

        JavaInternals.UNSAFE.putObject(
           JavaInternals.UNSAFE.staticFieldBase(impl),
           JavaInternals.UNSAFE.staticFieldOffset(impl),
           constructor.newInstance()
        );
      } catch (NoSuchFieldException | InvocationTargetException | InstantiationException |
               IllegalAccessException | NoSuchMethodException e) {
        throw new IllegalStateException(e);
      }
    }
  }

  public void invoke(Insts insts, AbstractInsnNode insnNode, ClassMethod method) {
    insts.set(insnNode, invokeOp(method.name + STATIC, method.desc));
  }

  public void invokeRaw(Insts insts, AbstractInsnNode insnNode, ClassMethod method) {
    insts.set(insnNode, invokeOp(method.name, method.desc));
  }

  public MethodInsnNode invokeStaticOp(ClassMethod method) {
    return invokeOp(method.name.endsWith(STATIC) ? method.name : method.name + STATIC, method.desc);
  }

  public MethodInsnNode invokeOp(ClassMethod method) {
    return invokeOp(method.name, method.desc);
  }

  public MethodInsnNode invokeOp(String methodName, String methodDesc) {
    return new MethodInsnNode(INVOKESTATIC, this.magicInterface.name, methodName, methodDesc, true);
  }
}
