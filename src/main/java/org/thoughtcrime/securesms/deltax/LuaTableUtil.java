package org.thoughtcrime.securesms.deltax;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

/**
 * Converts between Jackson {@link JsonNode} (used for DeltaX plugin configuration files) and Lua
 * tables. Replaces the Bukkit/YAML based configuration handling of MinecraftX.
 */
public final class LuaTableUtil {

  private LuaTableUtil() {}

  public static LuaValue jsonToLua(JsonNode node) {
    if (node == null || node.isNull()) {
      return LuaValue.NIL;
    }
    if (node.isObject()) {
      LuaTable table = new LuaTable();
      node.fields()
          .forEachRemaining(entry -> table.set(entry.getKey(), jsonToLua(entry.getValue())));
      return table;
    }
    if (node.isArray()) {
      LuaTable table = new LuaTable();
      int i = 1;
      for (JsonNode element : node) {
        table.set(i++, jsonToLua(element));
      }
      return table;
    }
    if (node.isTextual()) {
      return LuaValue.valueOf(node.asText());
    }
    if (node.isBoolean()) {
      return LuaValue.valueOf(node.asBoolean());
    }
    if (node.isInt() || node.isLong()) {
      return LuaValue.valueOf(node.asInt());
    }
    if (node.isNumber()) {
      return LuaValue.valueOf(node.asDouble());
    }
    return LuaValue.valueOf(node.asText());
  }

  public static JsonNode luaToJson(LuaValue value) {
    JsonNodeFactory factory = JsonNodeFactory.instance;
    if (value == null || value.isnil()) {
      return factory.nullNode();
    }
    if (value.istable()) {
      LuaTable table = value.checktable();
      if (isArrayLike(table)) {
        ArrayNode array = factory.arrayNode();
        int i = 1;
        while (!table.get(i).isnil()) {
          array.add(luaToJson(table.get(i)));
          i++;
        }
        return array;
      } else {
        ObjectNode object = factory.objectNode();
        LuaValue k = LuaValue.NIL;
        while (true) {
          org.luaj.vm2.Varargs n = table.next(k);
          k = n.arg1();
          if (k.isnil()) break;
          if (k.isstring()) {
            object.set(k.tojstring(), luaToJson(n.arg(2)));
          }
        }
        return object;
      }
    }
    if (value.isboolean()) {
      return factory.booleanNode(value.toboolean());
    }
    if (value.isint()) {
      return factory.numberNode(value.toint());
    }
    if (value.isnumber()) {
      return factory.numberNode(value.todouble());
    }
    return factory.textNode(value.tojstring());
  }

  private static boolean isArrayLike(LuaTable table) {
    if (table.keyCount() == 0) {
      // No integer keys at all -> treat as object
      return false;
    }
    int i = 1;
    while (!table.get(i).isnil()) {
      i++;
    }
    // If the highest contiguous integer key equals the key count, it is array-like
    return (i - 1) == table.keyCount();
  }

  public static List<String> luaTableToStringList(LuaValue value) {
    List<String> result = new ArrayList<>();
    if (value == null || !value.istable()) return result;
    LuaTable table = value.checktable();
    int i = 1;
    while (!table.get(i).isnil()) {
      result.add(table.get(i).tojstring());
      i++;
    }
    return result;
  }
}
