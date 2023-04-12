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

package kk.redefiner.registry;

import org.objectweb.asm.Type;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

public class AnnotationRegistry {
  private final Map<String, AnnotationHandler> handlers = new HashMap<>();

  public void register(Class<? extends Annotation> remapperType, AnnotationHandler handler) {
    this.handlers.put(Type.getDescriptor(remapperType), handler);
  }

  public AnnotationHandler getHandler(String desc) {
    return this.handlers.get(desc);
  }
}
