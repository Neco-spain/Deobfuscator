package uwu.narumi.deobfuscator.api.helper;

import static org.objectweb.asm.Opcodes.*;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.OriginalSourceValue;
import uwu.narumi.deobfuscator.api.asm.matcher.Match;
import uwu.narumi.deobfuscator.api.asm.matcher.impl.MethodMatch;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

public final class AsmMathHelper {
  private static final Map<Integer, Predicate<Integer>> ONE_VALUE_CONDITION_PREDICATES = Map.of(
      IFEQ, value -> value == 0,
      IFNE, value -> value != 0,
      IFLT, value -> value < 0,
      IFGE, value -> value >= 0,
      IFGT, value -> value > 0,
      IFLE, value -> value <= 0
  );

  private static final Map<Integer, BiPredicate<Integer, Integer>> TWO_VALUES_VALUE_CONDITION_PREDICATES = Map.of(
      IF_ICMPEQ, Objects::equals,
      IF_ICMPNE, (first, second) -> !Objects.equals(first, second),
      IF_ICMPLT, (first, second) -> first < second,
      IF_ICMPGE, (first, second) -> first >= second,
      IF_ICMPGT, (first, second) -> first > second,
      IF_ICMPLE, (first, second) -> first <= second
  );

  // Binary operations means that instructions takes two values from stack
  private static final Map<Integer, BiFunction<Number, Number, Number>> MATH_BINARY_OPERATIONS = Map.ofEntries(
      /// Math operations ///

      // Integer
      Map.entry(IADD, (first, second) -> first.intValue() + second.intValue()),
      Map.entry(ISUB, (first, second) -> first.intValue() - second.intValue()),
      Map.entry(IMUL, (first, second) -> first.intValue() * second.intValue()),
      Map.entry(IDIV, (first, second) -> first.intValue() / second.intValue()),
      Map.entry(IREM, (first, second) -> first.intValue() % second.intValue()),
      Map.entry(IXOR, (first, second) -> first.intValue() ^ second.intValue()),
      Map.entry(IAND, (first, second) -> first.intValue() & second.intValue()),
      Map.entry(IOR, (first, second) -> first.intValue() | second.intValue()),
      Map.entry(ISHL, (first, second) -> first.intValue() << second.intValue()),
      Map.entry(ISHR, (first, second) -> first.intValue() >> second.intValue()),
      Map.entry(IUSHR, (first, second) -> first.intValue() >>> second.intValue()),
      // Long
      Map.entry(LADD, (first, second) -> first.longValue() + second.longValue()),
      Map.entry(LSUB, (first, second) -> first.longValue() - second.longValue()),
      Map.entry(LMUL, (first, second) -> first.longValue() * second.longValue()),
      Map.entry(LDIV, (first, second) -> first.longValue() / second.longValue()),
      Map.entry(LREM, (first, second) -> first.longValue() % second.longValue()),
      Map.entry(LXOR, (first, second) -> first.longValue() ^ second.longValue()),
      Map.entry(LAND, (first, second) -> first.longValue() & second.longValue()),
      Map.entry(LOR, (first, second) -> first.longValue() | second.longValue()),
      Map.entry(LSHL, (first, second) -> first.longValue() << second.longValue()),
      Map.entry(LSHR, (first, second) -> first.longValue() >> second.longValue()),
      Map.entry(LUSHR, (first, second) -> first.longValue() >>> second.longValue()),
      // Float
      Map.entry(FADD, (first, second) -> first.floatValue() + second.floatValue()),
      Map.entry(FSUB, (first, second) -> first.floatValue() - second.floatValue()),
      Map.entry(FMUL, (first, second) -> first.floatValue() * second.floatValue()),
      Map.entry(FDIV, (first, second) -> first.floatValue() / second.floatValue()),
      Map.entry(FREM, (first, second) -> first.floatValue() % second.floatValue()),
      // Double
      Map.entry(DADD, (first, second) -> first.doubleValue() + second.doubleValue()),
      Map.entry(DSUB, (first, second) -> first.doubleValue() - second.doubleValue()),
      Map.entry(DMUL, (first, second) -> first.doubleValue() * second.doubleValue()),
      Map.entry(DDIV, (first, second) -> first.doubleValue() / second.doubleValue()),
      Map.entry(DREM, (first, second) -> first.doubleValue() % second.doubleValue()),

      /// Compare operations ///
      Map.entry(LCMP, (first, second) -> Long.compare(first.longValue(), second.longValue())),
      Map.entry(FCMPL, (first, second) -> Float.isNaN(first.floatValue()) || Float.isNaN(second.floatValue()) ? -1 : Float.compare(first.floatValue(), second.floatValue())),
      Map.entry(FCMPG, (first, second) -> Float.isNaN(first.floatValue()) || Float.isNaN(second.floatValue()) ? 1 : Float.compare(first.floatValue(), second.floatValue())),
      Map.entry(DCMPL, (first, second) -> Double.isNaN(first.doubleValue()) || Double.isNaN(second.doubleValue()) ? -1 : Double.compare(first.doubleValue(), second.doubleValue())),
      Map.entry(DCMPG, (first, second) -> Double.isNaN(first.doubleValue()) || Double.isNaN(second.doubleValue()) ? 1 : Double.compare(first.doubleValue(), second.doubleValue()))
  );

