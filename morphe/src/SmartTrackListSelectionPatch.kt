package deezer.patches

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.patch.patch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

internal val smartTrackListSelectionPatch = patch {
    compatibleWith(deezerCompatibility)
    dependsOn(trackCellDisplayRightsPatch)

    execute {
        val rows = mutableClassDefBy("LlIlIIIIllllII;").methods.single {
            it.name == "invokeSuspend" && it.parameterTypes == listOf("Ljava/lang/Object;")
        }
        val rowInstructions = checkNotNull(rows.implementation).instructions
        val bind = rowInstructions.indices.single { index ->
            rowInstructions[index].opcode == Opcode.INVOKE_STATIC_RANGE &&
                (rowInstructions[index] as ReferenceInstruction).reference.toString().startsWith(
                    "LlIIlllIII;->llIIIIlIlllllIl(LllIllIlIlIlIIl;",
                )
        }
        rows.showSelectableTrackCell(bind)

        // The expanded recommendation derives its row state from account policy and track checks.
        val expandedRows = mutableClassDefBy("LIlllIlllIIlllIIl;").methods.single {
            it.name == "invokeSuspend" && it.parameterTypes == listOf("Ljava/lang/Object;")
        }
        val expandedInstructions = checkNotNull(expandedRows.implementation).instructions
        val displayPolicy = expandedInstructions.indices.single { index ->
            expandedInstructions[index].opcode == Opcode.INVOKE_VIRTUAL &&
                (expandedInstructions[index] as ReferenceInstruction).reference.toString() ==
                "LIIIlllIlIllllIl;->lIllllIIIIII(LIIlIlIlllllIllII;Ljava/lang/String;)Z"
        }
        val displayResult = expandedInstructions[displayPolicy + 1] as OneRegisterInstruction
        check(displayResult.opcode == Opcode.MOVE_RESULT &&
            (expandedInstructions[displayPolicy + 2] as? ReferenceInstruction)?.reference.toString() ==
            "LIllIIIIIllIlllll;->lllIlIIIlllIIII:LllIIIIIIllllllI;")
        expandedRows.replaceInstruction(displayPolicy, "nop")
        expandedRows.replaceInstruction(displayPolicy + 1, "const/4 v${displayResult.registerA}, 0x1")

        val tap = mutableClassDefBy("LIlIIlIllIllIll;").methods.single {
            it.name == "IlIllllIIlIIllIl" &&
                it.parameterTypes == listOf("Ljava/lang/String;", "Z", "LllIIIIIIlllIll;")
        }
        val tapInstructions = checkNotNull(tap.implementation).instructions
        val branch = tapInstructions.indices.single { index ->
            tapInstructions[index].opcode == Opcode.IF_EQZ
        }
        val flag = tapInstructions[branch] as OneRegisterInstruction
        check(flag.registerA == checkNotNull(tap.implementation).registerCount - 2)
        // Use the existing full-track handler for a row tap.
        tap.replaceInstruction(branch, "nop")

        val route = mutableClassDefBy("LIIllIIIIIlllllI;").methods.single {
            it.name == "IllIIIlIIII" && it.parameterTypes == listOf("LlIIIIlIll;")
        }
        // FromTrack carries the selected ID. Leave the header shuffle on its native path.
        route.addInstructionsWithLabels(
            0,
            """
                iget-object v0, p0, LIIllIIIIIlllllI;->IllIIIlIIII:LlIIlllIIlIIIlIl;
                iget-object v1, v0, LIIlIlIIIllIIllIl;->lllIlIIIlllIIII:LIlIIIIlIIIIlllIl;
                instance-of v1, v1, LIlIIIlIlllII;
                if-eqz v1, :original
                return-object v0
                :original
                nop
            """.trimIndent(),
        )
    }
}
