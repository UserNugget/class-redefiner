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

import kk.redefiner.annotation.GetField;
import kk.redefiner.annotation.Head;
import kk.redefiner.annotation.Inject;
import kk.redefiner.annotation.PutField;
import kk.redefiner.annotation.Raw;
import kk.redefiner.annotation.Replace;
import kk.redefiner.annotation.Tail;
import kk.redefiner.annotation.Var;
import kk.redefiner.handlers.GetFieldHandler;
import kk.redefiner.handlers.HeadHandler;
import kk.redefiner.handlers.InjectHandler;
import kk.redefiner.handlers.PutFieldHandler;
import kk.redefiner.handlers.RawHandler;
import kk.redefiner.handlers.ReplaceHandler;
import kk.redefiner.handlers.TailHandler;
import kk.redefiner.handlers.VarHandler;

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
