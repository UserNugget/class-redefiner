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

package kk.redefiner.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects a new method that will wrap input of putfield/putstatic instructions
 * <p>
 *
 * Limitations:
 * <p>
 * 1) Static final constant fields (like {@code FIELD = 1 << 1;} or {@code FIELD = true;})
 * will be inlined by the compiler, so cannot be wrapped
 * <p>
 *
 * You can see example in {@link kk.examples.FieldExample}
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PutField {
  String method() default "";
  String field();

  int offset() default -1;
}
