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

package io.github.usernugget.redefiner.examples;

import io.github.usernugget.redefiner.ClassRedefiner;
import io.github.usernugget.redefiner.annotation.GetField;
import io.github.usernugget.redefiner.annotation.Mapping;
import io.github.usernugget.redefiner.annotation.PutField;
import io.github.usernugget.redefiner.registry.DefaultAnnotationRegistry;
import io.github.usernugget.redefiner.throwable.RedefineFailedException;

public class FieldExample {
  @Mapping(ClassValue.class)
  public static final class ClassValueMapping {
    @GetField(field = "superField")
    public static boolean getSuperField(boolean value) {
      return !value;
    }

    @PutField(field = "superField")
    public static boolean setSuperField(boolean value) {
      return value;
    }
  }

  public static final class ClassValue {
    private static boolean superField = true;

    public static boolean getSuperField() {
      return superField;
    }

    public static void setSuperField(boolean value) {
      superField = value;
    }
  }

  public static void main(String[] args) throws RedefineFailedException {
    ClassRedefiner redefiner = new ClassRedefiner(
       new DefaultAnnotationRegistry()
    );

    System.out.println("superField = " + ClassValue.getSuperField());

    redefiner.applyMapping(ClassValueMapping.class);

    ClassValue.setSuperField(false);
    System.out.println("superField = " + ClassValue.getSuperField());
  }
}
