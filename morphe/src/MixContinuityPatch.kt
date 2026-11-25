package deezer.patches

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

private const val MIX_DISPATCH = "LlllIlIIIlIlllIl;"
private const val MIX_ARGUMENTS = "LIIlllllIIlIlII;Ljava/util/List;LIlIIlllIIlIIllll;Ljava/util/List;LIIIIllIlIIlIIIIl;"

internal val mixContinuityPatch = patch {
    compatibleWith(deezerCompatibility)
    execute {
        val owner = mutableClassDefBy(MIX_DISPATCH)
        val parameters = listOf("LIIlllllIIlIlII;", "Ljava/util/List;", "LIlIIlllIIlIIllll;",
            "Ljava/util/List;", "LIIIIllIlIIlIIIIl;")
        owner.methods.add(MutableMethod(ImmutableMethod(
            MIX_DISPATCH, "morpheContinueCurrentMix", parameters.map {
                ImmutableMethodParameter(it, emptySet(), null)
            }, "Z", AccessFlags.PUBLIC.value or AccessFlags.STATIC.value,
            emptySet(), emptySet(), ImmutableMethodImplementation(17, emptyList(), emptyList(), emptyList()),
        )).apply {
            addInstructionsWithLabels(0, """
                move-object/from16 v0, p0
                move-object/from16 v1, p1
                move-object/from16 v4, p4
                invoke-interface {v4}, LIIIIllIlIIlIIIIl;->lIlIIllIIlI()LlIlIIlllIIIIIl;
                move-result-object v5
                sget-object v6, LlIlIIlllIIIIIl;->lIllIlIllllIIlI:LlIlIIlllIIIIIl;
                if-eq v5, v6, :mix
                sget-object v6, LlIlIIlllIIIIIl;->IIlIlIIIllIIII:LlIlIIlllIIIIIl;
                if-ne v5, v6, :native
                :mix
                invoke-interface {v4}, LIIIlIIllIlIIIIl;->IIllIIIIIIIllIll()LIIIlIllllIlIIlII;
                move-result-object v5
                iget-object v5, v5, LIIIlIllllIlIIlII;->IllIlIlIIlIllIll:Ljava/lang/String;
                invoke-interface {v0}, LIIlllllIIlIlII;->IIIIllllIIIIIllI()LIIIlIlIIlllIlIIl;
                move-result-object v2
                if-eqz v2, :native
                invoke-interface {v2}, LIlIIlllIIlIIllll;->lIlIIIlIlIIlIlI()Ljava/lang/String;
                move-result-object v6
                invoke-static {v5, v6}, Landroid/text/TextUtils;->equals(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Z
                move-result v6
                if-eqz v6, :native
                new-instance v7, Ljava/util/ArrayList;
                invoke-direct {v7}, Ljava/util/ArrayList;-><init>()V
                invoke-interface {v1}, Ljava/util/List;->iterator()Ljava/util/Iterator;
                move-result-object v8
                :next
                invoke-interface {v8}, Ljava/util/Iterator;->hasNext()Z
                move-result v9
                if-eqz v9, :update
                invoke-interface {v8}, Ljava/util/Iterator;->next()Ljava/lang/Object;
                move-result-object v9
                check-cast v9, LIlIIlllIIlIIllll;
                invoke-interface {v9}, LIlIIlllIIlIIllll;->lIlIIIlIlIIlIlI()Ljava/lang/String;
                move-result-object v10
                invoke-static {v5, v10}, Landroid/text/TextUtils;->equals(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Z
                move-result v10
                if-nez v10, :next
                invoke-virtual {v7, v9}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z
                goto :next
                :update
                invoke-static {v4}, LllIllIIllllIlI;->IlllllllllIIIIII(LIIIIllIlIIlIIIIl;)LIIlIllIIllllllI;
                move-result-object v4
                invoke-static {v4}, LlIIlIIllIIIlllI;->lIllIIllIIl(LIIlIllIIllllllI;)LlIlIlIlIIlIIl;
                move-result-object v4
                # Edit only upcoming tracks. A replacement/play request resets the decoder,
                # and the shuffle replacement can select an entirely different track.
                invoke-virtual {v7}, Ljava/util/ArrayList;->isEmpty()Z
                move-result v3
                if-nez v3, :done
                invoke-static {}, LIlllIIIlIlllllI;->IllIlIlIIlIllIll()LIlllIIIlIlllllI;
                move-result-object v3
                const/4 v6, -0x1
                iput v6, v3, LIlllIIIlIlllllI;->llIllllIllIIlIl:I
                const/4 v6, -0x2
                iput v6, v3, LIlllIIIlIlllllI;->lIlIIllIIlI:I
                invoke-interface {v0}, LIIlllllIIlIlII;->isShuffle()Z
                move-result v6
                iput-boolean v6, v3, LIlllIIIlIlllllI;->llIIIIIIlIIIIll:Z
                invoke-interface {v0}, LIIlllllIIlIlII;->IIlIlIlIlIllllll()V
                invoke-interface {v0, v7, v4, v3}, LIIlllllIIlIlII;->IIIIIllIIIlIIllI(Ljava/util/List;LlIlIlIlIIlIIl;LIlllIIIlIlllllI;)V
                move-object/from16 v3, p3
                if-eqz v3, :done
                invoke-interface {v0, v3}, LIIlllllIIlIlII;->llIlIllIIIl(Ljava/util/List;)V
                :done
                const/4 v0, 0x1
                return v0
                :native
                const/4 v0, 0x0
                return v0
            """.trimIndent())
        })
        owner.methods.single {
            it.name == "IlIllIIIlIIIlIIl" && it.parameterTypes == parameters
        }.addInstructionsWithLabels(0, """
            invoke-static/range {p0 .. p4}, $MIX_DISPATCH->morpheContinueCurrentMix($MIX_ARGUMENTS)Z
            move-result v0
            if-eqz v0, :native
            return-void
            :native
            nop
        """.trimIndent())
        // Full-track mix requests must not be downgraded to a different recommendation action.
        mutableClassDefBy("LlllIlllIIllllIl;").methods.single {
            it.name == "IIlIlIlIlIllllll" && it.parameterTypes == listOf("LlIIIIIlllIlIlll;")
        }.addInstructionsWithLabels(0, """
            iget-object v0, p0, LlllIlllIIllllIl;->IIIIIllIlIIllll:LlIIIIIlIIIIIlll;
            iget-object v1, v0, LlIIIIIlIIIIIlll;->IIllllIIIIIlIIl:LIllllllIIIlIlIl;
            iget-object v1, v1, LIllllllIIIlIlIl;->llIIIIIIlIIIIll:LlIlIIlllIIIIIl;
            sget-object v2, LlIlIIlllIIIIIl;->lIllIlIllllIIlI:LlIlIIlllIIIIIl;
            if-eq v1, v2, :full
            sget-object v2, LlIlIIlllIIIIIl;->IIlIlIIIllIIII:LlIlIIlllIIIIIl;
            if-ne v1, v2, :native
            :full
            return-object v0
            :native
            nop
        """.trimIndent())
    }
}
