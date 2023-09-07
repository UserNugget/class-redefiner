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

import io.github.usernugget.redefiner.handlers.types.HeadHandler;
import io.github.usernugget.redefiner.handlers.types.RawHandler;
import io.github.usernugget.redefiner.handlers.types.ReplaceHandler;
import io.github.usernugget.redefiner.handlers.types.TailHandler;
import io.github.usernugget.redefiner.handlers.types.annotations.Head;
import io.github.usernugget.redefiner.handlers.types.annotations.Raw;
import io.github.usernugget.redefiner.handlers.types.annotations.Replace;
import io.github.usernugget.redefiner.handlers.types.annotations.Tail;
import io.github.usernugget.redefiner.handlers.types.global.CrossClassLoaderHandler;
import io.github.usernugget.redefiner.handlers.types.global.OpHandler;
import io.github.usernugget.redefiner.handlers.types.global.RedirectHandler;
import org.objectweb.asm.Type;

public class StandardHandlerTypes extends HandlerTypes {
  public StandardHandlerTypes() {
    this.add(5000, Type.getType(Raw.class), new RawHandler());

    this.add(3002, "*", new RedirectHandler());
    this.add(3001, "*", new OpHandler());
    this.add(3000, "*", new CrossClassLoaderHandler());

    this.add(1002, Type.getType(Replace.class), new ReplaceHandler());
    this.add(1001, Type.getType(Tail.class), new TailHandler());
    this.add(1000, Type.getType(Head.class), new HeadHandler());
  }
}
