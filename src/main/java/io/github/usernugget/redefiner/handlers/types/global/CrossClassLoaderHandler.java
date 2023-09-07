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

package io.github.usernugget.redefiner.handlers.types.global;

import io.github.usernugget.redefiner.changes.MethodChange;
import io.github.usernugget.redefiner.handlers.Handler;
import io.github.usernugget.redefiner.handlers.Op;
import io.github.usernugget.redefiner.util.JavaInternals;
import io.github.usernugget.redefiner.util.asm.ClassField;
import io.github.usernugget.redefiner.util.asm.ClassFile;
import io.github.usernugget.redefiner.util.asm.ClassMethod;
import io.github.usernugget.redefiner.util.asm.Ops;
import io.github.usernugget.redefiner.util.asm.instruction.Insns;
import io.github.usernugget.redefiner.util.asm.io.ClassSerializer;
import io.github.usernugget.redefiner.util.asm.reflect.Reflection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;

// Allowing mapping and target to have intersecting code
// if they are defined in different classloaders
public class CrossClassLoaderHandler implements Handler {
  private static final String OP_NAME = Type.getInternalName(Op.class);

  private static final class Wrapper {
    private final ClassLoader overlappingLoader;
    private final ClassLoader targetLoader;
    private final Reflection reflection = new Reflection();
    private final ClassFile code = new ClassFile();

    public Wrapper(ClassLoader overlappingLoader, ClassLoader targetLoader) {
      this.overlappingLoader = overlappingLoader;
      this.targetLoader = targetLoader;
      this.code.visitSimpleInitializer();
      this.code.visit(
        JavaInternals.CLASS_VERSION,
        Opcodes.ACC_PUBLIC,
        this.reflection.getTargetClass().name + "_code",
        null,
        "java/lang/Object",
        null
      );
    }

    public void define(ClassSerializer serializer) {
      if (!this.code.methods.isEmpty()) {
        serializer.defineClass(this.code, this.targetLoader);
      }

      this.reflection.defineClasses(
        serializer, this.overlappingLoader, this.targetLoader
      );
    }
  }

