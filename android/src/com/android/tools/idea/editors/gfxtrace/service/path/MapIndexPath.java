/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * THIS FILE WAS GENERATED BY codergen. EDIT WITH CARE.
 */
package com.android.tools.idea.editors.gfxtrace.service.path;

import com.android.tools.rpclib.any.Box;
import com.android.tools.rpclib.binary.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public final class MapIndexPath extends Path {
  @Override
  public StringBuilder stringPath(StringBuilder builder) {
    return myMap.stringPath(builder).append("[").append(myKey).append("]");
  }

  //<<<Start:Java.ClassBody:1>>>
  Path myMap;
  Object myKey;

  // Constructs a default-initialized {@link MapIndexPath}.
  public MapIndexPath() {}


  public Path getMap() {
    return myMap;
  }

  public MapIndexPath setMap(Path v) {
    myMap = v;
    return this;
  }

  public Object getKey() {
    return myKey;
  }

  public MapIndexPath setKey(Object v) {
    myKey = v;
    return this;
  }

  @Override @NotNull
  public BinaryClass klass() { return Klass.INSTANCE; }

  private static final byte[] IDBytes = {13, 70, 86, -13, 29, -70, -7, -40, 94, -49, -52, 14, -124, -109, 56, 91, -69, -46, -20, -34, };
  public static final BinaryID ID = new BinaryID(IDBytes);

  static {
    Namespace.register(ID, Klass.INSTANCE);
  }
  public static void register() {}
  //<<<End:Java.ClassBody:1>>>
  public enum Klass implements BinaryClass {
    //<<<Start:Java.KlassBody:2>>>
    INSTANCE;

    @Override @NotNull
    public BinaryID id() { return ID; }

    @Override @NotNull
    public BinaryObject create() { return new MapIndexPath(); }

    @Override
    public void encode(@NotNull Encoder e, BinaryObject obj) throws IOException {
      MapIndexPath o = (MapIndexPath)obj;
      e.object(o.myMap.unwrap());
      e.variant(Box.wrap(o.myKey));
    }

    @Override
    public void decode(@NotNull Decoder d, BinaryObject obj) throws IOException {
      MapIndexPath o = (MapIndexPath)obj;
      o.myMap = Path.wrap(d.object());
      o.myKey = ((Box)d.variant()).unwrap();
    }
    //<<<End:Java.KlassBody:2>>>
  }
}
