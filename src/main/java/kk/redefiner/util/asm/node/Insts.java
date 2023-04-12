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

package kk.redefiner.util.asm.node;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import java.lang.reflect.Modifier;
import static org.objectweb.asm.Opcodes.ATHROW;
import static org.objectweb.asm.Opcodes.BIPUSH;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DCONST_0;
import static org.objectweb.asm.Opcodes.DCONST_1;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.FCONST_0;
import static org.objectweb.asm.Opcodes.FCONST_1;
import static org.objectweb.asm.Opcodes.FCONST_2;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.PUTSTATIC;
import static org.objectweb.asm.Opcodes.SIPUSH;

public class Insts extends InsnList {
  public Insts() { }

  public void jump(int opcode, LabelNode label) {
    add(new JumpInsnNode(opcode, label));
  }

  public void var(int opcode, int var) {
    add(new VarInsnNode(opcode, var));
  }

  public void returnOp(Type type) {
    op(type.getOpcode(IRETURN));
  }

  public void get(ClassFile owner, String fieldName, String fieldDesc) {
    get(owner, owner.findField(fieldName, fieldDesc));
  }

  public void get(ClassFile owner, ClassField field) {
    fieldOp(field.isStatic() ? GETSTATIC : GETFIELD, owner, field);
  }

  public void put(ClassFile owner, String fieldName, String fieldDesc) {
    put(owner, owner.findField(fieldName, fieldDesc));
  }

  public void put(ClassFile owner, ClassField field) {
    fieldOp(field.isStatic() ? PUTSTATIC : PUTFIELD, owner, field);
  }

  public void fieldOp(int opcode, ClassFile owner, ClassField field) {
    fieldOp(opcode, owner.name, field.name, field.desc);
  }

  public void fieldOp(int opcode, String owner, String fieldName, String fieldDesc) {
    add(new FieldInsnNode(opcode, owner, fieldName, fieldDesc));
  }

  public void invoke(ClassFile owner, MethodInsnNode methodNode) {
    invoke(owner, methodNode.name, methodNode.desc);
  }

  public void invoke(ClassFile owner, String methodName) {
    invoke(owner, methodName, null);
  }

  public void invoke(ClassFile owner, String methodName, String methodDesc) {
    invoke(owner, owner.findMethod(methodName, methodDesc));
  }

  public void invoke(ClassFile owner, ClassMethod method) {
    boolean isInterface = owner.isInterface();

    int opcode;
    if (method.isStatic()) {
      opcode = INVOKESTATIC;
    } else if (method.isInitializer()) {
      opcode = INVOKESPECIAL;
    } else if (isInterface) {
      opcode = INVOKEINTERFACE;
    } else {
      opcode = INVOKEVIRTUAL;
    }

    add(new MethodInsnNode(opcode, owner.name, method.name, method.desc, isInterface));
  }

  public void invoke(int opcode, ClassFile owner, String methodName, String methodDesc) {
    invoke(opcode, owner, owner.findMethod(methodName, methodDesc));
  }

  public void invoke(int opcode, ClassFile owner, ClassMethod method) {
    invoke(opcode, owner.name, method.name, method.desc, owner.isInterface());
  }

  public void invoke(int opcode, String owner, String methodName, String methodDesc, boolean isInterface) {
    add(new MethodInsnNode(opcode, owner, methodName, methodDesc, isInterface));
  }

  public void throwException(String throwable, String message) {
    type(NEW, throwable);
    op(DUP);
    ldc(message);
    invoke(INVOKESPECIAL, throwable, "<init>", "(Ljava/lang/String;)V", false);
    op(ATHROW);
  }

  public void load(ClassMethod method) {
    load(method.desc, method.access);
  }

  public void load(String desc, int access) {
    load(Type.getMethodType(desc), Modifier.isStatic(access) ? 0 : 1);
  }

  public void load(Type methodType, int offset) {
    for (Type argumentType : methodType.getArgumentTypes()) {
      var(argumentType.getOpcode(ILOAD), offset);
      offset += argumentType.getSize();
    }
  }

  public void ldc(Object obj) {
    if (obj instanceof Boolean) {
      ldc((boolean) obj);
    } else if (obj instanceof Byte) {
      ldc((byte) obj);
    } else if (obj instanceof Short) {
      ldc((short) obj);
    } else if (obj instanceof Character) {
      ldc((char) obj);
    } else if (obj instanceof Integer) {
      ldc((int) obj);
    } else if (obj instanceof Float) {
      ldc((float) obj);
    } else if (obj instanceof Long) {
      ldc((long) obj);
    } else if (obj instanceof Double) {
      ldc((double) obj);
    } else {
      add(new LdcInsnNode(obj));
    }
  }

  public void ldc(boolean value) {
    op(value ? ICONST_0 : ICONST_1);
  }

  public void ldc(byte value) {
    if (value <= 5 && value >= -1) {
      op(value + ICONST_0);
    } else {
      bipush(value);
    }
  }

  public void ldc(short value) {
    if (value <= 5 && value >= -1) {
      op(value + ICONST_0);
    } else if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
      bipush(value);
    } else {
      sipush(value);
    }
  }

  public void ldc(int value) {
    if (value <= 5 && value >= -1) {
      op(value + ICONST_0);
    } else if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
      bipush(value);
    } else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
      sipush(value);
    } else {
      add(new LdcInsnNode(value));
    }
  }

  public void ldc(long value) {
    add(new LdcInsnNode(value));
  }

  public void ldc(float value) {
    if (value == 0) {
      op(FCONST_0);
    } else if (value == 1) {
      op(FCONST_1);
    } else if (value == 2) {
      op(FCONST_2);
    } else {
      add(new LdcInsnNode(value));
    }
  }

  public void ldc(double value) {
    if (value == 0) {
      op(DCONST_0);
    } else if (value == 1) {
      op(DCONST_1);
    } else {
      add(new LdcInsnNode(value));
    }
  }

  public void type(int opcode, Type type) {
    type(opcode, type.getInternalName());
  }

  public void type(int opcode, String type) {
    add(new TypeInsnNode(opcode, type));
  }

  public void cast(Type asmType, String type) {
    if (asmType.getSort() == Type.OBJECT || asmType.getSort() == Type.ARRAY) {
      add(new TypeInsnNode(CHECKCAST, type));
    }
  }

  public void bipush(int value) {
    intOp(BIPUSH, value);
  }

  public void sipush(int value) {
    intOp(SIPUSH, value);
  }

  public void intOp(int opcode, int value) {
    add(new IntInsnNode(opcode, value));
  }

  public void op(int opcode) {
    add(new InsnNode(opcode));
  }
}
