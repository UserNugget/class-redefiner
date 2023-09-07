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

import java.util.List;
import java.util.function.Consumer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public final class Ops {
  public static InsnNode op(int op) {
    return new InsnNode(op);
  }

  public static IntInsnNode intOp(int op, int value) {
    return new IntInsnNode(op, value);
  }

  public static VarInsnNode varOp(int op, int var) {
    return new VarInsnNode(op, var);
  }

  public static VarInsnNode varOp(Type type, int op, int var) {
    return varOp(type.getOpcode(op), var);
  }

  public static IincInsnNode iincOp(int var, int increment) {
    return new IincInsnNode(var, increment);
  }

  public static JumpInsnNode jumpOp(int op, LabelNode label) {
    return new JumpInsnNode(op, label);
  }

  public static TypeInsnNode typeOp(int op, String type) {
    return new TypeInsnNode(op, type);
  }

  public static FieldInsnNode fieldOp(int op, ClassField field) {
    return fieldOp(op, field.owner.name, field.name, field.desc);
  }

  public static FieldInsnNode fieldOp(int op, String owner, String field, String desc) {
    return new FieldInsnNode(op, owner, field, desc);
  }

  public static MethodInsnNode methodOp(int op, ClassMethod method) {
    return methodOp(op, method.owner.name, method.name, method.desc, method.owner.isInterface());
  }

  public static MethodInsnNode methodOp(int op, String owner, String method, String desc, boolean itf) {
    return new MethodInsnNode(op, owner, method, desc, itf);
  }

  public static AbstractInsnNode ldc(Object value) {
    if (value instanceof Number) {
      if (value instanceof Long) {
        return ldc((long) value);
      } else if (value instanceof Double) {
        return ldc((double) value);
      } else if (value instanceof Float) {
        return ldc((float) value);
      }

      return ldc(((Number) value).intValue());
    } if (value instanceof Character) {
      return ldc((char) value);
    } else if (value instanceof Boolean) {
      return ldc((boolean) value);
    }

    if (value == null) {
      return op(Opcodes.ACONST_NULL);
    }

    return new LdcInsnNode(value);
  }

  public static InsnNode ldc(boolean value) {
    return op(value ? Opcodes.ICONST_1 : Opcodes.ICONST_0);
  }

  public static AbstractInsnNode ldc(int value) {
    if (value >= -1 && value <= 5) {
      return op(Opcodes.ICONST_0 + value);
    } else if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
      return intOp(Opcodes.BIPUSH, value);
    } else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
      return intOp(Opcodes.SIPUSH, value);
    }

    return new LdcInsnNode(value);
  }

  public static AbstractInsnNode ldc(float value) {
    if (value == 0 || value == 1 || value == 2) {
      return op((int) (Opcodes.FCONST_0 + value));
    }

    return new LdcInsnNode(value);
  }

  public static AbstractInsnNode ldc(long value) {
    if (value == 0 || value == 1) {
      return op((int) (Opcodes.LCONST_0 + value));
    }

    return new LdcInsnNode(value);
  }

  public static AbstractInsnNode ldc(double value) {
    if (value == 0 || value == 1) {
      return op((int) (Opcodes.DCONST_0 + value));
    }

    return new LdcInsnNode(value);
  }

  public static VarInsnNode loadOp(Type type, int var) {
    return varOp(type, Opcodes.ILOAD, var);
  }

  public static VarInsnNode storeOp(Type type, int var) {
    return varOp(type, Opcodes.ISTORE, var);
  }

  public static FieldInsnNode fieldGetter(ClassField field) {
    return fieldOp(
      field.isStatic() ? Opcodes.GETSTATIC : Opcodes.GETFIELD,
      field.owner.name, field.name, field.desc
    );
  }

  public static FieldInsnNode fieldSetter(ClassField field) {
    return fieldOp(
      field.isStatic() ? Opcodes.PUTSTATIC : Opcodes.PUTFIELD,
      field.owner.name, field.name, field.desc
    );
  }

  public static MethodInsnNode invoke(ClassMethod method) {
    boolean isInterface = method.owner.isInterface();

    int op;
    if (method.isStatic()) {
      op = Opcodes.INVOKESTATIC;
    } else if (method.isSpecial()) {
      op = Opcodes.INVOKESPECIAL;
    } else if (isInterface) {
      op = Opcodes.INVOKEINTERFACE;
    } else {
      op = Opcodes.INVOKEVIRTUAL;
    }

    return methodOp(
      op, method.owner.name, method.name, method.desc, isInterface
    );
  }

  public static InsnNode returnOp(String desc) {
    return returnOp(Type.getType(desc));
  }

  public static InsnNode returnOp(Type type) {
    return op(type.getOpcode(Opcodes.IRETURN));
  }

  static void eachAnnotation(
    Consumer<AnnotationNode> consumer,
    List<AnnotationNode> visible, List<AnnotationNode> invisible
  ) {
    if (visible != null) {
      for (AnnotationNode node : visible) {
        consumer.accept(node);
      }
    }

    if (invisible != null) {
      for (AnnotationNode node : invisible) {
        consumer.accept(node);
      }
    }
  }
}
