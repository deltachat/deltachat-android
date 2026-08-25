package org.thoughtcrime.securesms.deltax.bridge;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

public class AnnotationBridge {
  public void register(Globals globals) {
    globals.set(
        "getAnnotations",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue obj) {
            AnnotatedElement element = resolveAnnotatedElement(obj);
            LuaTable table = new LuaTable();
            int idx = 1;
            for (Annotation ann : element.getAnnotations()) {
              table.set(idx, annotationToJavaTable(ann));
              idx++;
            }
            return table;
          }
        });
    globals.set(
        "getAnnotation",
        new TwoArgFunction() {
          @Override
          public LuaValue call(LuaValue obj, LuaValue annClass) {
            AnnotatedElement element = resolveAnnotatedElement(obj);
            try {
              Class<? extends Annotation> annType =
                  (Class<? extends Annotation>) Class.forName(annClass.tojstring());
              Annotation annotation = element.getAnnotation(annType);
              return annotation != null ? annotationToJavaTable(annotation) : LuaValue.NIL;

            } catch (Exception e) {
              return LuaValue.NIL;
            }
          }
        });
    globals.set(
        "hasAnnotation",
        new TwoArgFunction() {
          @Override
          public LuaValue call(LuaValue obj, LuaValue annClass) {
            try {
              AnnotatedElement element = resolveAnnotatedElement(obj);
              Class<? extends Annotation> annType =
                  (Class<? extends Annotation>) Class.forName(annClass.tojstring());
              return LuaValue.valueOf(element.isAnnotationPresent(annType));

            } catch (Exception e) {
              return LuaValue.FALSE;
            }
          }
        });
    globals.set(
        "getAnnotationsByType",
        new TwoArgFunction() {
          @Override
          public LuaValue call(LuaValue obj, LuaValue annClass) {
            AnnotatedElement element = resolveAnnotatedElement(obj);
            try {
              Class<? extends Annotation> annType =
                  (Class<? extends Annotation>) Class.forName(annClass.tojstring());
              Annotation[] annotations = element.getAnnotationsByType(annType);
              LuaTable table = new LuaTable();
              int idx = 1;
              for (Annotation ann : annotations) {
                table.set(idx, annotationToJavaTable(ann));
                idx++;
              }
              return table;

            } catch (Exception e) {
              return LuaValue.NIL;
            }
          }
        });
    globals.set(
        "getDeclaredAnnotations",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue obj) {
            AnnotatedElement element = resolveAnnotatedElement(obj);
            LuaTable table = new LuaTable();
            int idx = 1;
            for (Annotation ann : element.getDeclaredAnnotations()) {
              table.set(idx, annotationToJavaTable(ann));
              idx++;
            }
            return table;
          }
        });
    globals.set(
        "getMethodAnnotations",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue methodObj) {
            Method method = (Method) methodObj.checkuserdata(Method.class);
            LuaTable table = new LuaTable();
            int idx = 1;
            for (Annotation ann : method.getDeclaredAnnotations()) {
              table.set(idx, annotationToJavaTable(ann));
              idx++;
            }
            return table;
          }
        });
    globals.set(
        "getFieldAnnotations",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue fieldObj) {
            Field field = (Field) fieldObj.checkuserdata(Field.class);
            LuaTable table = new LuaTable();
            int idx = 1;
            for (Annotation ann : field.getDeclaredAnnotations()) {
              table.set(idx, annotationToJavaTable(ann));
              idx++;
            }
            return table;
          }
        });
    globals.set(
        "getParameterAnnotations",
        new TwoArgFunction() {
          @Override
          public LuaValue call(LuaValue methodObj, LuaValue paramIndex) {
            Method method = (Method) methodObj.checkuserdata(Method.class);
            int index = paramIndex.checkint();
            java.lang.annotation.Annotation[][] allParams = method.getParameterAnnotations();
            if (index < 0 || index >= allParams.length) return new LuaTable();
            LuaTable table = new LuaTable();
            int idx = 1;
            for (Annotation ann : allParams[index]) {
              table.set(idx, annotationToJavaTable(ann));
              idx++;
            }
            return table;
          }
        });
  }

  private AnnotatedElement resolveAnnotatedElement(LuaValue obj) {
    if (obj.isuserdata()) {
      Object userObj = obj.checkuserdata(Object.class);
      if (userObj instanceof Class) return (Class<?>) userObj;
      if (userObj instanceof Method) return (Method) userObj;
      if (userObj instanceof Field) return (Field) userObj;
      if (userObj instanceof java.lang.reflect.Constructor)
        return (java.lang.reflect.Constructor<?>) userObj;
      if (userObj instanceof Parameter) return (Parameter) userObj;
      return userObj.getClass();
    }
    if (obj.isstring()) {
      try {
        return Class.forName(obj.tojstring());

      } catch (ClassNotFoundException e) {
        throw new LuaError(e);
      }
    }
    throw new LuaError("cannot resolve annotated element from: " + obj.type());
  }

  public static LuaValue annotationToJavaTable(Annotation annotation) {
    LuaTable table = new LuaTable();
    Class<? extends Annotation> annType = annotation.annotationType();
    table.set("annotationType", annType.getName());
    for (Method method : annType.getDeclaredMethods()) {
      if (method.getParameterCount() == 0) {
        try {
          method.setAccessible(true);
          Object value = method.invoke(annotation);
          table.set(method.getName(), CoerceJavaToLua.coerce(value));

        } catch (Exception ignored) {

        }
      }
    }
    return table;
  }
}
