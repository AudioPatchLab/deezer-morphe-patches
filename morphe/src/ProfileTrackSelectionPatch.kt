package deezer.patches

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.patch.patch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

internal val profileTrackSelectionPatch = patch {
    compatibleWith(deezerCompatibility)
    dependsOn(trackCellDisplayRightsPatch)

    execute {
        val rows = mutableClassDefBy("LllIlIllllIIlll;").methods.single {
            it.name == "invokeSuspend" && it.parameterTypes == listOf("Ljava/lang/Object;")
        }
        val instructions = checkNotNull(rows.implementation).instructions
        val profileMapper = instructions.indices.single { index ->
            (instructions[index] as? ReferenceInstruction)?.reference.toString() ==
                "LIIIllIIIlIIIIlI;->lIIIIIlllIlIIIIl(LIllIlllIlIIlllII;)LlIIlIIIIIlIlI;"
        }
        val bind = (profileMapper + 1 until instructions.size).first { index ->
            instructions[index].opcode == Opcode.INVOKE_STATIC_RANGE
        }
        rows.showSelectableTrackCell(bind)

        // The profile's native Top tracks queue starts with the selected track ID.
        val tap = mutableClassDefBy("LlIIllllIIlllI;").methods.single {
            it.name == "invoke" && it.parameterTypes == listOf("Ljava/lang/Object;")
        }
        val actions = checkNotNull(tap.implementation).instructions
        val policy = actions.indices.single { index ->
            actions[index].opcode == Opcode.INVOKE_VIRTUAL &&
                (actions[index] as ReferenceInstruction).reference.toString() ==
                "LllIIlIllIIIllIl;->IllIIIlIIII(Ljava/lang/String;LlllIllIlIIIl;)Z" &&
                (actions.getOrNull(index + 2) as? ReferenceInstruction)?.reference.toString() ==
                "LIlIIlIIIlIlIlIlI;->IIlllIllllllII:LIlIIlIIIlIlIlIlI;"
        }
        val result = actions[policy + 1] as OneRegisterInstruction
        check(result.opcode == Opcode.MOVE_RESULT && actions[policy + 3].opcode == Opcode.IF_EQZ)
        tap.replaceInstruction(policy, "nop")
        tap.replaceInstruction(policy + 1, "const/4 v${result.registerA}, 0x1")

        // The full list uses legacy rows. Only change the playback policy for its callback.
        val legacyRows = mutableClassDefBy("LIllIIIlIIlllllIl;").methods.single {
            it.name == "IllIIIlIIII" && it.parameterTypes == listOf("LlIIIIlIllIIIIIll;")
        }
        val legacyInstructions = checkNotNull(legacyRows.implementation).instructions
        val legacyPolicy = legacyInstructions.indices.single { index ->
            (legacyInstructions[index] as? ReferenceInstruction)?.reference.toString() ==
                "LlllIlIlIIlIlI;->lIllllIIIIII(LIIlIlIlllllIllII;Ljava/lang/String;)Z"
        }
        val legacyResult = legacyInstructions[legacyPolicy + 1] as OneRegisterInstruction
        check(legacyResult.opcode == Opcode.MOVE_RESULT &&
            (legacyInstructions[legacyPolicy + 2] as? ReferenceInstruction)?.reference.toString() ==
            "LIllIIIlIIlllllIl;->lIllIIIlIlIlll:LllIIIIIIllllllI;")
        legacyRows.addInstructionsWithLabels(
            legacyPolicy + 2,
            """
                move-object/from16 v8, p0
                iget-object v8, v8, LIllIIIlIIlllllIl;->lIIIIlIIlIllIlll:LIlllIllIlIllIl;
                instance-of v8, v8, LIlIlIIlIllIlIlII;
                if-eqz v8, :keep_policy
                const/4 v${legacyResult.registerA}, 0x1
                :keep_policy
                nop
            """.trimIndent(),
        )

        val legacyTap = mutableClassDefBy("LIlIlIIlIllIlIlII;").methods.single {
            it.name == "IIIlIlIllllIllIl" && it.parameterTypes == listOf("Landroid/view/View;", "Ljava/lang/Object;")
        }
        val legacyActions = checkNotNull(legacyTap.implementation).instructions
        val route = legacyActions.indices.single { index ->
            (legacyActions[index] as? ReferenceInstruction)?.reference.toString() ==
                "LlllIIIIlIIIlIlI;->IllIIIlIIII(Ljava/lang/String;)LIIllllIllIIIll;"
        }
        val routeResult = legacyActions[route + 1] as OneRegisterInstruction
        check(routeResult.opcode == Opcode.MOVE_RESULT_OBJECT)
        val routeRegister = routeResult.registerA
        // Keep the native paths for replaying a mix and starting the profile queue.
        legacyTap.addInstructionsWithLabels(
            route + 2,
            """
                instance-of v3, v$routeRegister, LIIIllIlIIll;
                if-eqz v3, :keep_route
                sget-object v$routeRegister, LlIlllIlIIII;->lIllllIIIIII:LlIlllIlIIII;
                :keep_route
                nop
            """.trimIndent(),
        )
    }
}
