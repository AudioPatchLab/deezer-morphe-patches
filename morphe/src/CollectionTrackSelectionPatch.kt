package deezer.patches

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableFieldReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction10t
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction22c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21t
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction22t

internal val deezerCompatibility = Compatibility(
    name = "Deezer",
    packageName = "deezer.android.app",
    apkFileType = ApkFileType.APKM_REQUIRED,
    targets = listOf(AppTarget(version = "9.0.26.3", minSdk = 26)),
)

private const val TRACK_RIGHTS = "LIIllIIIIIIllI;"
private const val DISPLAY_RIGHTS = "morpheCollectionDisplayRights"
private const val TRACK_ROW_CONSTRUCTOR = "LIIIlIIIIllIllII;-><init>(" +
    "Ljava/lang/String;Ljava/lang/String;$TRACK_RIGHTS" +
    "Ljava/lang/String;Ljava/util/List;Ljava/util/List;Ljava/lang/String;IZZIIZ" +
    "LlIllllIlIIlllI;FZLllllIlIIIlIlIll;Ljava/lang/Integer;I)V"

private fun collectionDisplayRightsMethod(): MutableMethod {
    val method = MutableMethod(
        ImmutableMethod(
            TRACK_RIGHTS, DISPLAY_RIGHTS, listOf(ImmutableMethodParameter(TRACK_RIGHTS, emptySet(), null)),
            TRACK_RIGHTS, AccessFlags.PUBLIC.value or AccessFlags.STATIC.value,
            emptySet(), emptySet(), ImmutableMethodImplementation(2, emptyList(), emptyList(), emptyList()),
        ),
    )
    method.addInstructionsWithLabels(
        0,
        """
            sget-object v0, $TRACK_RIGHTS->IllIIIllIIllIlll:$TRACK_RIGHTS
            if-ne p0, v0, :keep_rights
            sget-object p0, $TRACK_RIGHTS->IlllIIlIIIlll:$TRACK_RIGHTS
            :keep_rights
            return-object p0
        """.trimIndent(),
    )
    return method
}

private fun MutableMethod.showSelectableRows(numberedTitle: Boolean = false) {
    val instructions = checkNotNull(implementation).instructions
    val index = instructions.indices.single { index ->
        val instruction = instructions[index]
        instruction.opcode == Opcode.INVOKE_DIRECT_RANGE &&
            (instruction as ReferenceInstruction).reference.toString() == TRACK_ROW_CONSTRUCTOR
    }
    val call = instructions[index] as RegisterRangeInstruction
    check(call.registerCount == 20)
    val rights = call.startRegister + 3
    if (numberedTitle) {
        val title = call.startRegister + 2
        val number = call.startRegister + 18
        // The native model otherwise gives the number the cover's slot.
        addInstructionsWithLabels(index, """
            move-object/from16 v7, v$number
            if-eqz v7, :keep_title
            invoke-virtual {v7}, Ljava/lang/Integer;->intValue()I
            move-result v7
            invoke-static {v7, v11}, $NUMBERED_TITLE
            move-result-object v$title
            const/16 v$number, 0x0
            :keep_title
            nop
        """.trimIndent())
    }
    addInstructions(
        index,
        """
            invoke-static/range {v$rights .. v$rights}, $TRACK_RIGHTS->$DISPLAY_RIGHTS($TRACK_RIGHTS)$TRACK_RIGHTS
            move-result-object v$rights
        """.trimIndent(),
    )
}

private fun MutableMethod.replaceField(
    opcode: Opcode,
    owner: String,
    field: String,
    fieldType: String,
    replacement: String,
) {
    val instructions = checkNotNull(implementation).instructions
    val index = instructions.indices.single { index ->
        val instruction = instructions[index]
        instruction.opcode == opcode &&
            (instruction as ReferenceInstruction).reference.toString() == "$owner->$field:$fieldType"
    }
    val reference = ImmutableFieldReference(owner, replacement, fieldType)
    val instruction = instructions[index] as OneRegisterInstruction
    val updated = if (opcode == Opcode.IGET_OBJECT) {
        BuilderInstruction22c(opcode, instruction.registerA, (instruction as TwoRegisterInstruction).registerB, reference)
    } else {
        BuilderInstruction21c(opcode, instruction.registerA, reference)
    }
    replaceInstruction(index, updated)
}

