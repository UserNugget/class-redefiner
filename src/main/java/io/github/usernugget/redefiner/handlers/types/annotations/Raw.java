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

package io.github.usernugget.redefiner.handlers.types.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Calls mapping method to let it change bytecode directly.
 * <p>
 * Example:
 * <pre>{@code
 * @Raw
 * public static void raw(MethodChange change) {
 *   ClassMethod target = change.findTargetMethod();
 *   Insns targetCode = target.getInstructions();
 *
 *   targetCode.clear();
 *
 *   // return 2;
 *   targetCode.ldc(2);
 *   targetCode.op(Opcodes.IRETURN);
 * }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Raw {
  String method() default "";
}
