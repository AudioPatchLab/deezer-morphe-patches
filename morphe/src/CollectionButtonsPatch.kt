package deezer.patches

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
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