  // Unary operations means that instructions takes one value from stack
  private static final Map<Integer, Function<Number, Number>> MATH_UNARY_OPERATIONS = Map.ofEntries(
      // Integer
      Map.entry(INEG, number -> -number.intValue()),
      Map.entry(I2B, Number::byteValue),
      Map.entry(I2C, Number::shortValue),
      Map.entry(I2D, Number::doubleValue),
      Map.entry(I2F, Number::floatValue),
      Map.entry(I2L, Number::longValue),
      Map.entry(I2S, Number::shortValue),
      // Long
      Map.entry(LNEG, number -> -number.longValue()),
      Map.entry(L2D, Number::doubleValue),
      Map.entry(L2F, Number::floatValue),
      Map.entry(L2I, Number::intValue),
      // Float
      Map.entry(FNEG, number -> -number.floatValue()),
      Map.entry(F2D, Number::doubleValue),
      Map.entry(F2I, Number::intValue),
      Map.entry(F2L, Number::longValue),
      // Double
      Map.entry(DNEG, number -> -number.doubleValue()),
      Map.entry(D2F, Number::floatValue),
      Map.entry(D2I, Number::intValue),
      Map.entry(D2L, Number::longValue)
  );

  public static final Match STRING_LENGTH =
      MethodMatch.invokeVirtual()
          .owner("java/lang/String")
          .name("length")
          .desc("()I")
          .defineTransformation(context -> {
            // Get value from stack
            OriginalSourceValue sourceValue = context.frame().getStack(context.frame().getStackSize() - 1);
            OriginalSourceValue originalSourceValue = sourceValue.originalSource;
            if (!originalSourceValue.isOneWayProduced()) return false;

            AbstractInsnNode originalInsn = originalSourceValue.getProducer();
            if (!originalInsn.isString()) return false;

            context.pop(1);
            context.methodNode().instructions.set(
                context.insn(),
                AsmHelper.getNumber(originalInsn.asString().length())
            );
            return true;
          });

  public static final Match STRING_HASHCODE =
      MethodMatch.invokeVirtual()
          .owner("java/lang/String")
          .name("hashCode")
          .desc("()I")
          .defineTransformation(context -> {
            // Get value from stack
            OriginalSourceValue sourceValue = context.frame().getStack(context.frame().getStackSize() - 1);
            OriginalSourceValue originalSourceValue = sourceValue.originalSource;
            if (!originalSourceValue.isOneWayProduced()) return false;

            AbstractInsnNode originalInsn = originalSourceValue.getProducer();
            if (!originalInsn.isString()) return false;

            context.pop(1);
            context.methodNode().instructions.set(
                context.insn(),
                AsmHelper.getNumber(originalInsn.asString().hashCode())
            );
            return true;
          });

