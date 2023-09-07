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

import org.objectweb.asm.Opcodes;

public interface AccessFlags {
  int access();

  default boolean isPublic() { return (access() & Opcodes.ACC_PUBLIC) != 0; }
  default boolean isPrivate() { return (access() & Opcodes.ACC_PRIVATE) != 0; }
  default boolean isProtected() { return (access() & Opcodes.ACC_PROTECTED) != 0; }
  default boolean isStatic() { return (access() & Opcodes.ACC_STATIC) != 0; }
  default boolean isFinal() { return (access() & Opcodes.ACC_FINAL) != 0; }
  default boolean isSuper() { return (access() & Opcodes.ACC_SUPER) != 0; }
  default boolean isSynchronized() { return (access() & Opcodes.ACC_SYNCHRONIZED) != 0; }
  default boolean isVolatile() { return (access() & Opcodes.ACC_VOLATILE) != 0; }
  default boolean isBridge() { return (access() & Opcodes.ACC_BRIDGE) != 0; }
  default boolean isInterface() { return (access() & Opcodes.ACC_INTERFACE) != 0; }
  default boolean isStrict() { return (access() & Opcodes.ACC_STRICT) != 0; }
  default boolean isSynthetic() { return (access() & Opcodes.ACC_SYNTHETIC) != 0; }
  default boolean isAnnotation() { return (access() & Opcodes.ACC_ANNOTATION) != 0; }
  default boolean isEnum() { return (access() & Opcodes.ACC_ENUM) != 0; }
  default boolean isMandated() { return (access() & Opcodes.ACC_MANDATED) != 0; }
}
