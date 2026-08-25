package org.thoughtcrime.securesms.deltax.bridge;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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

public class CollectionBridge {
  public void register(Globals globals) {
    registerListHelpers(globals);
    registerMapHelpers(globals);
    registerSetHelpers(globals);
    registerOptionalHelpers(globals);
    registerStreamHelpers(globals);
    registerConversionHelpers(globals);
  }

  private void registerListHelpers(Globals globals) {
    globals.set(
        "newArrayList",
        new VarArgFunction() {
          @Override
          public Varargs invoke(Varargs args) {
            List<Object> list = new ArrayList<>();
            for (int i = 1; i <= args.narg(); i++) {
              list.add(CoerceLuaToJava.coerce(args.arg(i), Object.class));
            }
            return CoerceJavaToLua.coerce(list);
          }
        });
    globals.set(
        "newLinkedList",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue arg) {
            return CoerceJavaToLua.coerce(new LinkedList<>());
          }
        });
    globals.set(
        "listSize",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue listObj) {
            List<?> list = (List) listObj.checkuserdata(List.class);
            return LuaValue.valueOf(list.size());
          }
        });
    globals.set(
        "listGet",
        new TwoArgFunction() {
          @Override
          public LuaValue call(LuaValue listObj, LuaValue index) {
            List<?> list = (List) listObj.checkuserdata(List.class);
            return CoerceJavaToLua.coerce(list.get(index.checkint() - 1));
          }
        });
    globals.set(
        "listSet",
        new ThreeArgFunction() {
          @Override
          public LuaValue call(LuaValue listObj, LuaValue index, LuaValue value) {
            List<Object> list = (List) listObj.checkuserdata(List.class);
            list.set(index.checkint() - 1, CoerceLuaToJava.coerce(value, Object.class));
            return LuaValue.NIL;
          }
        });
    globals.set(
        "listAdd",
        new VarArgFunction() {
          @Override
          public Varargs invoke(Varargs args) {
            List<Object> list = (List) args.checkuserdata(1, List.class);
            for (int i = 2; i <= args.narg(); i++) {
              list.add(CoerceLuaToJava.coerce(args.arg(i), Object.class));
            }
            return LuaValue.valueOf(true);
          }
        });
    globals.set(
        "listRemove",
        new TwoArgFunction() {
          @Override
          public LuaValue call(LuaValue listObj, LuaValue value) {
            List<Object> list = (List) listObj.checkuserdata(List.class);
            return LuaValue.valueOf(list.remove(CoerceLuaToJava.coerce(value, Object.class)));
          }
        });
    globals.set(
        "listRemoveAt",
        new TwoArgFunction() {
          @Override
          public LuaValue call(LuaValue listObj, LuaValue index) {
            List<Object> list = (List) listObj.checkuserdata(List.class);
            return CoerceJavaToLua.coerce(list.remove(index.checkint() - 1));
          }
        });
    globals.set(
        "listClear",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue listObj) {
            List<?> list = (List) listObj.checkuserdata(List.class);
            list.clear();
            return LuaValue.NIL;
          }
        });
    globals.set(
        "listContains",
        new TwoArgFunction() {
          @Override
          public LuaValue call(LuaValue listObj, LuaValue value) {
            List<?> list = (List) listObj.checkuserdata(List.class);
            return LuaValue.valueOf(list.contains(CoerceLuaToJava.coerce(value, Object.class)));
          }
        });
    globals.set(
        "listIsEmpty",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue listObj) {
            List<?> list = (List) listObj.checkuserdata(List.class);
            return LuaValue.valueOf(list.isEmpty());
          }
        });
    globals.set(
        "listSort",
        new VarArgFunction() {
          @Override
          public Varargs invoke(Varargs args) {
            List<Object> list = (List) args.checkuserdata(1, List.class);
            if (args.narg() >= 2) {
              Comparator<Object> comparator = (Comparator) args.checkuserdata(2, Comparator.class);
              list.sort(comparator);

            } else {
              ((List) list).sort(Comparator.naturalOrder());
            }
            return LuaValue.NIL;
          }
        });
    globals.set(
        "listToTable",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue listObj) {
            List<?> list = (List) listObj.checkuserdata(List.class);
            LuaTable table = new LuaTable();
            int i = 1;
            for (Object item : list) {
              table.set(i++, CoerceJavaToLua.coerce(item));
            }
            return table;
          }
        });
  }

  private void registerMapHelpers(Globals globals) {
    globals.set(
        "newHashMap",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue arg) {
            return CoerceJavaToLua.coerce(new HashMap<String, Object>());
          }
        });
    globals.set(
        "newLinkedHashMap",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue arg) {
            return CoerceJavaToLua.coerce(new LinkedHashMap<String, Object>());
          }
        });
    globals.set(
        "newTreeMap",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue arg) {
            return CoerceJavaToLua.coerce(new TreeMap<String, Object>());
          }
        });
    globals.set(
        "mapPut",
        new ThreeArgFunction() {
          @Override
          public LuaValue call(LuaValue mapObj, LuaValue key, LuaValue value) {
            Map<Object, Object> map = (Map) mapObj.checkuserdata(Map.class);
            map.put(
                CoerceLuaToJava.coerce(key, Object.class),
                CoerceLuaToJava.coerce(value, Object.class));
            return LuaValue.NIL;
          }
        });
    globals.set(
        "mapGet",
        new TwoArgFunction() {
          @Override
          public LuaValue call(LuaValue mapObj, LuaValue key) {
            Map<?, ?> map = (Map) mapObj.checkuserdata(Map.class);
            return CoerceJavaToLua.coerce(map.get(CoerceLuaToJava.coerce(key, Object.class)));
          }
        });
    globals.set(
        "mapRemove",
        new TwoArgFunction() {
          @Override
          public LuaValue call(LuaValue mapObj, LuaValue key) {
            Map<Object, Object> map = (Map) mapObj.checkuserdata(Map.class);
            return CoerceJavaToLua.coerce(map.remove(CoerceLuaToJava.coerce(key, Object.class)));
          }
        });
    globals.set(
        "mapContainsKey",
        new TwoArgFunction() {
          @Override
          public LuaValue call(LuaValue mapObj, LuaValue key) {
            Map<?, ?> map = (Map) mapObj.checkuserdata(Map.class);
            return LuaValue.valueOf(map.containsKey(CoerceLuaToJava.coerce(key, Object.class)));
          }
        });
    globals.set(
        "mapContainsValue",
        new TwoArgFunction() {
          @Override
          public LuaValue call(LuaValue mapObj, LuaValue value) {
            Map<?, ?> map = (Map) mapObj.checkuserdata(Map.class);
            return LuaValue.valueOf(map.containsValue(CoerceLuaToJava.coerce(value, Object.class)));
          }
        });
    globals.set(
        "mapKeys",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue mapObj) {
            Map<?, ?> map = (Map) mapObj.checkuserdata(Map.class);
            LuaTable table = new LuaTable();
            int idx = 1;
            for (Object key : map.keySet()) {
              table.set(idx++, CoerceJavaToLua.coerce(key));
            }
            return table;
          }
        });
    globals.set(
        "mapValues",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue mapObj) {
            Map<?, ?> map = (Map) mapObj.checkuserdata(Map.class);
            LuaTable table = new LuaTable();
            int idx = 1;
            for (Object value : map.values()) {
              table.set(idx++, CoerceJavaToLua.coerce(value));
            }
            return table;
          }
        });
    globals.set(
        "mapSize",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue mapObj) {
            Map<?, ?> map = (Map) mapObj.checkuserdata(Map.class);
            return LuaValue.valueOf(map.size());
          }
        });
    globals.set(
        "mapIsEmpty",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue mapObj) {
            Map<?, ?> map = (Map) mapObj.checkuserdata(Map.class);
            return LuaValue.valueOf(map.isEmpty());
          }
        });
    globals.set(
        "mapClear",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue mapObj) {
            Map<?, ?> map = (Map) mapObj.checkuserdata(Map.class);
            map.clear();
            return LuaValue.NIL;
          }
        });
    globals.set(
        "mapToTable",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue mapObj) {
            Map<?, ?> map = (Map) mapObj.checkuserdata(Map.class);
            return javaMapToLuaTable(map);
          }
        });
  }

  private void registerSetHelpers(Globals globals) {
    globals.set(
        "newHashSet",
        new VarArgFunction() {
          @Override
          public Varargs invoke(Varargs args) {
            Set<Object> set = new HashSet<>();
            for (int i = 1; i <= args.narg(); i++) {
              set.add(CoerceLuaToJava.coerce(args.arg(i), Object.class));
            }
            return CoerceJavaToLua.coerce(set);
          }
        });
    globals.set(
        "setAdd",
        new TwoArgFunction() {
          @Override
          public LuaValue call(LuaValue setObj, LuaValue value) {
            Set<Object> set = (Set) setObj.checkuserdata(Set.class);
            return LuaValue.valueOf(set.add(CoerceLuaToJava.coerce(value, Object.class)));
          }
        });
    globals.set(
        "setRemove",
        new TwoArgFunction() {
          @Override
          public LuaValue call(LuaValue setObj, LuaValue value) {
            Set<Object> set = (Set) setObj.checkuserdata(Set.class);
            return LuaValue.valueOf(set.remove(CoerceLuaToJava.coerce(value, Object.class)));
          }
        });
    globals.set(
        "setContains",
        new TwoArgFunction() {
          @Override
          public LuaValue call(LuaValue setObj, LuaValue value) {
            Set<?> set = (Set) setObj.checkuserdata(Set.class);
            return LuaValue.valueOf(set.contains(CoerceLuaToJava.coerce(value, Object.class)));
          }
        });
    globals.set(
        "setSize",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue setObj) {
            Set<?> set = (Set) setObj.checkuserdata(Set.class);
            return LuaValue.valueOf(set.size());
          }
        });
    globals.set(
        "setToTable",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue setObj) {
            Set<?> set = (Set) setObj.checkuserdata(Set.class);
            LuaTable table = new LuaTable();
            int idx = 1;
            for (Object item : set) {
              table.set(idx++, CoerceJavaToLua.coerce(item));
            }
            return table;
          }
        });
    globals.set(
        "setClear",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue setObj) {
            Set<?> set = (Set) setObj.checkuserdata(Set.class);
            set.clear();
            return LuaValue.NIL;
          }
        });
  }

  private void registerOptionalHelpers(Globals globals) {
    globals.set(
        "optionalOf",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue value) {
            return CoerceJavaToLua.coerce(
                Optional.ofNullable(CoerceLuaToJava.coerce(value, Object.class)));
          }
        });
    globals.set(
        "optionalEmpty",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue arg) {
            return CoerceJavaToLua.coerce(Optional.empty());
          }
        });
    globals.set(
        "optionalIsPresent",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue optObj) {
            Optional<?> opt = (Optional) optObj.checkuserdata(Optional.class);
            return LuaValue.valueOf(opt.isPresent());
          }
        });
    globals.set(
        "optionalGet",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue optObj) {
            Optional<?> opt = (Optional) optObj.checkuserdata(Optional.class);
            return CoerceJavaToLua.coerce(opt.orElse(null));
          }
        });
    globals.set(
        "optionalOrElse",
        new TwoArgFunction() {
          @Override
          public LuaValue call(LuaValue optObj, LuaValue defaultVal) {
            Optional<Object> opt = (Optional) optObj.checkuserdata(Optional.class);
            return CoerceJavaToLua.coerce(
                opt.orElse(CoerceLuaToJava.coerce(defaultVal, Object.class)));
          }
        });
  }

  private void registerStreamHelpers(Globals globals) {
    globals.set(
        "streamToList",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue streamObj) {
            Stream<?> stream = (Stream) streamObj.checkuserdata(Stream.class);
            return CoerceJavaToLua.coerce(stream.collect(Collectors.toList()));
          }
        });
    globals.set(
        "streamToSet",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue streamObj) {
            Stream<?> stream = (Stream) streamObj.checkuserdata(Stream.class);
            return CoerceJavaToLua.coerce(stream.collect(Collectors.toSet()));
          }
        });
    globals.set(
        "streamMap",
        new TwoArgFunction() {
          @Override
          public LuaValue call(LuaValue streamObj, LuaValue func) {
            Stream<Object> stream = (Stream) streamObj.checkuserdata(Stream.class);
            org.luaj.vm2.LuaFunction luaFunc = func.checkfunction();
            Stream<Object> mapped =
                stream.map(
                    item ->
                        CoerceLuaToJava.coerce(
                            luaFunc.call(CoerceJavaToLua.coerce(item)), Object.class));
            return CoerceJavaToLua.coerce(mapped);
          }
        });
    globals.set(
        "streamFilter",
        new TwoArgFunction() {
          @Override
          public LuaValue call(LuaValue streamObj, LuaValue func) {
            Stream<Object> stream = (Stream) streamObj.checkuserdata(Stream.class);
            org.luaj.vm2.LuaFunction luaFunc = func.checkfunction();
            Stream<Object> filtered =
                stream.filter(item -> luaFunc.call(CoerceJavaToLua.coerce(item)).toboolean());
            return CoerceJavaToLua.coerce(filtered);
          }
        });
    globals.set(
        "streamForEach",
        new TwoArgFunction() {
          @Override
          public LuaValue call(LuaValue streamObj, LuaValue func) {
            Stream<Object> stream = (Stream) streamObj.checkuserdata(Stream.class);
            org.luaj.vm2.LuaFunction luaFunc = func.checkfunction();
            stream.forEach(item -> luaFunc.call(CoerceJavaToLua.coerce(item)));
            return LuaValue.NIL;
          }
        });
    globals.set(
        "streamCount",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue streamObj) {
            Stream<?> stream = (Stream) streamObj.checkuserdata(Stream.class);
            return LuaValue.valueOf(stream.count());
          }
        });
    globals.set(
        "streamCollect",
        new TwoArgFunction() {
          @Override
          public LuaValue call(LuaValue streamObj, LuaValue collectorObj) {
            Stream<Object> stream = (Stream) streamObj.checkuserdata(Stream.class);
            @SuppressWarnings("rawtypes")
            java.util.stream.Collector collector =
                (java.util.stream.Collector)
                    collectorObj.checkuserdata(java.util.stream.Collector.class);
            return CoerceJavaToLua.coerce(stream.collect(collector));
          }
        });
    globals.set(
        "streamDistinct",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue streamObj) {
            Stream<Object> stream = (Stream) streamObj.checkuserdata(Stream.class);
            return CoerceJavaToLua.coerce(stream.distinct());
          }
        });
    globals.set(
        "streamSorted",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue streamObj) {
            Stream<Object> stream = (Stream) streamObj.checkuserdata(Stream.class);
            return CoerceJavaToLua.coerce(stream.sorted());
          }
        });
    globals.set(
        "streamLimit",
        new TwoArgFunction() {
          @Override
          public LuaValue call(LuaValue streamObj, LuaValue max) {
            Stream<Object> stream = (Stream) streamObj.checkuserdata(Stream.class);
            return CoerceJavaToLua.coerce(stream.limit(max.tolong()));
          }
        });
    globals.set(
        "streamSkip",
        new TwoArgFunction() {
          @Override
          public LuaValue call(LuaValue streamObj, LuaValue n) {
            Stream<Object> stream = (Stream) streamObj.checkuserdata(Stream.class);
            return CoerceJavaToLua.coerce(stream.skip(n.tolong()));
          }
        });
    globals.set(
        "streamFindFirst",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue streamObj) {
            Stream<Object> stream = (Stream) streamObj.checkuserdata(Stream.class);
            return CoerceJavaToLua.coerce(stream.findFirst().orElse(null));
          }
        });
    globals.set(
        "streamAnyMatch",
        new TwoArgFunction() {
          @Override
          public LuaValue call(LuaValue streamObj, LuaValue func) {
            Stream<Object> stream = (Stream) streamObj.checkuserdata(Stream.class);
            org.luaj.vm2.LuaFunction luaFunc = func.checkfunction();
            return LuaValue.valueOf(
                stream.anyMatch(item -> luaFunc.call(CoerceJavaToLua.coerce(item)).toboolean()));
          }
        });
    globals.set(
        "streamAllMatch",
        new TwoArgFunction() {
          @Override
          public LuaValue call(LuaValue streamObj, LuaValue func) {
            Stream<Object> stream = (Stream) streamObj.checkuserdata(Stream.class);
            org.luaj.vm2.LuaFunction luaFunc = func.checkfunction();
            return LuaValue.valueOf(
                stream.allMatch(item -> luaFunc.call(CoerceJavaToLua.coerce(item)).toboolean()));
          }
        });
    globals.set(
        "streamNoneMatch",
        new TwoArgFunction() {
          @Override
          public LuaValue call(LuaValue streamObj, LuaValue func) {
            Stream<Object> stream = (Stream) streamObj.checkuserdata(Stream.class);
            org.luaj.vm2.LuaFunction luaFunc = func.checkfunction();
            return LuaValue.valueOf(
                stream.noneMatch(item -> luaFunc.call(CoerceJavaToLua.coerce(item)).toboolean()));
          }
        });
    globals.set(
        "collectorsToList",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue arg) {
            return CoerceJavaToLua.coerce(Collectors.toList());
          }
        });
    globals.set(
        "collectorsToSet",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue arg) {
            return CoerceJavaToLua.coerce(Collectors.toSet());
          }
        });
    globals.set(
        "collectorsJoining",
        new VarArgFunction() {
          @Override
          public Varargs invoke(Varargs args) {
            String delimiter = args.narg() >= 1 ? args.checkjstring(1) : "";
            String prefix = args.narg() >= 2 ? args.checkjstring(2) : "";
            String suffix = args.narg() >= 3 ? args.checkjstring(3) : "";
            return CoerceJavaToLua.coerce(Collectors.joining(delimiter, prefix, suffix));
          }
        });
    globals.set(
        "collectorsToMap",
        new TwoArgFunction() {
          @Override
          public LuaValue call(LuaValue keyMapper, LuaValue valueMapper) {
            org.luaj.vm2.LuaFunction keyFunc = keyMapper.checkfunction();
            org.luaj.vm2.LuaFunction valFunc = valueMapper.checkfunction();
            @SuppressWarnings("rawtypes")
            java.util.stream.Collector collector =
                Collectors.toMap(
                    k ->
                        CoerceLuaToJava.coerce(
                            keyFunc.call(CoerceJavaToLua.coerce(k)), Object.class),
                    v ->
                        CoerceLuaToJava.coerce(
                            valFunc.call(CoerceJavaToLua.coerce(v)), Object.class));
            return CoerceJavaToLua.coerce(collector);
          }
        });
    globals.set(
        "collectorsGroupingBy",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue classifier) {
            org.luaj.vm2.LuaFunction func = classifier.checkfunction();
            @SuppressWarnings("rawtypes")
            java.util.stream.Collector collector =
                Collectors.groupingBy(
                    k ->
                        CoerceLuaToJava.coerce(func.call(CoerceJavaToLua.coerce(k)), Object.class));
            return CoerceJavaToLua.coerce(collector);
          }
        });
  }

  private void registerConversionHelpers(Globals globals) {
    globals.set(
        "asList",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue tableObj) {
            LuaTable table = tableObj.checktable();
            List<Object> list = new ArrayList<>();
            int i = 1;
            while (true) {
              LuaValue value = table.get(i);
              if (value.isnil()) break;
              list.add(CoerceLuaToJava.coerce(value, Object.class));
              i++;
            }
            if (list.isEmpty()) {
              LuaValue k = LuaValue.NIL;
              while (true) {
                Varargs n = table.next(k);
                k = n.arg1();
                if (k.isnil()) break;
                if (!k.isint()) {
                  list.add(CoerceLuaToJava.coerce(k, Object.class));
                }
              }
            }
            return CoerceJavaToLua.coerce(list);
          }
        });
    globals.set(
        "asMap",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue tableObj) {
            LuaTable table = tableObj.checktable();
            Map<Object, Object> map = new LinkedHashMap<>();
            LuaValue k = LuaValue.NIL;
            while (true) {
              Varargs n = table.next(k);
              k = n.arg1();
              if (k.isnil()) break;
              if (k.isstring()) {
                map.put(k.tojstring(), CoerceLuaToJava.coerce(n.arg(2), Object.class));
              }
            }
            return CoerceJavaToLua.coerce(map);
          }
        });
    globals.set(
        "asSet",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue tableObj) {
            LuaTable table = tableObj.checktable();
            Set<Object> set = new LinkedHashSet<>();
            int i = 1;
            while (true) {
              LuaValue value = table.get(i);
              if (value.isnil()) break;
              set.add(CoerceLuaToJava.coerce(value, Object.class));
              i++;
            }
            return CoerceJavaToLua.coerce(set);
          }
        });
    globals.set(
        "toTable",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue collectionObj) {
            Object obj = collectionObj.checkuserdata(Object.class);
            if (obj instanceof Map) {
              return javaMapToLuaTable((Map<?, ?>) obj);

            } else if (obj instanceof Collection) {
              LuaTable table = new LuaTable();
              int idx = 1;
              for (Object item : (Collection<?>) obj) {
                table.set(idx++, CoerceJavaToLua.coerce(item));
              }
              return table;

            } else if (obj.getClass().isArray()) {
              LuaTable table = new LuaTable();
              int len = java.lang.reflect.Array.getLength(obj);
              for (int i = 0; i < len; i++) {
                table.set(i + 1, CoerceJavaToLua.coerce(java.lang.reflect.Array.get(obj, i)));
              }
              return table;

            } else {
              LuaTable table = new LuaTable();
              table.set(1, CoerceJavaToLua.coerce(obj));
              return table;
            }
          }
        });
    globals.set(
        "toArray",
        new TwoArgFunction() {
          @Override
          public LuaValue call(LuaValue tableObj, LuaValue componentType) {
            LuaTable table = tableObj.checktable();
            try {
              Class<?> clazz = Class.forName(componentType.tojstring());
              List<Object> values = new ArrayList<>();
              int i = 1;
              while (true) {
                LuaValue value = table.get(i);
                if (value.isnil()) break;
                values.add(CoerceLuaToJava.coerce(value, clazz));
                i++;
              }
              Object arr = java.lang.reflect.Array.newInstance(clazz, values.size());
              for (int idx = 0; idx < values.size(); idx++) {
                java.lang.reflect.Array.set(arr, idx, values.get(idx));
              }
              return CoerceJavaToLua.coerce(arr);

            } catch (Exception e) {
              throw new LuaError(e);
            }
          }
        });
    globals.set(
        "iteratorToTable",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue iterObj) {
            Iterator<?> iter = (Iterator) iterObj.checkuserdata(Iterator.class);
            LuaTable table = new LuaTable();
            int idx = 1;
            while (iter.hasNext()) {
              table.set(idx++, CoerceJavaToLua.coerce(iter.next()));
            }
            return table;
          }
        });
    globals.set(
        "enumerationToTable",
        new OneArgFunction() {
          @Override
          public LuaValue call(LuaValue enumObj) {
            Enumeration<?> enumeration = (Enumeration) enumObj.checkuserdata(Enumeration.class);
            LuaTable table = new LuaTable();
            int idx = 1;
            while (enumeration.hasMoreElements()) {
              table.set(idx++, CoerceJavaToLua.coerce(enumeration.nextElement()));
            }
            return table;
          }
        });
  }

  public static LuaTable javaMapToLuaTable(Map<?, ?> map) {
    LuaTable table = new LuaTable();
    for (Map.Entry<?, ?> entry : map.entrySet()) {
      LuaValue luaKey =
          entry.getKey() instanceof String
              ? LuaValue.valueOf((String) entry.getKey())
              : CoerceJavaToLua.coerce(entry.getKey());
      table.set(luaKey, CoerceJavaToLua.coerce(entry.getValue()));
    }
    return table;
  }
}