  public static final Match STRING_TO_INTEGER =
      MethodMatch.invokeStatic()
          .owner("java/lang/Integer")
          .name("parseInt", "valueOf")
          .desc("(Ljava/lang/String;)I")
          .defineTransformation(context -> {
            // Get value from stack
            OriginalSourceValue sourceValue = context.frame().getStack(context.frame().getStackSize() - 1);
            OriginalSourceValue originalSourceValue = sourceValue.originalSource;
            if (!originalSourceValue.isOneWayProduced()) return false;

            AbstractInsnNode originalInsn = originalSourceValue.getProducer();
            // Integer#parseInt(String)
            if (!originalInsn.isString()) return false;

            context.pop(1);
            context.methodNode().instructions.set(
                context.insn(),
                AsmHelper.getNumber(Integer.parseInt(originalInsn.asString()))
            );
            return true;
          });

  public static final Match STRING_TO_INTEGER_RADIX =
      MethodMatch.invokeStatic()
          .owner("java/lang/Integer")
          .name("parseInt", "valueOf")
          .desc("(Ljava/lang/String;I)I")
          .defineTransformation(context -> {
            // Get values from stack
            OriginalSourceValue firstValue = context.frame().getStack(context.frame().getStackSize() - 2);
            OriginalSourceValue originalFirstValue = firstValue.originalSource;
            OriginalSourceValue secondValue = context.frame().getStack(context.frame().getStackSize() - 1);
            OriginalSourceValue originalSecondValue = secondValue.originalSource;
            if (!originalFirstValue.isOneWayProduced() || !originalSecondValue.isOneWayProduced()) return false;

            AbstractInsnNode originalFirstInsn = originalFirstValue.getProducer();
            AbstractInsnNode originalSecondInsn = originalSecondValue.getProducer();

            // Integer#parseInt(String, int)
            if (!originalFirstInsn.isString() || !originalSecondInsn.isInteger()) return false;

            context.pop(2);
            context.methodNode().instructions.set(
                context.insn(),
                AsmHelper.getNumber(
                    Integer.parseInt(originalFirstInsn.asString(), originalSecondInsn.asInteger())
                )
            );
            return true;
          });

  public static final Match INTEGER_REVERSE =
      MethodMatch.invokeStatic()
          .owner("java/lang/Integer")
          .name("reverse")
          .desc("(I)I")
          .defineTransformation(context -> {
            // Get value from stack
            OriginalSourceValue sourceValue = context.frame().getStack(context.frame().getStackSize() - 1);
            OriginalSourceValue originalSourceValue = sourceValue.originalSource;
            if (!originalSourceValue.isOneWayProduced()) return false;

            AbstractInsnNode originalInsn = originalSourceValue.getProducer();
            // Integer#reverse(int)
            if (!originalInsn.isInteger()) return false;

            context.pop(1);
            context.methodNode().instructions.set(
                context.insn(),
                AsmHelper.getNumber(Integer.reverse(originalInsn.asInteger()))
            );
            return true;
          });

  public static final Match LONG_REVERSE =
      MethodMatch.invokeStatic()
          .owner("java/lang/Long")
          .name("reverse")
          .desc("(J)J")
          .defineTransformation(context -> {
            // Get value from stack
            OriginalSourceValue sourceValue = context.frame().getStack(context.frame().getStackSize() - 1);
            OriginalSourceValue originalSourceValue = sourceValue.originalSource;
            if (!originalSourceValue.isOneWayProduced()) return false;

            AbstractInsnNode originalInsn = originalSourceValue.getProducer();
            // Long#reverse(long)
            if (!originalInsn.isLong()) return false;

            context.pop(1);
            context.methodNode().instructions.set(
                context.insn(),
                AsmHelper.getNumber(Long.reverse(originalInsn.asLong()))
            );
            return true;
          });

  public static final Match FLOAT_TO_BITS =
      MethodMatch.invokeStatic()
          .owner("java/lang/Float")
          .name("floatToIntBits")
          .desc("(F)I")
          .defineTransformation(context -> {
            // Get value from stack
            OriginalSourceValue sourceValue = context.frame().getStack(context.frame().getStackSize() - 1);
            OriginalSourceValue originalSourceValue = sourceValue.originalSource;
            if (!originalSourceValue.isOneWayProduced()) return false;

            AbstractInsnNode originalInsn = originalSourceValue.getProducer();
            // Float#floatToIntBits(float)
            if (!originalInsn.isFloat()) return false;

            context.pop(1);
            context.methodNode().instructions.set(
                context.insn(),
                AsmHelper.getNumber(Float.floatToIntBits(originalInsn.asFloat()))
            );
            return true;
          });

