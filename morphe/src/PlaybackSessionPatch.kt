package deezer.patches

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference

private const val PLAYLOG_REQUESTS = "LIIIIIlIllIllIII;"
private const val GATEWAY_REQUEST = "LlIlIlIIIlIlIIl;"

private fun MutableMethod.skipPlaylogRequest(apiMethod: String) {
    check(returnType == GATEWAY_REQUEST)
    check(checkNotNull(implementation).instructions.count {
        ((it as? ReferenceInstruction)?.reference as? StringReference)?.string == apiMethod
    } == 1)

    // The sender already handles null as no request and completes its cache bookkeeping.
    addInstructions(
        0,
        """
            const/4 v0, 0x0
            return-object v0
        """.trimIndent(),
    )
}

internal val playbackSessionPatch = patch {
    compatibleWith(deezerCompatibility)

    execute {
        // A background media-browser start can refresh an incompletely restored user.
        // Do not overwrite a valid persisted login with an empty signed-in user ID.
        val save = mutableClassDefBy("LIIIllllIllIIIllI;").methods.single {
            it.name == "lIlIIllIIlI" && it.parameterTypes == listOf("Landroid/content/Context;", "Z") &&
                it.returnType == "V"
        }
        check(checkNotNull(save.implementation).instructions.any {
            ((it as? ReferenceInstruction)?.reference as? StringReference)?.string == "Where are you now"
        })
        save.addInstructionsWithLabels(0, """
            if-nez p1, :persist
            sget-object v0, LIIIllllIllIIIllI;->lIlIIllIIlI:LIIIIIIIlIlllIlll;
            if-eqz v0, :incomplete
            iget-object v0, v0, LIIIIIIIlIlllIlll;->IllIlIlIIlIllIll:Ljava/lang/String;
            invoke-static {v0}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z
            move-result v0
            if-eqz v0, :persist
            :incomplete
            return-void
            :persist
            nop
        """.trimIndent())
        // Playback logs share their sender with other logs. Skip only the two listening endpoints.
        val requests = mutableClassDefBy(PLAYLOG_REQUESTS)
        requests.methods.single {
            it.name == "IIIIIllIlIIllll" && it.parameterTypes == listOf("LllIlIIlllllII;")
        }.skipPlaylogRequest("log.listen")
        requests.methods.single {
            it.name == "llIIIIIIlIIIIll" && it.parameterTypes == listOf("Ljava/util/List;")
        }.skipPlaylogRequest("log.batch_listen")
    }
}
