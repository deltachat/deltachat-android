package org.thoughtcrime.securesms.deltax.bridge;

import org.luaj.vm2.Globals;

/**
 * Registers all reusable Java&lt;-&gt;Lua bridges. Ported from MinecraftX; only the engine-agnostic
 * bridges are kept (LuaProxy, Lambda/SAM, annotations, async/concurrency, reflection and
 * collections). The Bukkit/Adventure helpers were server specific and removed.
 */
public class JavaBridge {

  public void registerAll(Globals globals) {
    new LuaProxyBridge().register(globals);
    new LambdaBridge().register(globals);
    new AnnotationBridge().register(globals);
    new AsyncBridge().register(globals);
    new ReflectionBridge().register(globals);
    new CollectionBridge().register(globals);
  }
}
