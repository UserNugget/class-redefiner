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
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class Reflection {
  private static final Pattern ILLEGAL_NAME_CHARACTERS = Pattern.compile("[.;\\[/<>]");
  private static final String CURRENT_CLASS_NAME = Type.getInternalName(Reflection.class);

  private enum AccessorType {
    SET, GET, INVOKE
  }

  private static final class Prop {

    private final AccessorType type;
    private final AccessFlags flags;

    public Prop(AccessorType type, AccessFlags flags) {
      this.type = type;
      this.flags = flags;
    }
  }

  private final Map<AccessFlags, ClassMethod> reflections = new HashMap<>();
  private final Map<ClassField, Prop> props = new HashMap<>();

  private final ClassFile targetClass;
  private final ClassMethod targetClassInit;
  private final ClassFile targetInterface;
  private final ClassField implField;
  private final String propertyPrefix;
  private final String propertyName;
  private int accessorIndex;

  public Reflection() {
    String classId = ClassFile.generateClassEnding();

    this.targetInterface = new ClassFile(
      Opcodes.ACC_PUBLIC | Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT,
      CURRENT_CLASS_NAME + "$InterfaceGen_" + classId
    );

    this.targetClass = new ClassFile(
      Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL,
      CURRENT_CLASS_NAME + "$ClassGen_" + classId,
      "java/lang/Object", this.targetInterface.name
    );

    this.targetClass.visitSimpleInitializer();

    this.implField = this.targetInterface.visitField(
      Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL,
      "IMPL", 'L' + this.targetInterface.name + ';'
    );

    // TODO: use other storage to store cross-classloader instance?
    this.propertyPrefix = classId + "-redefiner";
    this.propertyName = this.propertyPrefix + ".initialized";

    ClassMethod clinit = this.targetInterface.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V");
    Insns insns = clinit.getInstructions();

    // IMPL = (<interface name>) System.getProperties().get(<property name>);
    insns.methodOp(Opcodes.INVOKESTATIC, "java/lang/System", "getProperties", "()Ljava/util/Properties;", false);
    insns.ldc(this.propertyName);
    insns.methodOp(Opcodes.INVOKEVIRTUAL, "java/util/Properties", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
    insns.typeOp(Opcodes.CHECKCAST, this.targetInterface.name);
    insns.fieldSetter(this.implField);
    insns.op(Opcodes.RETURN);

    this.targetClassInit = this.targetClass.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V");
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

  public void defineClasses(
    ClassSerializer serializer,
    ClassLoader interfaceClassLoader,
    ClassLoader accessorClassLoader
  ) {
    this.targetClassInit.getInstructions().op(Opcodes.RETURN);

    Class<?> interfaceClass = serializer.defineClass(this.targetInterface, interfaceClassLoader);
    Class<?> accessorClass = serializer.defineClass(this.targetClass, accessorClassLoader);

    try {
      for (Entry<ClassField, Prop> entry : this.props.entrySet()) {
        String propName = this.propertyPrefix + "." + entry.getKey().name;
        Prop prop = entry.getValue();

        MethodHandle handle = null;
        if (prop.flags instanceof ClassField) {
          ClassField field = (ClassField) prop.flags;
          Class<?> owner = Class.forName(field.owner.name.replace('/', '.'), false, accessorClassLoader);
          Class<?> type = Object.class;

          Type descType = Type.getType(field.desc);
          switch (descType.getSort()) {
            case Type.BOOLEAN: type = Boolean.TYPE; break;
            case Type.CHAR: type = Character.TYPE; break;
            case Type.BYTE: type = Byte.TYPE; break;
            case Type.SHORT: type = Short.TYPE; break;
            case Type.INT: type = Integer.TYPE; break;
            case Type.FLOAT: type = Float.TYPE; break;
            case Type.LONG: type = Long.TYPE; break;
            case Type.DOUBLE: type = Double.TYPE; break;
            case Type.OBJECT: type = Class.forName(descType.getClassName(), false, accessorClassLoader); break;
            case Type.ARRAY: {
              String arrayName = "[".repeat(descType.getDimensions()) + "L" + descType.getElementType().getClassName() + ";";
              type = Class.forName(arrayName, false, accessorClassLoader);
              break;
            }
          }

          if (field.isStatic()) {
            handle = prop.type == AccessorType.GET ?
                     JavaInternals.TRUSTED.findStaticGetter(owner, field.name, type) :
                     JavaInternals.TRUSTED.findStaticSetter(owner, field.name, type);
          } else {
            handle = prop.type == AccessorType.GET ?
                     JavaInternals.TRUSTED.findGetter(owner, field.name, type) :
                     JavaInternals.TRUSTED.findSetter(owner, field.name, type);
          }
        } else if (prop.flags instanceof ClassMethod) {
          ClassMethod method = (ClassMethod) prop.flags;
          Class<?> owner = Class.forName(method.owner.name.replace('/', '.'), false, accessorClassLoader);

          if (method.isStatic()) {
            handle = JavaInternals.TRUSTED.findStatic(owner, method.name,
              MethodType.fromMethodDescriptorString(method.desc, accessorClassLoader));
          } else if (method.isSpecial()) {
            handle = JavaInternals.TRUSTED.findConstructor(owner,
              MethodType.fromMethodDescriptorString(method.desc, accessorClassLoader));
          } else {
            handle = JavaInternals.TRUSTED.findVirtual(owner, method.name,
              MethodType.fromMethodDescriptorString(method.desc, accessorClassLoader));
          }
        }

        System.getProperties().put(propName, handle);
      }

      System.getProperties().put(this.propertyName, accessorClass.getDeclaredConstructor().newInstance());
      JavaInternals.TRUSTED.ensureInitialized(interfaceClass);
      System.getProperties().remove(this.propertyName);
      for (Entry<ClassField, Prop> entry : this.props.entrySet()) {
        System.getProperties().remove(this.propertyPrefix + "." + entry.getKey().name);
      }
    } catch (Throwable e) {
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

  protected void load(ClassMethod wrapper, ClassMethod targetMethod) {
    Insns insns = wrapper.getInstructions();
    int varOffset = wrapper.isStatic() ? 0 : 1;
    for (Type type : Type.getArgumentTypes(wrapper.desc)) {
      insns.loadOp(type, varOffset);
      varOffset += type.getSort() == Type.LONG ||
                   type.getSort() == Type.DOUBLE ? 2 : 1;
    }
  }

  protected void loadAndInvoke(ClassMethod wrapper, ClassMethod targetMethod) {
    this.load(wrapper, targetMethod);

    Insns insns = wrapper.getInstructions();
    insns.invoke(targetMethod);
    insns.returnOp(Type.getReturnType(targetMethod.desc));
  }

  protected ClassMethod findWrapper(String name, String desc) {
    return this.targetInterface.findMethod(name, desc);
  }

  private ClassField createMethodHandle() {
    String mhFieldName = "MH_" + this.accessorIndex++;
    ClassField mhField = this.targetClass.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL,
        mhFieldName, "Ljava/lang/invoke/MethodHandle;");

    // <field> = (MethodHandle) System.getProperties().get(<name>);
    Insns insns = this.targetClassInit.getInstructions();
    insns.methodOp(Opcodes.INVOKESTATIC, "java/lang/System", "getProperties", "()Ljava/util/Properties;", false);
    insns.ldc(this.propertyPrefix + "." + mhField.name);
    insns.methodOp(Opcodes.INVOKEVIRTUAL, "java/util/Properties", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
    insns.typeOp(Opcodes.CHECKCAST, "java/lang/invoke/MethodHandle");
    insns.fieldSetter(mhField);

    return mhField;
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
    if (field.isPublic()) {
      if (!field.isStatic()) {
        int varOffset = wrapper.isStatic() ? 0 : 1;
        insns.varOp(Opcodes.ALOAD, varOffset);
        insns.loadOp(type, varOffset + 1);
      }

      insns.fieldSetter(field);
    } else {
      ClassField fieldAccessor = this.createMethodHandle();
      this.props.put(fieldAccessor, new Prop(AccessorType.SET, field));

      insns.fieldGetter(fieldAccessor);

      if (field.isStatic()) {
        insns.methodOp(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodHandle",
          "invokeExact", "(" + type.getDescriptor() + ")V", false);
      } else {
        int varOffset = wrapper.isStatic() ? 0 : 1;
        insns.varOp(Opcodes.ALOAD, varOffset);
        insns.loadOp(type, varOffset + 1);

        insns.methodOp(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodHandle",
          "invokeExact", "(L" + field.owner.name + ";" + type.getDescriptor() + ")V", false);
      }
    }

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

    if (field.isPublic()) {
      if (!field.isStatic()) {
        insns.varOp(Opcodes.ALOAD, internalWrapper.isStatic() ? 0 : 1);
      }

      insns.fieldGetter(field);
    } else {
      ClassField fieldAccessor = this.createMethodHandle();
      this.props.put(fieldAccessor, new Prop(AccessorType.GET, field));

      insns.fieldGetter(fieldAccessor);

      if (field.isStatic()) {
        insns.methodOp(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodHandle",
          "invokeExact", "()" + field.desc, false);
      } else {
        insns.varOp(Opcodes.ALOAD, internalWrapper.isStatic() ? 0 : 1);
        insns.methodOp(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodHandle",
          "invokeExact", "(L" + field.owner.name + ";)" + field.desc, false);
      }
    }

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

    if (method.isPublic()) {
      this.loadAndInvoke(internalWrapper, method);
    } else {
      ClassField methodAccessor = this.createMethodHandle();
      this.props.put(methodAccessor, new Prop(AccessorType.INVOKE, method));

      insns.fieldGetter(methodAccessor);
      this.load(internalWrapper, method);
      if (method.isStatic()) {
        insns.methodOp(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodHandle",
          "invokeExact", method.desc, false);
      } else {
        String accessorDesc = "(L" + method.owner.name + ';' + method.desc.substring(1);
        insns.methodOp(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodHandle",
          "invokeExact", accessorDesc, false);
      }

      insns.returnOp(Type.getReturnType(method.desc));
    }

    return this.findWrapper(name, desc);
  }
}
