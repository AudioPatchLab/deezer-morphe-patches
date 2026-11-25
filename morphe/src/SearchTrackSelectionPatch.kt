package deezer.patches

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.patch.patch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

internal val searchTrackSelectionPatch = patch {
    compatibleWith(deezerCompatibility)
    dependsOn(exactSearchTrackPatch)
    execute {
        val mapper = mutableClassDefBy("LllIIIlIlIIlIIl;").methods.single {
            it.name == "IIIIIllIlIIllll" && it.parameterTypes == listOf(
                "Ljava/util/List;", "Lcom/deezer/core/data/model/policy/IllIlIlIIlIllIll;",
                "Z", "LIlIlIllIlllllI;")
        }
        val code = checkNotNull(mapper.implementation).instructions
        val blocked = code.indices.single {
            code[it].opcode == Opcode.SGET_OBJECT &&
                (code[it] as ReferenceInstruction).reference.toString() ==
                "LlIlIlIlllIIIll;->IIlllIlllIlll:LlIlIlIlllIIIll;"
        }
        check(code[blocked - 1].opcode == Opcode.IF_NEZ)
        val register = (code[blocked] as OneRegisterInstruction).registerA
        // Only replace the account on-demand denial. Catalogue availability,
        // explicit-content and licensing checks above this branch remain native.
        mapper.replaceInstruction(blocked,
            "sget-object v$register, LlIlIlIlllIIIll;->IIllllllllIlll:LlIlIlIlllIIIll;")

        // Recent searches use a separate legacy-style row and cached history model.
        // Both mappers derive the same on-demand-only state from account policy.
        for (type in listOf("LIlllIllIlllIllI;", "LIllIlIllll;")) {
            val recent = mutableClassDefBy(type).methods.single {
                it.name == "invokeSuspend" && it.parameterTypes == listOf("Ljava/lang/Object;")
            }
            val insns = checkNotNull(recent.implementation).instructions
            val denial = insns.indices.single {
                insns[it].opcode == Opcode.SGET_OBJECT &&
                    (insns[it] as ReferenceInstruction).reference.toString() ==
                    "LlIlIlIlllIIIll;->IIlllIlllIlll:LlIlIlIlllIIIll;"
            }
            check(insns.subList(maxOf(0, denial - 8), denial).any {
                (it as? ReferenceInstruction)?.reference.toString() ==
                    "LIIllllIlllIlIlI;->IIllIIIIIIIllIll(LlIlllIIlIlllIIl;)Z"
            })
            val target = (insns[denial] as OneRegisterInstruction).registerA
            recent.replaceInstruction(denial,
                "sget-object v$target, LlIlIlIlllIIIll;->IIllllllllIlll:LlIlIlIlllIIIll;")
        }
    }
}