private fun MutableMethod.useSelectedTrackAction() {
    val instructions = checkNotNull(implementation).instructions
    val rightsRead = instructions.indices.single { index ->
        val instruction = instructions[index]
        instruction.opcode == Opcode.IGET_OBJECT && (instruction as ReferenceInstruction)
            .reference.toString() == "LlIIIIlllIIllII;->lllIlIIIlllIIII:LIIllIIIIIIllI;"
    }
    check(instructions[rightsRead + 1].opcode == Opcode.INVOKE_VIRTUAL)
    check(instructions[rightsRead + 2].opcode == Opcode.MOVE_RESULT)
    val readable = instructions[rightsRead + 3] as BuilderInstruction21t
    check(readable.opcode == Opcode.IF_EQZ)
    val noOnDemand = instructions[rightsRead + 4] as BuilderInstruction22t
    check(noOnDemand.opcode == Opcode.IF_EQ && noOnDemand.registerA == readable.registerA)

    // NO_MOD goes to the same handler as READABLE. Unavailable and restricted tracks keep their branches.
    replaceInstruction(
        rightsRead + 4,
        BuilderInstruction22t(noOnDemand.opcode, noOnDemand.registerA, noOnDemand.registerB, readable.target),
    )

    val playbackPolicy = instructions.indices.single { index ->
        val instruction = instructions[index]
        instruction.opcode == Opcode.INVOKE_VIRTUAL && (instruction as ReferenceInstruction)
            .reference.toString() == "LlllIIIIlIIIlIlI;->IllIIIlIIII(Ljava/lang/String;)LIIllllIllIIIll;"
    }
    val policyResult = instructions[playbackPolicy + 1] as OneRegisterInstruction
    check(policyResult.opcode == Opcode.MOVE_RESULT_OBJECT)
    // NotActive is the app's normal selection path, which passes the album/playlist and the track ID.
    replaceInstruction(playbackPolicy, "nop")
    replaceInstruction(
        playbackPolicy + 1,
        "sget-object v${policyResult.registerA}, LlIlllIlIIII;->lIllllIIIIII:LlIlllIlIIII;",
    )
}

