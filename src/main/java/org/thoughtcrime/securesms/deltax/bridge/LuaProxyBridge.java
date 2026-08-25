package org.thoughtcrime.securesms.deltax.bridge;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.CoerceLuaToJava;

public class LuaProxyBridge {
  public void register(Globals globals) {
    globals.set(
        "createProxy",
        new TwoArgFunction() {
          @Override
          public LuaValue call(LuaValue className, LuaValue methods) {
            try {
              Class<?> clazz = Class.forName(className.tojstring());
              LuaTable methodTable = methods.checktable();
              return createProxy(clazz, methodTable);

            } catch (Exception e) {
              throw new LuaError(e);
            }
          }
        });
    globals.set(
        "createProxyFor",
        new VarArgFunction() {
          @Override
          public Varargs invoke(Varargs args) {
            LuaTable table = args.checktable(1);
            LuaTable methods = args.checktable(2);
            List<Class<?>> interfaces = new ArrayList<>();
            LuaValue k = LuaValue.NIL;
            while (true) {
              Varargs n = table.next(k);
              k = n.arg1();
              if (k.isnil()) break;
              try {
                interfaces.add(Class.forName(n.arg(2).tojstring()));

              } catch (ClassNotFoundException e) {
                throw new LuaError(e);
              }
            }
            if (interfaces.isEmpty()) throw new LuaError("at least one interface required");
            return createProxyForInterfaces(interfaces, methods);
          }
        });
    globals.set(
        "isProxy",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue obj) {
            if (!obj.isuserdata()) return LuaValue.FALSE;
            Object javaObj = obj.checkuserdata(Object.class);
            return LuaValue.valueOf(Proxy.isProxyClass(javaObj.getClass()));
          }
        });
    globals.set(
        "getProxyHandler",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue obj) {
            if (!obj.isuserdata()) return LuaValue.NIL;
            Object javaObj = obj.checkuserdata(Object.class);
            if (!Proxy.isProxyClass(javaObj.getClass())) return LuaValue.NIL;
            InvocationHandler handler = Proxy.getInvocationHandler(javaObj);
            if (handler instanceof LuaInvocationHandler) {
              return ((LuaInvocationHandler) handler).luaMethods;
            }
            return LuaValue.NIL;
          }
        });
  }

  private LuaValue createProxy(Class<?> clazz, LuaTable methods) {
    if (clazz.isInterface()) {
      return createProxyForInterfaces(java.util.Collections.singletonList(clazz), methods);
    }
    Class<?>[] interfaces = clazz.getInterfaces();
    List<Class<?>> ifaces = new ArrayList<>();
    for (Class<?> i : interfaces) {
      if (i.isInterface()) ifaces.add(i);
    }
    if (!ifaces.isEmpty()) {
      return createProxyForInterfaces(ifaces, methods);
    }
    throw new LuaError(
        "cannot proxy class "
            + clazz.getName()
            + ": not an interface and has no interfaces to delegate to");
  }

  private LuaValue createProxyForInterfaces(List<Class<?>> interfaces, LuaTable methods) {
    LuaInvocationHandler handler = new LuaInvocationHandler(methods);
    Object proxy =
        Proxy.newProxyInstance(
            interfaces.get(0).getClassLoader(), interfaces.toArray(new Class<?>[0]), handler);
    return CoerceJavaToLua.coerce(proxy);
  }

  static class LuaInvocationHandler implements InvocationHandler {
    final LuaTable luaMethods;

    LuaInvocationHandler(LuaTable methods) {
      this.luaMethods = methods;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
      LuaValue luaFunc = luaMethods.get(method.getName());
      if (luaFunc.isfunction()) {
        LuaValue result;
        if (args != null && args.length > 0) {
          LuaValue[] luaArgs = new LuaValue[args.length];
          for (int i = 0; i < args.length; i++) {
            luaArgs[i] = CoerceJavaToLua.coerce(args[i]);
          }
          result = callLuaFunc(luaFunc.checkfunction(), luaArgs);

        } else {
          result = luaFunc.checkfunction().call();
        }
        return coerceResult(result, method.getReturnType());
      }
      switch (method.getName()) {
        case "equals":
          return proxy == (args != null ? args[0] : null);
        case "hashCode":
          return System.identityHashCode(proxy);
        case "toString":
          return "LuaProxy@" + Integer.toHexString(System.identityHashCode(proxy));
        default:
          return null;
      }
    }

    private LuaValue callLuaFunc(org.luaj.vm2.LuaFunction func, LuaValue[] luaArgs) {
      switch (luaArgs.length) {
        case 0:
          return func.call();
        case 1:
          return func.call(luaArgs[0]);
        case 2:
          return func.call(luaArgs[0], luaArgs[1]);
        case 3:
          return func.call(luaArgs[0], luaArgs[1], luaArgs[2]);
        default:
          return func.invoke(LuaValue.varargsOf(luaArgs)).arg1();
      }
    }

    private Object coerceResult(LuaValue result, Class<?> returnType) {
      if (returnType == Void.TYPE || returnType == Void.class) return null;
      if (result.isnil()) return null;
      return CoerceLuaToJava.coerce(result, returnType);
    }
  }
}
