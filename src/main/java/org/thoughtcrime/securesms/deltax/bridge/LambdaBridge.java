package org.thoughtcrime.securesms.deltax.bridge;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.CoerceLuaToJava;

public class LambdaBridge {
  public void register(Globals globals) {
    globals.set(
        "createSAM",
        new TwoArgFunction() {
          @Override
          public LuaValue call(LuaValue className, LuaValue func) {
            try {
              Class<?> clazz = Class.forName(className.tojstring());
              return createSAMProxy(clazz, func.checkfunction());

            } catch (ClassNotFoundException e) {
              throw new LuaError(e);
            }
          }
        });
    globals.set(
        "wrapLambda",
        new TwoArgFunction() {
          @Override
          public LuaValue call(LuaValue func, LuaValue interfaceClass) {
            try {
              Class<?> clazz = Class.forName(interfaceClass.tojstring());
              LuaValue luaFunc = func.checkfunction();
              if (!clazz.isInterface()) throw new LuaError(clazz + " is not an interface");
              Method sam = findSAMMethod(clazz);
              if (sam == null) throw new LuaError(clazz + " is not a functional interface");
              return createSAMProxy(clazz, luaFunc.checkfunction());

            } catch (ClassNotFoundException e) {
              throw new LuaError(e);
            }
          }
        });
    globals.set(
        "isFunctionalInterface",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue className) {
            try {
              Class<?> clazz = Class.forName(className.tojstring());
              return LuaValue.valueOf(clazz.isInterface() && findSAMMethod(clazz) != null);

            } catch (Exception e) {
              return LuaValue.FALSE;
            }
          }
        });
    globals.set(
        "createRunnable",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue func) {
            return createSAMProxy(Runnable.class, func.checkfunction());
          }
        });
    globals.set(
        "createSupplier",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue func) {
            return createSAMProxy(java.util.function.Supplier.class, func.checkfunction());
          }
        });
    globals.set(
        "createConsumer",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue func) {
            return createSAMProxy(java.util.function.Consumer.class, func.checkfunction());
          }
        });
    globals.set(
        "createFunction",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue func) {
            return createSAMProxy(java.util.function.Function.class, func.checkfunction());
          }
        });
    globals.set(
        "createPredicate",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue func) {
            return createSAMProxy(java.util.function.Predicate.class, func.checkfunction());
          }
        });
  }

  private Method findSAMMethod(Class<?> clazz) {
    List<Method> methods = new ArrayList<>();
    for (Method m : clazz.getMethods()) {
      if (m.getDeclaringClass() == clazz && Modifier.isAbstract(m.getModifiers())) {
        methods.add(m);
      }
    }
    return methods.size() == 1 ? methods.get(0) : null;
  }

  private LuaValue createSAMProxy(Class<?> clazz, org.luaj.vm2.LuaFunction func) {
    SAMInvocationHandler handler = new SAMInvocationHandler(func);
    Object proxy = Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[] {clazz}, handler);
    return CoerceJavaToLua.coerce(proxy);
  }

  static class SAMInvocationHandler implements InvocationHandler {
    private final org.luaj.vm2.LuaFunction func;

    SAMInvocationHandler(org.luaj.vm2.LuaFunction func) {
      this.func = func;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
      if (method.getDeclaringClass() == Object.class) {
        switch (method.getName()) {
          case "equals":
            return proxy == (args != null ? args[0] : null);
          case "hashCode":
            return System.identityHashCode(proxy);
          case "toString":
            return "LambdaProxy@" + Integer.toHexString(System.identityHashCode(proxy));
          default:
            return null;
        }
      }
      LuaValue result;
      if (args != null && args.length > 0) {
        LuaValue[] luaArgs = new LuaValue[args.length];
        for (int i = 0; i < args.length; i++) {
          luaArgs[i] = CoerceJavaToLua.coerce(args[i]);
        }
        result = callLuaFunc(func, luaArgs);

      } else {
        result = func.call();
      }
      if (method.getReturnType() == Void.TYPE || method.getReturnType() == Void.class) return null;
      if (result.isnil()) return null;
      return CoerceLuaToJava.coerce(result, method.getReturnType());
    }

    private LuaValue callLuaFunc(org.luaj.vm2.LuaFunction function, LuaValue[] luaArgs) {
      switch (luaArgs.length) {
        case 0:
          return function.call();
        case 1:
          return function.call(luaArgs[0]);
        case 2:
          return function.call(luaArgs[0], luaArgs[1]);
        case 3:
          return function.call(luaArgs[0], luaArgs[1], luaArgs[2]);
        default:
          return function.invoke(LuaValue.varargsOf(luaArgs)).arg1();
      }
    }
  }
}
