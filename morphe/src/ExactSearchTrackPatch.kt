package deezer.patches

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

private const val SEARCH_REQUEST = "LlIIIIIlIIIIIlll;"

private fun searchMethod(name: String, parameter: String, registers: Int, body: String) =
    MutableMethod(ImmutableMethod(
        SEARCH_REQUEST, name, listOf(ImmutableMethodParameter(parameter, emptySet(), null)),
        "Ljava/lang/Object;", AccessFlags.PUBLIC.value, emptySet(), emptySet(),
        ImmutableMethodImplementation(registers, emptyList(), emptyList(), emptyList()),
    )).apply { addInstructionsWithLabels(0, body.trimIndent()) }

internal val exactSearchTrackPatch = patch {
    compatibleWith(deezerCompatibility)
    execute {
        val request = mutableClassDefBy(SEARCH_REQUEST)
        check(request.interfaces.add("LlIIllIllllIIlIl;"))
        // The dispatcher can reuse an older equal in-flight action for A -> B -> A,
        // leaving B running. Search taps must dispatch anew and cancel the pending job.
        mutableClassDefBy("LlIIlIIIlIIlIII;").methods.single {
            it.name == "IllIlIlIIlIllIll" && it.parameterTypes == listOf("LIllllllIIIlIlIl;")
        }.addInstructionsWithLabels(0, """
            iget-object v0, p1, LIllllllIIIlIlIl;->llIIIIIIlIIIIll:LlIlIIlllIIIIIl;
            sget-object v1, LlIlIIlllIIIIIl;->IIlIlllIllllllll:LlIlIIlllIIIIIl;
            if-eq v0, v1, :fresh
            sget-object v1, LlIlIIlllIIIIIl;->IlIIIIllIIIllIII:LlIlIIlllIIIIIl;
            if-eq v0, v1, :fresh
            sget-object v1, LlIlIIlllIIIIIl;->IIIlIIIllIIlIll:LlIlIIlllIIIIIl;
            if-ne v0, v1, :keep_pending
            :fresh
            iget-object v0, p0, LlIIlIIIlIIlIII;->IllIlIlIIlIllIll:Lcom/deezer/app/libraries/data/player/actions/jukebox/IllIlIlIIlIllIll;
            const/4 v1, 0x0
            iput-object v1, v0, Lcom/deezer/app/libraries/data/player/actions/jukebox/IllIlIlIIlIllIll;->IlIIIlIIIIlllIl:LlIllIlIIlIIIllI;
            :keep_pending
            nop
        """.trimIndent())
        // Keep the selected ID on the request instance, including across suspension.
        // A search tap must fetch that ID, rather than song.getSearchTrackMix.
        request.methods.add(searchMethod("morpheFetchSelectedTrack", "LIlIlIllIlllllI;", 9, """
            iget-object v0, p0, $SEARCH_REQUEST->IIllllIIIIIlIIl:LIllllllIIIlIlIl;
            iget-object v0, v0, LIllllllIIIlIlIl;->IllIlIlIIlIllIll:Ljava/lang/String;
            filled-new-array {v0}, [Ljava/lang/String;
            move-result-object v0
            iget-object v1, p0, $SEARCH_REQUEST->IIlllIlIIlIlII:LllIlllIIIIllIll;
            check-cast v1, LllllIIllIIllllI;
            iget-object v2, v1, LllllIIllIIllllI;->llIIIIIIlIIIIll:LIIIIlllIIlIll;
            iget-object v2, v2, LllIllIllIIlll;->IllIlIlIIlIllIll:LllIlllllllll;
            invoke-interface {v2, v0}, LllIlllllllll;->IIlllIIIIIIIIIlI([Ljava/lang/String;)LlIlIlIIIlIlIIl;
            move-result-object v0
            iget-object v1, v1, LllllIIllIIllllI;->IllIlIlIIlIllIll:LIIIllllIIlIIIII;
            iget-object v2, v1, LIIIllllIIlIIIII;->IllIlIlIIlIllIll:LIIlIlIIIllIIIIIl;
            iget-object v1, v1, LIIIllllIIlIIIII;->IIlIlIlIlIllllll:LIllllIIllIIllIll;
            const-class v3, LlIlIlIlIlIIllI;
            new-instance v4, LIIllIIIlIlIIIll;
            const/4 v5, 0x1
            invoke-direct {v4, v3, v5}, LIIllIIIlIlIIIll;-><init>(Ljava/lang/Object;I)V
            new-instance v3, LIIllIIIlIlIIIll;
            const/4 v5, 0x0
            invoke-direct {v3, v4, v5}, LIIllIIIlIlIIIll;-><init>(Ljava/lang/Object;I)V
            iget-object v1, v1, LIllllIIllIIllIll;->IIlllIlllIlll:Ljava/lang/Object;
            check-cast v1, LIIlIlIlIIllIlIII;
            invoke-static {v3, v1}, LlllIlIllllllIl;->IlIlIllllIIllllI(LIlllIIlIlllIlllI;LIIlIlIlIIllIlIII;)LIIlIlIllIIlIlII;
            move-result-object v1
            new-instance v3, LllIIlIIIlIIlI;
            invoke-direct {v3, v1, v0}, LllIIlIIIlIIlI;-><init>(LIlllIIlIlllIlllI;LlIlIlIIIlIlIIl;)V
            invoke-static {}, LIllllIIIllIIllI;->IIllllIIIIIlIIl()LIllllIIIllIIllI;
            move-result-object v0
            iput-object v0, v3, LllIIlIIIlIIlI;->IIlllIlIIlIlII:LIllllIIIllIIllI;
            invoke-virtual {v3}, LllIIlIIIlIIlI;->build()LIlIIIIIIIIlIIlII;
            move-result-object v0
            invoke-virtual {v2, v0}, LIIlIlIIIllIIIIIl;->IllIlIlIIlIllIll(LIlIIIIIIIIlIIlII;)LIIlIlIlIlIllllIl;
            move-result-object v0
            new-instance v1, LlIlIllIllllIIIl;
            const/16 v2, 0xa
            invoke-direct {v1, v2}, LlIlIllIllllIIIl;-><init>(I)V
            invoke-virtual {v0, v1}, LIlIlIllIIllIl;->lIIIIlllIlllll(LlIlllIIlIlllllI;)LIlIlIllIIllIl;
            move-result-object v0
            invoke-virtual {v0, p0}, LIlIlIllIIllIl;->IlllllllllIIIIII(LlIIllIllllIIlIl;)LIIlIlIlIlIllllIl;
            move-result-object v0
            sget-object v1, LIIIIIlIlIll;->IIlllIlllIlll:LIIIIIlIlIll;
            invoke-static {v0, v1, p1}, LIIlIllIIIllll;->lIllIIIIIllIlI(LIIlIIIIllIIlllll;LIIIIIlIlIll;LIlIlIllIlllllI;)Ljava/lang/Object;
            move-result-object v0
            return-object v0
        """))
        request.methods.add(searchMethod("apply", "Ljava/lang/Object;", 12, """
            instance-of v0, p1, LIllllIIllllllllI;
            if-nez v0, :success
            return-object p1
            :success
            check-cast p1, LIllllIIllllllllI;
            iget-object v0, p1, LIllllIIllllllllI;->llIIIIIIlIIIIll:Ljava/lang/Object;
            check-cast v0, Ljava/lang/Iterable;
            invoke-interface {v0}, Ljava/lang/Iterable;->iterator()Ljava/util/Iterator;
            move-result-object v0
            iget-object v1, p0, $SEARCH_REQUEST->IIllllIIIIIlIIl:LIllllllIIIlIlIl;
            iget-object v1, v1, LIllllllIIIlIlIl;->IllIlIlIIlIllIll:Ljava/lang/String;
            :next
            invoke-interface {v0}, Ljava/util/Iterator;->hasNext()Z
            move-result v2
            if-eqz v2, :missing
            invoke-interface {v0}, Ljava/util/Iterator;->next()Ljava/lang/Object;
            move-result-object v2
            check-cast v2, LlIlIlIlIlIIllI;
            iget-object v3, v2, LlllIIllIIllIlII;->IIllllllllIlll:Ljava/lang/String;
            invoke-virtual {v1, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
            move-result v3
            if-eqz v3, :next
            iget-object v0, p0, $SEARCH_REQUEST->lIlIIllIIlI:LlllllIIlIIIlII;
            iget-object v1, v0, LlllllIIlIIIlII;->IllIlIlIIlIllIll:LlIIllllllIlII;
            invoke-virtual {v1, v2}, LlIIllllllIlII;->IIllllIIIIIlIIl(LlllIIllIIllIlII;)LllIlIIIIlllIl;
            move-result-object v1
            iget-object v0, v0, LlllllIIlIIIlII;->llIIIIIIlIIIIll:Landroid/content/Context;
            invoke-static {v0, v2, v1}, LlIIIIIIlllIlIllI;->lIIIIlllIlllll(Landroid/content/Context;LlllIIllIIllIlII;LlIlIlIIIIIll;)V
            new-instance v0, LlllllllIlIlllII;
            const/4 v3, 0x0
            invoke-direct {v0, v1, v3}, LlllllllIlIlllII;-><init>(LlIlIlIIIIIll;Lorg/json/JSONObject;)V
            iget-object v2, v2, LlIlIlIlIlIIllI;->IlIIlIIIllllIIl:Ljava/lang/String;
            if-eqz v2, :mapped
            new-instance v1, Lorg/json/JSONObject;
            invoke-direct {v1, v2}, Lorg/json/JSONObject;-><init>(Ljava/lang/String;)V
            iput-object v1, v0, LlllllllIlIlllII;->IIlllIlllIlll:Lorg/json/JSONObject;
            :mapped
            invoke-static {v0}, LIlIllIllllIIlIIl;->IllIlIlIIlIllIll(LIlIIlllIIlIIllll;)LllllIIIlIIlIlII;
            move-result-object v0
            invoke-static {v0}, Ljava/util/Collections;->singletonList(Ljava/lang/Object;)Ljava/util/List;
            move-result-object v3
            new-instance v2, LIIIlIllIlllIIlI;
            const/4 v4, 0x0
            const/4 v5, 0x0
            const/4 v6, 0x1
            const/4 v7, 0x0
            const/4 v8, 0x0
            invoke-direct/range {v2 .. v8}, LIIIlIllIlllIIlI;-><init>(Ljava/util/List;Ljava/util/List;ZZZI)V
            new-instance v0, LIllllIIllllllllI;
            invoke-direct {v0, v2}, LIllllIIllllllllI;-><init>(Ljava/lang/Object;)V
            return-object v0
            :missing
            new-instance v0, Ljava/util/NoSuchElementException;
            const-string v1, "Selected search track unavailable"
            invoke-direct {v0, v1}, Ljava/util/NoSuchElementException;-><init>(Ljava/lang/String;)V
            throw v0
        """))
        request.methods.single {
            it.name == "IIIIIllIlIIllll" && it.parameterTypes == listOf("LlIIIIIlllIlIlll;")
        }.addInstructionsWithLabels(0, """
            iget-object v0, p0, $SEARCH_REQUEST->IIllllIIIIIlIIl:LIllllllIIIlIlIl;
            iget-object v0, v0, LIllllllIIIlIlIl;->llIIIIIIlIIIIll:LlIlIIlllIIIIIl;
            sget-object v1, LlIlIIlllIIIIIl;->IIlIlllIllllllll:LlIlIIlllIIIIIl;
            if-eq v0, v1, :exact
            sget-object v1, LlIlIIlllIIIIIl;->IlIIIIllIIIllIII:LlIlIIlllIIIIIl;
            if-eq v0, v1, :exact
            sget-object v1, LlIlIIlllIIIIIl;->IIIlIIIllIIlIll:LlIlIIlllIIIIIl;
            if-ne v0, v1, :original
            :exact
            check-cast p1, LIlIlIllIlllllI;
            invoke-virtual {p0, p1}, $SEARCH_REQUEST->morpheFetchSelectedTrack(LIlIlIllIlllllI;)Ljava/lang/Object;
            move-result-object v0
            return-object v0
            :original
            nop
        """.trimIndent())
    }
}