  @Override
  public void handleMethod(MethodChange change) {
    ClassSerializer serializer = change.getRedefiner().getClassSerializer();

    ClassFile mapping = change.getMappingClass();
    ClassFile target = change.getTargetClass();

    ClassMethod mappingMethod = change.getMappingMethod();

    ClassLoader mappingLoader = change.getClassLoader();
    ClassLoader targetLoader = change.getTargetJavaClass().getClassLoader();

    Set<ClassLoader> accessibleLoaders =
      this.findParents(change.getTargetJavaClass().getClassLoader());

    String targetName = change.getTargetClass().name;

    Map<ClassLoader, Wrapper> wrappers = new IdentityHashMap<>();

    try {
      if (mappingMethod.tryCatchBlocks != null) {
        for (TryCatchBlockNode catchNode : mappingMethod.tryCatchBlocks) {
          if (catchNode.type == null) continue; // finally block

          if (!this.interactable(
            catchNode.type, target,
            accessibleLoaders,
            mappingLoader, targetLoader
          )) {
            throw new UnsupportedOperationException(
              "try-catch block is using class from inaccessible classloader"
            );
          }
        }
      }

      Insns mappingCode = mappingMethod.getInstructions();
      for (AbstractInsnNode instruction : mappingCode) {
        if (instruction instanceof MethodInsnNode) {
          MethodInsnNode method = (MethodInsnNode) instruction;
          if (!this.interactable(
            method.owner, target,
            accessibleLoaders,
            mappingLoader, targetLoader
          )) {
            // FIXME: trick frames to make it think this is normal
            if (method.name.equals("<init>")) {
              throw new UnsupportedOperationException(
                "creating objects from another classloader is not supported yet, you " +
                "can create " + method.owner + " from another method as a workaround"
              );
            }

            Wrapper wrapper = this.findWrapper(
              wrappers, target.name, mappingLoader, targetLoader
            );

            Class<?> owner = this.findClass(method.owner, mappingLoader, targetLoader);
            ClassMethod ownerMethod = serializer.readClass(owner, ClassReader.SKIP_CODE)
              .findMethod(method.name, method.desc);

            if (ownerMethod == null) {
              throw new IllegalStateException(
                "method " + method.owner + "::" + method.name + method.desc +
                " exists in code, but not in runtime"
              );
            }

            mappingCode.set(
              instruction,
              Ops.invoke(wrapper.reflection.wrapMethod(ownerMethod))
            );
          }
        } else if (instruction instanceof FieldInsnNode) {
          FieldInsnNode field = (FieldInsnNode) instruction;
          if (!this.interactable(
            field.owner, target,
            accessibleLoaders,
            mappingLoader, targetLoader
          )) {
            Wrapper wrapper = this.findWrapper(
              wrappers, target.name, mappingLoader, targetLoader
            );

            Class<?> owner = this.findClass(field.owner, mappingLoader, targetLoader);

            ClassField ownerField = serializer.readClass(owner, ClassReader.SKIP_CODE)
              .findField(field.name, field.desc);

            if (ownerField == null) {
              throw new IllegalStateException(
                "field " + field.desc + " " + field.owner + "." + field.name +
                " exists in code, but not in runtime"
              );
            }

            boolean setter = field.getOpcode() == Opcodes.PUTFIELD ||
                             field.getOpcode() == Opcodes.PUTSTATIC;

            mappingCode.set(
              instruction,
              Ops.invoke(
                setter ? wrapper.reflection.wrapSetter(ownerField) :
                         wrapper.reflection.wrapGetter(ownerField)
              )
            );
          }
        } else if (instruction instanceof InvokeDynamicInsnNode) { // TODO: recheck
          InvokeDynamicInsnNode invokeDynamic = (InvokeDynamicInsnNode) instruction;

          boolean replace = !this.interactable(
            invokeDynamic.bsm.getOwner(), target,
            accessibleLoaders,
            mappingLoader, targetLoader
          );

          if (!replace) {
            for (Object arg : invokeDynamic.bsmArgs) {
              if (arg instanceof Handle) {
                replace = !this.interactable(
                  ((Handle) arg).getOwner(), target,
                  accessibleLoaders,
                  mappingLoader, targetLoader
                );
              } else if (arg instanceof Type) {
                replace = !this.interactable(
                  (Type) arg, target,
                  accessibleLoaders,
                  mappingLoader, targetLoader
                );
              }

              if (replace) {
                break;
              }
            }
          }

          if (replace) {
            Wrapper wrapper = this.findWrapper(
              wrappers, target.name, mappingLoader, targetLoader
            );

            ClassMethod wrappedType = wrapper.code.visitMethod(
              Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
              Reflection.escapeName("InvokeDynamic_" + ClassFile.generateClassEnding()),
              invokeDynamic.desc
            );

            Type descType = Type.getType(invokeDynamic.desc);
            Insns code = wrappedType.getInstructions();

            int index = 0;
            for (Type argumentType : descType.getArgumentTypes()) {
              code.loadOp(argumentType, index);
              index += argumentType.getSort() == Type.DOUBLE ||
                       argumentType.getSort() == Type.LONG ? 2 : 1;
            }

            code.add(invokeDynamic.clone(null));
            code.returnOp(descType.getReturnType());

            mappingCode.set(
              instruction,
              Ops.invoke(wrapper.reflection.wrapMethod(wrappedType))
            );
          }
        } else if (instruction instanceof LdcInsnNode) {
          LdcInsnNode ldc = (LdcInsnNode) instruction;
          if (ldc.cst instanceof Type) {
            Type type = (Type) ldc.cst;

            String className;
            if (type.getSort() == Type.ARRAY) {
              className = type.getElementType().getInternalName();
            } else { // OBJECT
              className = type.getInternalName();
            }

            if (!this.interactable(
              className, target,
              accessibleLoaders,
              mappingLoader, targetLoader
            )) {
              Wrapper wrapper = this.findWrapper(
                wrappers, target.name, mappingLoader, targetLoader
              );

              ClassMethod wrappedType = wrapper.code.visitMethod(
                Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                Reflection.escapeName("ClassType_" + type.getInternalName()),
                "()Ljava/lang/Class;"
              );

              Insns code = wrappedType.getInstructions();
              code.ldc(type);
              code.op(Opcodes.ARETURN);

              mappingCode.set(
                instruction,
                Ops.invoke(wrapper.reflection.wrapMethod(wrappedType))
              );
            }
          }

          // FIXME:
          //  ldc.cst instanceof Handle
          //  ldc.cst instanceof ConstantDynamic
        } else if (instruction instanceof TypeInsnNode) {
          TypeInsnNode type = (TypeInsnNode) instruction;

          // FIXME
          if (type.getOpcode() == Opcodes.NEW) continue;

          if (!this.interactable(
            type.desc, target,
            accessibleLoaders,
            mappingLoader, targetLoader
          )) {
            Wrapper wrapper = this.findWrapper(
              wrappers, target.name, mappingLoader, targetLoader
            );

            ClassMethod wrappedType;
            if (type.getOpcode() == Opcodes.ANEWARRAY) {
              wrappedType = wrapper.code.visitMethod(
                Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                Reflection.escapeName("ArrayType_" + type.desc),
                "(I)[L" + type.desc + ';'
              );

              Insns code = wrappedType.getInstructions();
              code.varOp(Opcodes.ILOAD, 0);
              code.typeOp(type.getOpcode(), type.desc);
              code.op(Opcodes.ARETURN);
            } else if (type.getOpcode() == Opcodes.NEW) {
              wrappedType = wrapper.code.visitMethod(
                Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                Reflection.escapeName("NewType_" + type.desc),
                "()L" + type.desc + ';'
              );

              Insns code = wrappedType.getInstructions();
              code.typeOp(type.getOpcode(), type.desc);
              code.op(Opcodes.ARETURN);
            } else if (type.getOpcode() == Opcodes.CHECKCAST) {
              wrappedType = wrapper.code.visitMethod(
                Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                Reflection.escapeName("TypeCheckCast_" + type.desc),
                "(Ljava/lang/Object;)Ljava/lang/Object;"
              );

              Insns code = wrappedType.getInstructions();
              code.varOp(Opcodes.ALOAD, 0);
              code.typeOp(type.getOpcode(), type.desc);
              code.op(Opcodes.ARETURN);
            } else {
              continue;
            }

            mappingCode.set(
              instruction,
              Ops.invoke(wrapper.reflection.wrapMethod(wrappedType))
            );
          }
        } else if (instruction instanceof MultiANewArrayInsnNode) {
          MultiANewArrayInsnNode array = (MultiANewArrayInsnNode) instruction;
          Type type = Type.getType(array.desc);
          Type element = type.getElementType();

          if (!this.interactable(
            element.getInternalName(), target,
            accessibleLoaders,
            mappingLoader, targetLoader
          )) {
            Wrapper wrapper = this.findWrapper(
              wrappers, target.name, mappingLoader, targetLoader
            );

            int dimensions = type.getDimensions();

            ClassMethod wrappedType = wrapper.code.visitMethod(
              Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
              Reflection.escapeName("MultiANewArray_" + array.desc),
              "(" + "I".repeat(dimensions) + ")" + array.desc
            );

            Insns code = wrappedType.getInstructions();

            for (int index = 0; index < dimensions; index++) {
              code.varOp(Opcodes.ILOAD, index);
            }

            code.add(new MultiANewArrayInsnNode(array.desc, array.dims));
            code.op(Opcodes.ARETURN);

            mappingCode.set(
              instruction,
              Ops.invoke(wrapper.reflection.wrapMethod(wrappedType))
            );
          }
        }
      }
    } catch (Throwable throwable) {
      throw new IllegalStateException(
        "failed to apply cross-classloader changes", throwable
      );
    }

    for (Wrapper wrapper : wrappers.values()) {
      wrapper.define(serializer);
    }
  }

