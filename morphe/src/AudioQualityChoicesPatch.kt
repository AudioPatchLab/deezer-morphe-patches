package deezer.patches

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.patch.patch
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

@Suppress("unused")
val audioQualityChoicesPatch = patch(
    name = "Show audio quality choices",
    description = "Shows the native Basic, Standard, High Quality and High Fidelity preferences. Actual streaming and download quality still depend on the account and server response.",
) {
    compatibleWith(deezerCompatibility)
    dependsOn(appBasePatch)
    execute {
        // Change only the UI list builder, not the licence parser or format identifiers.
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
            const/16 p1, 0xf
            :native
            nop
        """.trimIndent())
    }
}