internal val collectionTrackSelectionPatch = patch {
    compatibleWith(deezerCompatibility)
    dependsOn(numberedTrackTitleSupport)

    execute {
        // The album page combines the preview flag with the user's playback mode.
        // When that combination selects TrackPreview, use the Tracks state instead.
        val albumTrackState = "LIIIIIlIlIIIlIll;"
        mutableClassDefBy("LIlllIlllIIIlIIIl;").methods.single {
            it.name == "invokeSuspend" && it.parameterTypes == listOf("Ljava/lang/Object;")
        }.replaceField(Opcode.SGET_OBJECT, albumTrackState, "IllIIIllIIllIlll", albumTrackState, "IlllIIlIIIlll")

        // The playlist view model has separate data flows for inline tracks and the preview card.
        mutableClassDefBy("LlIIlllIlIllII;").methods.single {
            it.name == "invoke" && it.parameterTypes.isEmpty()
        }.replaceField(
            Opcode.IGET_OBJECT, "LIlllIIIlIIlIIIll;", "IlIlllIIlllIllll",
            "LIlllIlllIlllllII;", "llIllIlIlIIIIIl",
        )

        check(mutableClassDefBy(TRACK_RIGHTS).methods.add(collectionDisplayRightsMethod()))
        mutableClassDefBy("LIlIllllIlIIlIl;").methods.single {
            it.name == "invokeSuspend" && it.parameterTypes == listOf("Ljava/lang/Object;")
        }.showSelectableRows(numberedTitle = true)
        mutableClassDefBy("LIlIIlIlllllIlIll;").methods.single {
            it.name == "invokeSuspend" && it.parameterTypes == listOf("Ljava/lang/Object;")
        }.showSelectableRows()

        mutableClassDefBy("LIIlIIlIlIIllIlII;").methods.single {
            it.name == "invokeSuspend" && it.parameterTypes == listOf("Ljava/lang/Object;")
        }.useSelectedTrackAction()
        mutableClassDefBy("LIIlIIlllIllllII;").methods.single {
            it.name == "IlIllllIIlIIllIl" && it.parameterTypes == listOf("Ljava/lang/Object;")
        }.useSelectedTrackAction()

        // Starting a collection and starting a selected track share the same router.
        // Only the FromTrack requests use the direct queue action on ad-supported accounts.
        mutableClassDefBy("LIIIIlIlllIIllII;").methods.single {
            it.name == "IllIIIlIIII" && it.parameterTypes == listOf("LlIIIIlIll;")
        }.addInstructionsWithLabels(
            0,
            """
                iget-object v0, p0, LIIIIlIlllIIllII;->IIllIIIlIIIllII:LIlIlIllIlIIlII;
                iget-object v1, v0, LIlIlIllIlIIlII;->lIllIIIlIlIlll:LlIlIllIIIllIIIl;
                instance-of v1, v1, LIlIlllIIIIIIIIII;
                if-eqz v1, :original
                return-object v0
                :original
                nop
            """.trimIndent(),
        )
        mutableClassDefBy("LlllllIlIllIIlII;").methods.single {
            it.name == "IllIIIlIIII" && it.parameterTypes == listOf("LlIIIIlIll;")
        }.addInstructionsWithLabels(
            0,
            """
                iget-object v0, p0, LlllllIlIllIIlII;->IllIIIlIIII:LlIIIIlIIIIlIIIl;
                iget-object v1, v0, LlIIIIlIIIIlIIIl;->lIllIIIlIlIlll:LIlIlllIIllIllIII;
                instance-of v1, v1, LlIlIllllIl;
                if-eqz v1, :original
                return-object v0
                :original
                nop
            """.trimIndent(),
        )

        // Do not downgrade normal album/playlist playback to a radio mix when MOD is false.
        mutableClassDefBy("LllIIlllIllIIIl;").methods.single {
            it.name == "IllIIllIllIIII" &&
                it.parameterTypes == listOf("LIIIlIIlIlllIIlll;", "LIllIIIlllI;") &&
                it.returnType == "Z"
        }.apply {
            val instructions = checkNotNull(implementation).instructions
            val modCheck = instructions.indices.first { index ->
                instructions[index].opcode == Opcode.INVOKE_VIRTUAL &&
                    (instructions[index] as ReferenceInstruction).reference.toString() ==
                    "LIIlIIlIlllIIlll;->lIllllIIIIII(I)Z"
            }
            val moveResult = instructions[modCheck + 1] as OneRegisterInstruction
            check(moveResult.opcode == Opcode.MOVE_RESULT)
            addInstructions(
                modCheck + 1,
                """
                    const/4 v0, 0x1
                    return v0
                """.trimIndent(),
            )
        }

        // Bypass JukeboxService's MOD/RADIO rights bitmask gate when starting selected tracks.
        val jukeboxService = mutableClassDefBy("Lcom/deezer/core/jukebox/JukeboxService;")
        jukeboxService.methods.single {
            it.name == "IIIIlIIIIIIIIlll" && it.parameterTypes == listOf("LlllIllIIlllllll;") && it.returnType == "V"
        }.apply {
            val instructions = checkNotNull(implementation).instructions
            val blockCall = instructions.indices.single { index ->
                instructions[index].opcode == Opcode.INVOKE_VIRTUAL &&
                    (instructions[index] as ReferenceInstruction).reference.toString() ==
                    "Lcom/deezer/core/jukebox/JukeboxService;->IllIIIlIIII(I)V"
            }
            val branch = instructions[blockCall - 1] as BuilderInstruction21t
            check(branch.opcode == Opcode.IF_EQZ)
            replaceInstruction(blockCall - 1, BuilderInstruction10t(Opcode.GOTO, branch.target))
        }
        jukeboxService.methods.single {
            it.name == "lIllllIIIIII" && it.parameterTypes == listOf("LIIllIIllIllIIlI;") && it.returnType == "Z"
        }.addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """.trimIndent(),
        )
    }
}