  private Wrapper findWrapper(
    Map<ClassLoader, Wrapper> reflections,
    String name, ClassLoader mapping, ClassLoader target
  ) {
    ClassLoader intersection = null;

    Set<ClassLoader> targets = this.findParents(target);
    for (ClassLoader parent : this.findParents(
      this.findClass(name, mapping, target).getClassLoader()
    )) {
      if (targets.contains(parent)) {
        intersection = parent;
        break;
      }
    }

    return reflections.computeIfAbsent(intersection, key -> new Wrapper(key, mapping));
  }

  private Class<?> findClass(String name, ClassLoader mapping, ClassLoader target) {
    name = name.replace('/', '.');

    try {
      return Class.forName(name, false, Objects.requireNonNullElse(
        target, ClassLoader.getPlatformClassLoader()
      ));
    } catch (Throwable targetThrowable) {
      try {
        return Class.forName(name, false, Objects.requireNonNullElse(
          mapping, ClassLoader.getPlatformClassLoader()
        ));
      } catch (Throwable mappingThrowable) {
        IllegalStateException state = new IllegalStateException(targetThrowable);
        state.addSuppressed(mappingThrowable);
        throw state;
      }
    }
  }

  private boolean interactable(
    Type type, ClassFile target,
    Set<ClassLoader> accessibleLoaders,
    ClassLoader mappingLoader, ClassLoader targetLoader
  ) {
    if (type.getSort() == Type.METHOD) {
      for (Type argumentType : type.getArgumentTypes()) {
        if (!this.interactable(
          argumentType, target,
          accessibleLoaders,
          mappingLoader, targetLoader
        )) {
          return false;
        }
      }

      return !this.interactable(
        type.getReturnType(), target,
        accessibleLoaders,
        mappingLoader, targetLoader
      );
    } else if (type.getSort() == Type.OBJECT ||
               type.getSort() == Type.ARRAY) {
      return !this.interactable(
        type.getInternalName(), target,
        accessibleLoaders,
        mappingLoader, targetLoader
      );
    }

    return true;
  }

  private boolean interactable(
    String owner, ClassFile target,
    Set<ClassLoader> accessibleLoaders,
    ClassLoader mappingLoader, ClassLoader targetLoader
  ) {
    if (owner.equals(target.name)) {
      return true;
    }

    ClassLoader loader = this.findClass(
      owner, mappingLoader, targetLoader
    ).getClassLoader();

    return loader == null || accessibleLoaders.contains(loader);
  }

  protected Set<ClassLoader> findParents(ClassLoader classLoader) {
    if (classLoader == null) {
      return Set.of(ClassLoader.getPlatformClassLoader());
    }

    Set<ClassLoader> classLoaders = Collections.newSetFromMap(new IdentityHashMap<>());

    classLoader = classLoader.getParent();
    while (classLoader != null && classLoaders.add(classLoader)) {
      classLoader = classLoader.getParent();
    }

    return classLoaders;
  }
}
