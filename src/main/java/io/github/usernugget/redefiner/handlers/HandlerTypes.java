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

package io.github.usernugget.redefiner.handlers;

import io.github.usernugget.redefiner.handlers.HandlerTypes.HandlerDesc;
import io.github.usernugget.redefiner.util.collections.SortedList;
import java.util.function.Consumer;
import org.objectweb.asm.Type;

public class HandlerTypes extends SortedList<HandlerDesc> {
  public boolean add(int priority, Type type, Handler handler) {
    return this.add(priority, type.getDescriptor(), handler);
  }

  public boolean add(int priority, String desc, Handler handler) {
    if (desc == null) throw new NullPointerException("desc is null");
    if (handler == null) throw new NullPointerException("handler is null");

    return this.add(priority, new HandlerDesc(handler, desc));
  }

  public void eachHandler(String desc, Consumer<HandlerDesc> consumer) {
    for (HandlerDesc handlerDesc : getValues().values()) {
      if (handlerDesc.desc.equals(desc) || handlerDesc.desc.equals("*")) {
        consumer.accept(handlerDesc);
      }
    }
  }

  public static final class HandlerDesc {
    public Handler handler;
    public String desc;

    public HandlerDesc(Handler handler, String desc) {
      this.handler = handler;
      this.desc = desc;
    }
  }
}
