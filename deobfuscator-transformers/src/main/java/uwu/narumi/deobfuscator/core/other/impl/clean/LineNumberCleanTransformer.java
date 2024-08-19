package uwu.narumi.deobfuscator.core.other.impl.clean;

import java.util.Arrays;
import org.objectweb.asm.tree.LineNumberNode;
import uwu.narumi.deobfuscator.api.asm.ClassWrapper;
import uwu.narumi.deobfuscator.api.context.Context;
import uwu.narumi.deobfuscator.api.transformer.Transformer;

public class LineNumberCleanTransformer extends Transformer {

  private boolean changed = false;

  @Override
  protected boolean transform(ClassWrapper scope, Context context) throws Exception {
    context.classes(scope).stream()
        .flatMap(classWrapper -> classWrapper.methods().stream())
        .forEach(
            methodNode ->
                Arrays.stream(methodNode.instructions.toArray())
                    .filter(node -> node instanceof LineNumberNode)
                    .forEach(node -> {
                        methodNode.instructions.remove(node);
                        changed = true;
                    }));

    return changed;
  }
}