  public static final Match BITS_TO_FLOAT =
      MethodMatch.invokeStatic()
          .owner("java/lang/Float")
          .name("intBitsToFloat")
          .desc("(I)F")
          .defineTransformation(context -> {
            // Get value from stack
            OriginalSourceValue sourceValue = context.frame().getStack(context.frame().getStackSize() - 1);
            OriginalSourceValue originalSourceValue = sourceValue.originalSource;
            if (!originalSourceValue.isOneWayProduced()) return false;

            AbstractInsnNode originalInsn = originalSourceValue.getProducer();
            // Float#intBitsToFloat(int)
            if (!originalInsn.isInteger()) return false;

            context.pop(1);
            context.methodNode().instructions.set(
                context.insn(),
                AsmHelper.getNumber(Float.intBitsToFloat(originalInsn.asInteger()))
            );
            return true;
          });

  public static final Match DOUBLE_TO_BITS =
      MethodMatch.invokeStatic()
          .owner("java/lang/Double")
          .name("doubleToLongBits")
          .desc("(D)J")
          .defineTransformation(context -> {
            // Get value from stack
            OriginalSourceValue sourceValue = context.frame().getStack(context.frame().getStackSize() - 1);
            OriginalSourceValue originalSourceValue = sourceValue.originalSource;
            if (!originalSourceValue.isOneWayProduced()) return false;

            AbstractInsnNode originalInsn = originalSourceValue.getProducer();
            // Double#doubleToLongBits(double)
            if (!originalInsn.isDouble()) return false;

            context.pop(1);
            context.methodNode().instructions.set(
                context.insn(),
                AsmHelper.getNumber(Double.doubleToLongBits(originalInsn.asDouble()))
            );
            return true;
          });

  public static final Match BITS_TO_DOUBLE =
      MethodMatch.invokeStatic()
          .owner("java/lang/Double")
          .name("longBitsToDouble")
          .desc("(J)D")
          .defineTransformation(context -> {
            // Get value from stack
            OriginalSourceValue sourceValue = context.frame().getStack(context.frame().getStackSize() - 1);
            OriginalSourceValue originalSourceValue = sourceValue.originalSource;
            if (!originalSourceValue.isOneWayProduced()) return false;

            AbstractInsnNode originalInsn = originalSourceValue.getProducer();
            // Double#longBitsToDouble(long)
            if (!originalInsn.isLong()) return false;

            context.pop(1);
            context.methodNode().instructions.set(
                context.insn(),
                AsmHelper.getNumber(Double.longBitsToDouble(originalInsn.asLong()))
            );
            return true;
          });

  public static final List<Match> METHOD_CALLS_ON_LITERALS = List.of(
      STRING_LENGTH,
      STRING_HASHCODE,
      STRING_TO_INTEGER,
      STRING_TO_INTEGER_RADIX,
      INTEGER_REVERSE,
      LONG_REVERSE,
      FLOAT_TO_BITS,
      BITS_TO_FLOAT,
      DOUBLE_TO_BITS,
      BITS_TO_DOUBLE
  );

  private AsmMathHelper() {
    throw new IllegalArgumentException();
  }

  public static boolean isMathBinaryOperation(int opcode) {
    return MATH_BINARY_OPERATIONS.containsKey(opcode);
  }

  public static Number mathBinaryOperation(Number first, Number second, int opcode) {
    return MATH_BINARY_OPERATIONS.get(opcode).apply(first, second);
  }

  public static boolean isMathUnaryOperation(int opcode) {
    return MATH_UNARY_OPERATIONS.containsKey(opcode);
  }

  public static Number mathUnaryOperation(Number number, int opcode) {
    return MATH_UNARY_OPERATIONS.get(opcode).apply(number);
  }

