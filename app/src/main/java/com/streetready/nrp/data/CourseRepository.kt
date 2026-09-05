
package com.streetready.nrp.data

import android.content.Context
import com.streetready.nrp.model.*
import org.json.JSONArray
import org.json.JSONObject

class CourseRepository(private val context: Context) {
    fun load(): Course {
        val raw = context.assets.open("course.json").bufferedReader().use { it.readText() }
        val root = JSONObject(raw)
        return Course(
            chapters = root.getJSONArray("chapters").mapObjects { j ->
                Chapter(
                    id=j.getString("id"), number=j.getInt("number"), title=j.getString("title"),
                    track=j.getString("track"), domain=j.getString("domain"), visual=j.getString("visual"),
                    minutes=j.getInt("minutes"), hook=j.getString("hook"), concept=j.getString("concept"),
                    cues=j.getJSONArray("cues").strings(), priorities=j.getJSONArray("priorities").strings(),
                    pitfalls=j.getJSONArray("pitfalls").strings(), prompts=j.getJSONArray("prompts").strings()
                )
            },
            cards = root.getJSONArray("lessonCards").mapObjects { j ->
                LessonCard(j.getString("id"),j.getInt("chapter"),j.getInt("order"),j.getString("kicker"),
                    j.getString("title"),j.getString("body"),j.getJSONArray("bullets").strings(),
                    j.getString("callout"),j.getString("visual"))
            },
            questions = root.getJSONArray("questions").mapObjects { j ->
                Question(j.getString("id"),j.getString("type"),j.getInt("chapter"),j.getString("domain"),
                    j.getString("text"),j.getJSONArray("choices").strings(),j.getJSONArray("correct").ints(),
                    j.getString("explanation"))
            },
            cases = root.getJSONArray("cases").mapObjects { j ->
                val steps = j.getJSONArray("steps").let { a ->
                    (0 until a.length()).map { idx ->
                        val s=a.getJSONArray(idx); s.getString(0) to s.getString(1)
                    }
                }
                ClinicalCase(j.getString("id"),j.getString("title"),j.getString("domain"),j.getInt("chapter"),
                    j.getString("phase"),j.getString("scene"),j.getString("vitals"),steps)
            },
            ecgs = root.getJSONArray("ecgs").mapObjects { j ->
                EcgPattern(j.getString("name"),j.getString("type"),j.getInt("rate"),
                    j.getString("criteria"),j.getString("pearl"))
            }
        )
    }
}
private fun JSONArray.strings()=(0 until length()).map{getString(it)}
private fun JSONArray.ints()=(0 until length()).map{getInt(it)}
private fun <T> JSONArray.mapObjects(block:(JSONObject)->T)=(0 until length()).map{block(getJSONObject(it))}
