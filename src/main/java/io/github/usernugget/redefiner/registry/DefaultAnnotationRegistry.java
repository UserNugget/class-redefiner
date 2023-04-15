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

package io.github.usernugget.redefiner.registry;

import io.github.usernugget.redefiner.annotation.GetField;
import io.github.usernugget.redefiner.annotation.Head;
import io.github.usernugget.redefiner.annotation.Inject;
import io.github.usernugget.redefiner.annotation.PutField;
import io.github.usernugget.redefiner.annotation.Raw;
import io.github.usernugget.redefiner.annotation.Replace;
import io.github.usernugget.redefiner.annotation.Tail;
import io.github.usernugget.redefiner.annotation.Var;
import io.github.usernugget.redefiner.handlers.GetFieldHandler;
import io.github.usernugget.redefiner.handlers.HeadHandler;
import io.github.usernugget.redefiner.handlers.InjectHandler;
import io.github.usernugget.redefiner.handlers.PutFieldHandler;
import io.github.usernugget.redefiner.handlers.RawHandler;
import io.github.usernugget.redefiner.handlers.ReplaceHandler;
import io.github.usernugget.redefiner.handlers.TailHandler;
import io.github.usernugget.redefiner.handlers.VarHandler;

public class DefaultAnnotationRegistry extends AnnotationRegistry {
  public DefaultAnnotationRegistry() {
    register(Head.class, new HeadHandler());
    register(Tail.class, new TailHandler());
    register(Replace.class, new ReplaceHandler());
    register(Raw.class, new RawHandler());
    register(GetField.class, new GetFieldHandler());
    register(PutField.class, new PutFieldHandler());
    register(Inject.class, new InjectHandler());
    register(Var.class, new VarHandler());
  }
}