  public static boolean isOneValueCondition(int opcode) {
    return ONE_VALUE_CONDITION_PREDICATES.containsKey(opcode);
  }

  public static boolean condition(int value, int opcode) {
    return ONE_VALUE_CONDITION_PREDICATES.get(opcode).test(value);
  }

  public static boolean isTwoValuesCondition(int opcode) {
    return TWO_VALUES_VALUE_CONDITION_PREDICATES.containsKey(opcode);
  }

  public static boolean condition(int first, int second, int opcode) {
    return TWO_VALUES_VALUE_CONDITION_PREDICATES.get(opcode).test(first, second);
  }

  /**
   * Predict if statement result
   */
  public static Optional<Boolean> predictIf(JumpInsnNode jumpInsn, Frame<OriginalSourceValue> frame) {
    if (AsmMathHelper.isOneValueCondition(jumpInsn.getOpcode())) {
      // One-value if statement

      // Get instruction from stack that is passed to if statement
      OriginalSourceValue sourceValue = frame.getStack(frame.getStackSize() - 1);
      OriginalSourceValue.ConstantValue constantValue = sourceValue.getConstantValue();
      if (constantValue == null) return Optional.empty();

      // Process if statement
      if (constantValue.get() instanceof Integer value) {
        boolean ifResult = AsmMathHelper.condition(
            value, // Value
            jumpInsn.getOpcode() // Opcode
        );

        return Optional.of(ifResult);
      }
    } else if (AsmMathHelper.isTwoValuesCondition(jumpInsn.getOpcode())) {
      // Two-value if statements

      // Get instructions from stack that are passed to if statement
      OriginalSourceValue sourceValue1 = frame.getStack(frame.getStackSize() - 2);
      OriginalSourceValue sourceValue2 = frame.getStack(frame.getStackSize() - 1);
      OriginalSourceValue.ConstantValue constValue1 = sourceValue1.getConstantValue();
      OriginalSourceValue.ConstantValue constValue2 = sourceValue2.getConstantValue();
      if (constValue1 == null || constValue2 == null) return Optional.empty();

      // Process if statement
      if (constValue1.get() instanceof Integer value1 && constValue2.get() instanceof Integer value2) {
        boolean ifResult = AsmMathHelper.condition(
            value1, // First value
            value2, // Second value
            jumpInsn.getOpcode() // Opcode
        );

        return Optional.of(ifResult);
      }
    }

    return Optional.empty();
  }

  /**
   * Predict lookup switch jump
   */
  public static Optional<LabelNode> predictLookupSwitch(LookupSwitchInsnNode lookupSwitchInsn, Frame<OriginalSourceValue> frame) {
    OriginalSourceValue sourceValue = frame.getStack(frame.getStackSize() - 1);
    OriginalSourceValue.ConstantValue constantValue = sourceValue.getConstantValue();
    if (constantValue == null) return Optional.empty();

    if (constantValue.get() instanceof Integer value) {
      int index = lookupSwitchInsn.keys.indexOf(value);

      if (index == -1) {
        // Jump to default
        return Optional.of(lookupSwitchInsn.dflt);
      } else {
        // Match found! Jump to target
        LabelNode targetLabel = lookupSwitchInsn.labels.get(index);
        return Optional.of(targetLabel);
      }
    }

    return Optional.empty();
  }

  /**
   * Predict table switch jump
   */
  public static Optional<LabelNode> predictTableSwitch(TableSwitchInsnNode tableSwitchInsn, Frame<OriginalSourceValue> frame) {
    OriginalSourceValue sourceValue = frame.getStack(frame.getStackSize() - 1);
    OriginalSourceValue.ConstantValue constantValue = sourceValue.getConstantValue();
    if (constantValue == null) return Optional.empty();

    if (constantValue.get() instanceof Integer value) {
      int index = value - tableSwitchInsn.min;

      if (index < 0 || index >= tableSwitchInsn.labels.size()) {
        // Jump to default
        return Optional.of(tableSwitchInsn.dflt);
      } else {
        // Match found! Jump to target
        LabelNode targetLabel = tableSwitchInsn.labels.get(index);
        return Optional.of(targetLabel);
      }
    }

    return Optional.empty();
  }
}
