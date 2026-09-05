package com.streetready.nrp.data

import android.content.Context

class ProgressStore(context: Context) {
    private val prefs = context.getSharedPreferences("streetready_progress", Context.MODE_PRIVATE)
    fun completed(): Set<Int> = prefs.getStringSet("completed", emptySet())!!.mapNotNull { it.toIntOrNull() }.toSet()
    fun markCompleted(chapter: Int) {
        val next = completed().toMutableSet().apply { add(chapter) }.map { it.toString() }.toSet()
        prefs.edit().putStringSet("completed", next).apply()
    }
    fun cardIndex(chapter: Int): Int = prefs.getInt("card_$chapter", 0)
    fun setCardIndex(chapter: Int, index: Int) = prefs.edit().putInt("card_$chapter", index).apply()
    fun addQuestion(correct: Boolean) {
        val total=prefs.getInt("q_total",0)+1
        val good=prefs.getInt("q_correct",0)+(if(correct)1 else 0)
        prefs.edit().putInt("q_total",total).putInt("q_correct",good).apply()
    }
    fun questionStats(): Pair<Int,Int> = prefs.getInt("q_correct",0) to prefs.getInt("q_total",0)
    fun addEcg(correct: Boolean) {
        val total=prefs.getInt("e_total",0)+1
        val good=prefs.getInt("e_correct",0)+(if(correct)1 else 0)
        prefs.edit().putInt("e_total",total).putInt("e_correct",good).apply()
    }
    fun ecgStats(): Pair<Int,Int> = prefs.getInt("e_correct",0) to prefs.getInt("e_total",0)
    fun reset() = prefs.edit().clear().apply()
}
