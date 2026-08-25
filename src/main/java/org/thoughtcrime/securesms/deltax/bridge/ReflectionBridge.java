package org.thoughtcrime.securesms.deltax.bridge;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.CoerceLuaToJava;

public class ReflectionBridge {
  public void register(Globals globals) {
    registerFieldHelpers(globals);
    registerMethodHelpers(globals);
    registerConstructorHelpers(globals);
    registerEnumHelpers(globals);
    registerArrayHelpers(globals);
    registerClassHelpers(globals);
    registerModifierHelpers(globals);
  }

  private void registerFieldHelpers(Globals globals) {
    globals.set(
        "getField",
        new TwoArgFunction() {
          @Override
          public LuaValue call(LuaValue obj, LuaValue fieldName) {
            Object target = obj.checkuserdata(Object.class);
            String name = fieldName.tojstring();
            Field field = findField(target.getClass(), name);
            field.setAccessible(true);
            try {
              return CoerceJavaToLua.coerce(field.get(target));

            } catch (Exception e) {
              throw new LuaError(e);
            }
          }
        });
    globals.set(
        "setField",
        new ThreeArgFunction() {
          @Override
          public LuaValue call(LuaValue obj, LuaValue fieldName, LuaValue value) {
            Object target = obj.checkuserdata(Object.class);
            String name = fieldName.tojstring();
            Field field = findField(target.getClass(), name);
            field.setAccessible(true);
            try {
              field.set(target, CoerceLuaToJava.coerce(value, field.getType()));

            } catch (Exception e) {
              throw new LuaError(e);
            }
            return LuaValue.NIL;
          }
        });
    globals.set(
        "getStaticField",
        new TwoArgFunction() {
          @Override
          public LuaValue call(LuaValue classObj, LuaValue fieldName) {
            Class<?> clazz = resolveClass(classObj);
            String name = fieldName.tojstring();
            Field field = findField(clazz, name);
            field.setAccessible(true);
            try {
              return CoerceJavaToLua.coerce(field.get(null));

            } catch (Exception e) {
              throw new LuaError(e);
            }
          }
        });
    globals.set(
        "setStaticField",
        new ThreeArgFunction() {
          @Override
          public LuaValue call(LuaValue classObj, LuaValue fieldName, LuaValue value) {
            Class<?> clazz = resolveClass(classObj);
            String name = fieldName.tojstring();
            Field field = findField(clazz, name);
            field.setAccessible(true);
            try {
              field.set(null, CoerceLuaToJava.coerce(value, field.getType()));

            } catch (Exception e) {
              throw new LuaError(e);
            }
            return LuaValue.NIL;
          }
        });
    globals.set(
        "getDeclaredFields",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue classObj) {
            Class<?> clazz = resolveClass(classObj);
            LuaTable table = new LuaTable();
            int idx = 1;
            for (Field f : clazz.getDeclaredFields()) {
              LuaTable ft = new LuaTable();
              ft.set("name", f.getName());
              ft.set("type", f.getType().getName());
              ft.set("modifiers", f.getModifiers());
              ft.set("declaringClass", f.getDeclaringClass().getName());
              table.set(idx++, ft);
            }
            return table;
          }
        });
    globals.set(
        "getFields",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue classObj) {
            Class<?> clazz = resolveClass(classObj);
            LuaTable table = new LuaTable();
            int idx = 1;
            for (Field f : clazz.getFields()) {
              LuaTable ft = new LuaTable();
              ft.set("name", f.getName());
              ft.set("type", f.getType().getName());
              ft.set("modifiers", f.getModifiers());
              table.set(idx++, ft);
            }
            return table;
          }
        });
  }

  private void registerMethodHelpers(Globals globals) {
    globals.set(
        "getMethods",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue classObj) {
            Class<?> clazz = resolveClass(classObj);
            LuaTable table = new LuaTable();
            int idx = 1;
            for (Method m : clazz.getMethods()) {
              table.set(idx++, CoerceJavaToLua.coerce(m));
            }
            return table;
          }
        });
    globals.set(
        "getDeclaredMethods",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue classObj) {
            Class<?> clazz = resolveClass(classObj);
            LuaTable table = new LuaTable();
            int idx = 1;
            for (Method m : clazz.getDeclaredMethods()) {
              table.set(idx++, CoerceJavaToLua.coerce(m));
            }
            return table;
          }
        });
    globals.set(
        "invokeMethod",
        new VarArgFunction() {
          @Override
          public Varargs invoke(Varargs args) {
            Object obj = args.checkuserdata(1, Object.class);
            String methodName = args.checkjstring(2);
            Object[] methodArgs = new Object[args.narg() - 2];
            for (int i = 3; i <= args.narg(); i++) {
              methodArgs[i - 3] = CoerceLuaToJava.coerce(args.arg(i), Object.class);
            }
            Method method = findMethod(obj.getClass(), methodName, methodArgs.length);
            method.setAccessible(true);
            try {
              return CoerceJavaToLua.coerce(method.invoke(obj, methodArgs));

            } catch (Exception e) {
              throw new LuaError(e);
            }
          }
        });
    globals.set(
        "invokeStatic",
        new VarArgFunction() {
          @Override
          public Varargs invoke(Varargs args) {
            Class<?> clazz = resolveClass(args.checkvalue(1));
            String methodName = args.checkjstring(2);
            Object[] methodArgs = new Object[args.narg() - 2];
            for (int i = 3; i <= args.narg(); i++) {
              methodArgs[i - 3] = CoerceLuaToJava.coerce(args.arg(i), Object.class);
            }
            Method method = findMethod(clazz, methodName, methodArgs.length);
            method.setAccessible(true);
            try {
              return CoerceJavaToLua.coerce(method.invoke(null, methodArgs));

            } catch (Exception e) {
              throw new LuaError(e);
            }
          }
        });
    globals.set(
        "methodInfo",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue methodObj) {
            Method method = (Method) methodObj.checkuserdata(Method.class);
            LuaTable table = new LuaTable();
            table.set("name", method.getName());
            table.set("returnType", method.getReturnType().getName());
            table.set("modifiers", method.getModifiers());
            table.set("declaringClass", method.getDeclaringClass().getName());
            LuaTable paramTypes = new LuaTable();
            Class<?>[] params = method.getParameterTypes();
            for (int i = 0; i < params.length; i++) {
              paramTypes.set(i + 1, params[i].getName());
            }
            table.set("parameterTypes", paramTypes);
            LuaTable exceptionTypes = new LuaTable();
            Class<?>[] exceptions = method.getExceptionTypes();
            for (int i = 0; i < exceptions.length; i++) {
              exceptionTypes.set(i + 1, exceptions[i].getName());
            }
            table.set("exceptionTypes", exceptionTypes);
            return table;
          }
        });
  }

  private void registerConstructorHelpers(Globals globals) {
    globals.set(
        "getConstructors",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue classObj) {
            Class<?> clazz = resolveClass(classObj);
            LuaTable table = new LuaTable();
            int idx = 1;
            for (Constructor<?> c : clazz.getConstructors()) {
              table.set(idx++, CoerceJavaToLua.coerce(c));
            }
            return table;
          }
        });
    globals.set(
        "getDeclaredConstructors",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue classObj) {
            Class<?> clazz = resolveClass(classObj);
            LuaTable table = new LuaTable();
            int idx = 1;
            for (Constructor<?> c : clazz.getDeclaredConstructors()) {
              table.set(idx++, CoerceJavaToLua.coerce(c));
            }
            return table;
          }
        });
    globals.set(
        "newInstance",
        new VarArgFunction() {
          @Override
          public Varargs invoke(Varargs args) {
            Class<?> clazz = resolveClass(args.checkvalue(1));
            Object[] initArgs = new Object[args.narg() - 1];
            for (int i = 2; i <= args.narg(); i++) {
              initArgs[i - 2] = CoerceLuaToJava.coerce(args.arg(i), Object.class);
            }
            Constructor<?> constructor = findConstructor(clazz, initArgs.length);
            constructor.setAccessible(true);
            try {
              return CoerceJavaToLua.coerce(constructor.newInstance(initArgs));

            } catch (Exception e) {
              throw new LuaError(e);
            }
          }
        });
  }

  private void registerEnumHelpers(Globals globals) {
    globals.set(
        "enumValues",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue classObj) {
            Class<?> clazz = resolveClass(classObj);
            if (!clazz.isEnum()) throw new LuaError(clazz.getName() + " is not an enum");
            Object[] values = clazz.getEnumConstants();
            LuaTable table = new LuaTable();
            for (int i = 0; i < values.length; i++) {
              table.set(i + 1, CoerceJavaToLua.coerce(values[i]));
            }
            return table;
          }
        });
    globals.set(
        "enumValueOf",
        new TwoArgFunction() {
          @Override
          public LuaValue call(LuaValue classObj, LuaValue name) {
            Class<?> clazz = resolveClass(classObj);
            if (!clazz.isEnum()) throw new LuaError(clazz.getName() + " is not an enum");
            String targetName = name.tojstring();
            for (Object v : clazz.getEnumConstants()) {
              if (((Enum<?>) v).name().equals(targetName)) {
                return CoerceJavaToLua.coerce(v);
              }
            }
            throw new LuaError("no enum constant " + clazz.getName() + "." + targetName);
          }
        });
    globals.set(
        "enumName",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue enumObj) {
            Enum<?> enumVal = (Enum) enumObj.checkuserdata(Enum.class);
            return LuaValue.valueOf(enumVal.name());
          }
        });
    globals.set(
        "enumOrdinal",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue enumObj) {
            Enum<?> enumVal = (Enum) enumObj.checkuserdata(Enum.class);
            return LuaValue.valueOf(enumVal.ordinal());
          }
        });
  }

  private void registerArrayHelpers(Globals globals) {
    globals.set(
        "arrayLength",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue arr) {
            Object javaArr = arr.checkuserdata(Object.class);
            return LuaValue.valueOf(java.lang.reflect.Array.getLength(javaArr));
          }
        });
    globals.set(
        "arrayGet",
        new TwoArgFunction() {
          @Override
          public LuaValue call(LuaValue arr, LuaValue index) {
            Object javaArr = arr.checkuserdata(Object.class);
            int idx = index.checkint() - 1;
            return CoerceJavaToLua.coerce(java.lang.reflect.Array.get(javaArr, idx));
          }
        });
    globals.set(
        "arraySet",
        new ThreeArgFunction() {
          @Override
          public LuaValue call(LuaValue arr, LuaValue index, LuaValue value) {
            Object javaArr = arr.checkuserdata(Object.class);
            int idx = index.checkint() - 1;
            Class<?> componentType = javaArr.getClass().getComponentType();
            java.lang.reflect.Array.set(javaArr, idx, CoerceLuaToJava.coerce(value, componentType));
            return LuaValue.NIL;
          }
        });
    globals.set(
        "newArray",
        new TwoArgFunction() {
          @Override
          public LuaValue call(LuaValue componentClass, LuaValue length) {
            try {
              Class<?> clazz = Class.forName(componentClass.tojstring());
              Object arr = java.lang.reflect.Array.newInstance(clazz, length.checkint());
              return CoerceJavaToLua.coerce(arr);

            } catch (Exception e) {
              throw new LuaError(e);
            }
          }
        });
    globals.set(
        "newPrimitiveArray",
        new TwoArgFunction() {
          @Override
          public LuaValue call(LuaValue typeName, LuaValue length) {
            String type = typeName.tojstring().toLowerCase(java.util.Locale.ROOT);
            int size = length.checkint();
            switch (type) {
              case "int":
                return CoerceJavaToLua.coerce(new int[size]);
              case "long":
                return CoerceJavaToLua.coerce(new long[size]);
              case "double":
                return CoerceJavaToLua.coerce(new double[size]);
              case "float":
                return CoerceJavaToLua.coerce(new float[size]);
              case "boolean":
                return CoerceJavaToLua.coerce(new boolean[size]);
              case "byte":
                return CoerceJavaToLua.coerce(new byte[size]);
              case "short":
                return CoerceJavaToLua.coerce(new short[size]);
              case "char":
                return CoerceJavaToLua.coerce(new char[size]);
              default:
                throw new LuaError("unknown primitive type: " + type);
            }
          }
        });
    globals.set(
        "arrayToList",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue obj) {
            Object javaArr = obj.checkuserdata(Object.class);
            if (!javaArr.getClass().isArray()) throw new LuaError("not an array");
            int len = java.lang.reflect.Array.getLength(javaArr);
            java.util.ArrayList<Object> list = new java.util.ArrayList<>(len);
            for (int i = 0; i < len; i++) {
              list.add(java.lang.reflect.Array.get(javaArr, i));
            }
            return CoerceJavaToLua.coerce(list);
          }
        });
  }

  private void registerClassHelpers(Globals globals) {
    globals.set(
        "classOf",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue obj) {
            if (!obj.isuserdata()) return LuaValue.NIL;
            Object javaObj = obj.checkuserdata(Object.class);
            return CoerceJavaToLua.coerce(javaObj.getClass());
          }
        });
    globals.set(
        "className",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue classObj) {
            return LuaValue.valueOf(resolveClass(classObj).getName());
          }
        });
    globals.set(
        "classSimpleName",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue classObj) {
            return LuaValue.valueOf(resolveClass(classObj).getSimpleName());
          }
        });
    globals.set(
        "superClass",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue classObj) {
            Class<?> clazz = resolveClass(classObj);
            Class<?> superClass = clazz.getSuperclass();
            return superClass != null ? CoerceJavaToLua.coerce(superClass) : LuaValue.NIL;
          }
        });
    globals.set(
        "interfaces",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue classObj) {
            Class<?> clazz = resolveClass(classObj);
            LuaTable table = new LuaTable();
            Class<?>[] interfaces = clazz.getInterfaces();
            for (int i = 0; i < interfaces.length; i++) {
              table.set(i + 1, interfaces[i].getName());
            }
            return table;
          }
        });
    globals.set(
        "isAssignableFrom",
        new TwoArgFunction() {
          @Override
          public LuaValue call(LuaValue classObj, LuaValue otherClass) {
            return LuaValue.valueOf(
                resolveClass(classObj).isAssignableFrom(resolveClass(otherClass)));
          }
        });
    globals.set(
        "isInstance",
        new TwoArgFunction() {
          @Override
          public LuaValue call(LuaValue classObj, LuaValue obj) {
            Class<?> clazz = resolveClass(classObj);
            if (!obj.isuserdata()) return LuaValue.FALSE;
            return LuaValue.valueOf(clazz.isInstance(obj.checkuserdata(Object.class)));
          }
        });
    globals.set(
        "cast",
        new TwoArgFunction() {
          @Override
          public LuaValue call(LuaValue classObj, LuaValue obj) {
            Class<?> clazz = resolveClass(classObj);
            if (!obj.isuserdata()) throw new LuaError("cannot cast non-Java object");
            return CoerceJavaToLua.coerce(clazz.cast(obj.checkuserdata(Object.class)));
          }
        });
  }

  private void registerModifierHelpers(Globals globals) {
    globals.set(
        "isPublic",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue mods) {
            return LuaValue.valueOf(Modifier.isPublic(mods.checkint()));
          }
        });
    globals.set(
        "isPrivate",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue mods) {
            return LuaValue.valueOf(Modifier.isPrivate(mods.checkint()));
          }
        });
    globals.set(
        "isStatic",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue mods) {
            return LuaValue.valueOf(Modifier.isStatic(mods.checkint()));
          }
        });
    globals.set(
        "isFinal",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue mods) {
            return LuaValue.valueOf(Modifier.isFinal(mods.checkint()));
          }
        });
    globals.set(
        "isAbstract",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue mods) {
            return LuaValue.valueOf(Modifier.isAbstract(mods.checkint()));
          }
        });
  }

  private Class<?> resolveClass(LuaValue value) {
    if (value.isuserdata()) {
      Object obj = value.checkuserdata(Object.class);
      if (obj instanceof Class) return (Class<?>) obj;
      return obj.getClass();
    }
    if (value.isstring()) {
      try {
        return Class.forName(value.tojstring());

      } catch (ClassNotFoundException e) {
        throw new LuaError(e);
      }
    }
    throw new LuaError("cannot resolve class from: " + value.type());
  }

  private Field findField(Class<?> clazz, String name) {
    Class<?> current = clazz;
    while (current != null) {
      try {
        return current.getDeclaredField(name);

      } catch (NoSuchFieldException ignored) {
        try {
          return current.getField(name);

        } catch (NoSuchFieldException ignored2) {
          current = current.getSuperclass();
        }
      }
    }
    throw new LuaError("field not found: " + name + " in " + clazz.getName());
  }

  private Method findMethod(Class<?> clazz, String name, int argCount) {
    for (Method method : clazz.getMethods()) {
      if (method.getName().equals(name) && method.getParameterCount() == argCount) {
        return method;
      }
    }
    Class<?> current = clazz;
    while (current != null) {
      for (Method method : current.getDeclaredMethods()) {
        if (method.getName().equals(name) && method.getParameterCount() == argCount) {
          return method;
        }
      }
      current = current.getSuperclass();
    }
    throw new LuaError(
        "method not found: " + name + " with " + argCount + " args in " + clazz.getName());
  }

  private Constructor<?> findConstructor(Class<?> clazz, int argCount) {
    for (Constructor<?> ctor : clazz.getConstructors()) {
      if (ctor.getParameterCount() == argCount) return ctor;
    }
    for (Constructor<?> ctor : clazz.getDeclaredConstructors()) {
      if (ctor.getParameterCount() == argCount) return ctor;
    }
    throw new LuaError("constructor with " + argCount + " args not found in " + clazz.getName());
  }
}
