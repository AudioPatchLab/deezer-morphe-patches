package deezer.patches

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.patch.patch
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

@Suppress("unused")
val audioQualityChoicesPatch = patch(
    name = "Show audio quality choices",
    description = "Shows native HQ and HiFi preferences alongside the qualities already offered by the app. This does not unlock streams: account checks can restore an allowed quality and the server can return a lower-quality format.",
) {
    compatibleWith(deezerCompatibility)
    dependsOn(appBasePatch)
    execute {
        // Basic is intentionally hidden. Keep the other original bits and add
        // only HQ and HiFi; this does not change the server's format decision.
        val choices = mutableClassDefBy("LIIllIIIlIllIllI;").methods.single {
            it.name == "IIllllIIIIllIl" &&
                it.parameterTypes == listOf("Landroid/content/Context;", "I") &&
                it.returnType == "Ljava/util/ArrayList;"
        }
        check(checkNotNull(choices.implementation).instructions.count {
            (it as? ReferenceInstruction)?.reference.toString() ==
                "LIllllIIllllIIllI;-><init>(ILjava/lang/String;Ljava/lang/String;)V"
        } == 4)
        choices.addInstructionsWithLabels(0, """
            if-lez p1, :native
            and-int/lit8 p1, p1, -0x2
            or-int/lit8 p1, p1, 0xc
            :native
            nop
        """.trimIndent())
    }
}
