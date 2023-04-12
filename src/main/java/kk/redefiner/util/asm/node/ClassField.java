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

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.FieldNode;
import static org.objectweb.asm.Opcodes.ASM9;

public class ClassField extends FieldNode {
  public ClassField(int access, String name, String descriptor, String signature, Object value) {
    this(ASM9, access, name, descriptor, signature, value);
  }

  public ClassField(int api, int access, String name, String descriptor, String signature, Object value) {
    super(api, access, name, descriptor, signature, value);
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

  public boolean isFinal() {
    return (this.access & Opcodes.ACC_FINAL) != 0;
  }

  public boolean isVolatile() {
    return (this.access & Opcodes.ACC_VOLATILE) != 0;
  }

  public boolean isTransient() {
    return (this.access & Opcodes.ACC_TRANSIENT) != 0;
  }

  public boolean isSynthetic() {
    return (this.access & Opcodes.ACC_SYNTHETIC) != 0;
  }

  public boolean isEnum() {
    return (this.access & Opcodes.ACC_ENUM) != 0;
  }

  public boolean isMandated() {
    return (this.access & Opcodes.ACC_MANDATED) != 0;
  }

  public boolean isDeprecated() {
    return (this.access & Opcodes.ACC_DEPRECATED) != 0;
  }
}
