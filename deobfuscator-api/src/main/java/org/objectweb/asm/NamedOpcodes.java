package org.objectweb.asm;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/*
Debug purposes
 */
public final class NamedOpcodes {

  private static final Map<Integer, String> names = new HashMap<>();

  private NamedOpcodes() {
    throw new IllegalArgumentException();
  }

  static {
    try {
      boolean found = false;
      for (Field declaredField : Opcodes.class.getDeclaredFields()) {
        if (declaredField.getName().equals("NOP")) found = true;

        if (!found) continue;

        names.put(declaredField.getInt(null), declaredField.getName());
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static String map(int opcode) {
    return names.getOrDefault(opcode, String.valueOf(opcode));
  }
}
