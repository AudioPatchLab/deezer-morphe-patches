package deezer.patches

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.patch.patch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

private const val BUTTON_MODE = "LIlIlIlIlllllIlll;"
private const val SHUFFLE = "$BUTTON_MODE->IllIIIllIIllIlll:$BUTTON_MODE"
private const val COLLECTION_TYPE = "LlllIllIlIIIl;"
private const val PLAYBACK_SCOPE = "LIIlIlIlllllIllII;"
private const val CONTAINER = "LIIlIllllIIIIIIlI;"
private const val CONTAINER_STATE = "LIIIlIlIIlIIlllI;"
private const val CONTAINER_TYPE = "LlllIllIlI;"
private const val PLAYING_STATE = "LIIlIIllIIllIlIll;"

internal val collectionButtonsPatch = patch {
    compatibleWith(deezerCompatibility)

    execute {
        val playlistButton = mutableClassDefBy("LIIIlIIlllllIIIl;").methods.single {
            it.name == "invokeSuspend" && it.parameterTypes == listOf("Ljava/lang/Object;")
        }
        val playlistInstructions = checkNotNull(playlistButton.implementation).instructions
        val shuffleLoad = playlistInstructions.indices.single { index ->
            val instruction = playlistInstructions[index]
            instruction.opcode == Opcode.SGET_OBJECT && (instruction as ReferenceInstruction).reference.toString() == SHUFFLE
        }
        val visibleButton = playlistInstructions[shuffleLoad + 2] as FiveRegisterInstruction
        check(visibleButton.opcode == Opcode.INVOKE_DIRECT && (visibleButton as ReferenceInstruction).reference.toString() ==
            "LllIlIlIlIIlIl;-><init>(Z$BUTTON_MODE)V")
        // The click handler reads the same mode as the model driving the visible icon.
        playlistButton.addInstructions(shuffleLoad + 2, "sget-object v${visibleButton.registerE}, $SHUFFLE")

        val playlistState = mutableClassDefBy("LIlIlIllllIlIIlll;").methods.single {
            it.name == "IlIIllllIlIIIlI" && it.returnType == PLAYING_STATE &&
                it.parameterTypes == listOf(CONTAINER_STATE, "Ljava/lang/String;", CONTAINER_TYPE, "[$CONTAINER_TYPE")
        }
        val stateInstructions = checkNotNull(playlistState.implementation).instructions
        val loading = stateInstructions.indices.single { index ->
            stateInstructions[index].opcode == Opcode.SGET_OBJECT &&
                (stateInstructions[index] as ReferenceInstruction).reference.toString() ==
                "$PLAYING_STATE->IIIlllIIlllIIll:$PLAYING_STATE"
        }
        check(playlistState.implementation?.registerCount == 7 && stateInstructions[loading - 1].opcode == Opcode.IF_EQZ)
        // A pending request for the playing playlist can linger across row selections.
        // Prefer the settled player state when the pending and active containers are identical.
        playlistState.addInstructionsWithLabels(
            loading,
            """
                sget-object v0, $CONTAINER_TYPE->lllIllIIlIIIIII:$CONTAINER_TYPE
                if-ne v5, v0, :loading
                iget-object v0, p0, $CONTAINER_STATE->lllIlIIIlllIIII:$CONTAINER
                iget-object v1, p0, $CONTAINER_STATE->lIllllIIIIII:$CONTAINER
                invoke-static {v0, v1}, LlIIIIllIIIIlllIl;->lllIlllIlllIl(Ljava/lang/Object;Ljava/lang/Object;)Z
                move-result v0
                if-eqz v0, :loading
                iget-object v1, p0, $CONTAINER_STATE->llIIlllIIlIlIII:$PLAYING_STATE
                sget-object v0, $PLAYING_STATE->lIllIIlIIIlIlII:$PLAYING_STATE
                if-eq v1, v0, :settled
                sget-object v0, $PLAYING_STATE->IllIIIllIIllIlll:$PLAYING_STATE
                if-ne v1, v0, :loading
                :settled
                return-object v1
                :loading
                nop
            """.trimIndent(),
        )

        // The scope at ordinal 5 selects native playback for a non-null collection ID.
        // Album and playlist requests keep their own IDs, track lists and shuffle flags.
        val collectionPolicy = mutableClassDefBy("LllIIlIllIIIllIl;").methods.single {
            it.name == "IllIIIlIIII" && it.parameterTypes == listOf("Ljava/lang/String;", COLLECTION_TYPE) &&
                it.returnType == "Z"
        }
        val policyInstructions = checkNotNull(collectionPolicy.implementation).instructions
        for (scope in listOf("IlllIIlIIIlll", "lllIllIIlIIIIII")) {
            val scopeLoad = policyInstructions.indices.single { index ->
                val instruction = policyInstructions[index]
                instruction.opcode == Opcode.SGET_OBJECT && (instruction as ReferenceInstruction).reference.toString() ==
                    "$PLAYBACK_SCOPE->$scope:$PLAYBACK_SCOPE"
            }
            val register = (policyInstructions[scopeLoad] as OneRegisterInstruction).registerA
            collectionPolicy.replaceInstruction(
                scopeLoad, "sget-object v$register, $PLAYBACK_SCOPE->IIIllllllIlllIlI:$PLAYBACK_SCOPE",
            )
        }
    }
}
