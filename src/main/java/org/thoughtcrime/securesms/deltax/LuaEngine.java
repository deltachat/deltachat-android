package org.thoughtcrime.securesms.deltax;

import android.content.Context;
import android.util.Log;
import java.io.File;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.JsePlatform;
import org.thoughtcrime.securesms.deltax.bridge.JavaBridge;

/**
 * Core of the DeltaX plugin engine. Sets up a fresh Lua environment for each plugin, registers the
 * Java-interop bridges and a small set of DeltaChat-relevant globals.
 *
 * <p>MinecraftX shipped a Bukkit/Adventure driven engine (events, broadcasts, titles, scheduled
 * tasks on the server thread, ...). Those are specific to a game server and are intentionally
 * dropped here; plugins interact with DeltaChat through the exposed Android {@code Context}, the
 * {@code deltax} manager and the Java bridges.
 */
public class LuaEngine {

  private static final String TAG = "DeltaX";
  private final Context context;
  private final DeltaX deltaX;

  public LuaEngine(Context context, DeltaX deltaX) {
    this.context = context.getApplicationContext();
    this.deltaX = deltaX;
  }

  public Globals createGlobals() {
    Globals globals = JsePlatform.standardGlobals();
    ClassLoader classLoader = context.getClassLoader();
    LuaValue luajava = globals.get("luajava");
    if (luajava.istable()) {
      ((org.luaj.vm2.LuaTable) luajava)
          .set(
              "bindClass",
              new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue className) {
                  try {
                    Class<?> clazz = Class.forName(className.tojstring(), true, classLoader);
                    return CoerceJavaToLua.coerce(clazz);
                  } catch (Exception e) {
                    throw new LuaError(e);
                  }
                }
              });
    }
    globals.set("context", CoerceJavaToLua.coerce(context));
    globals.set("deltax", CoerceJavaToLua.coerce(deltaX));
    registerLogHelper(globals);
    new JavaBridge().registerAll(globals);
    return globals;
  }

  public Object runScript(File file, Globals globals) {
    String name = file.getName();
    int dot = name.lastIndexOf('.');
    String scriptName = dot > 0 ? name.substring(0, dot) : name;
    globals.set("SCRIPT_NAME", LuaValue.valueOf(scriptName));
    LuaValue chunk = globals.loadfile(file.getAbsolutePath());
    return chunk.call();
  }

  private void registerLogHelper(Globals globals) {
    globals.set(
        "log",
        new TwoArgFunction() {
          @Override
          public LuaValue call(LuaValue level, LuaValue message) {
            String lvl = level.tojstring().toUpperCase(java.util.Locale.ROOT);
            String msg = message.tojstring();
            switch (lvl) {
              case "ERROR":
              case "SEVERE":
                Log.e(TAG, msg);
                break;
              case "WARN":
              case "WARNING":
                Log.w(TAG, msg);
                break;
              case "DEBUG":
                Log.d(TAG, msg);
                break;
              case "VERBOSE":
                Log.v(TAG, msg);
                break;
              default:
                Log.i(TAG, msg);
            }
            return LuaValue.NIL;
          }
        });
  }
}
