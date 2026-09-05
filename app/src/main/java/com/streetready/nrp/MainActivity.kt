package com.streetready.nrp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.streetready.nrp.audio.NarrationService
import com.streetready.nrp.data.CourseRepository
import com.streetready.nrp.data.ProgressStore
import com.streetready.nrp.model.*
import kotlin.math.sin
import kotlin.random.Random

private val Ink = Color(0xFF13231F)
private val Teal = Color(0xFF0B7B73)
private val Gold = Color(0xFFD69B2D)
private val Cream = Color(0xFFF5F6F4)
private val Mint = Color(0xFFE7F2EF)

class MainActivity : ComponentActivity() {
    private lateinit var course: Course
    private lateinit var progress: ProgressStore
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        course = CourseRepository(this).load()
        progress = ProgressStore(this)
        if(Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent { StreetReadyApp(course, progress, ::narrate, ::stopNarration) }
    }

    private fun narrate(title:String, text:String) {
        val i=Intent(this, NarrationService::class.java).apply {
            action=NarrationService.ACTION_SPEAK
            putExtra(NarrationService.EXTRA_TITLE,title)
            putExtra(NarrationService.EXTRA_TEXT,text)
        }
        ContextCompat.startForegroundService(this,i)
    }
    private fun stopNarration() {
        startService(Intent(this,NarrationService::class.java).setAction(NarrationService.ACTION_STOP))
    }
}

enum class Tab(val label:String, val glyph:String){ Learn("Learn","▤"), Listen("Listen","▶"), Cases("Cases","⌁"), ECG("ECG","∿"), Practice("Practice","✓") }

@Composable
fun StreetReadyApp(course: Course, progress: ProgressStore, narrate:(String,String)->Unit, stopNarration:()->Unit) {
    var tab by remember { mutableStateOf(Tab.Learn) }
    var openChapter by remember { mutableStateOf<Int?>(null) }
    var openCase by remember { mutableStateOf<ClinicalCase?>(null) }
    MaterialTheme(colorScheme = lightColorScheme(primary=Teal, secondary=Gold, background=Cream, surface=Color.White)) {
        Scaffold(
            containerColor=Cream,
            bottomBar={
                NavigationBar(containerColor=Color.White) {
                    Tab.entries.forEach { t ->
                        NavigationBarItem(selected=tab==t, onClick={tab=t; openChapter=null; openCase=null}, icon={Text(t.glyph,fontWeight=FontWeight.Black)}, label={Text(t.label,fontSize=11.sp)})
                    }
                }
            }
        ){ pad ->
            Box(Modifier.padding(pad).fillMaxSize()) {
                when {
                    openChapter!=null -> LessonPlayer(course, openChapter!!, progress, narrate, stopNarration, onBack={openChapter=null}, onNextChapter={ n -> openChapter=n })
                    openCase!=null -> CasePlayer(openCase!!, onBack={openCase=null})
                    tab==Tab.Learn -> LearnScreen(course,progress,onOpen={openChapter=it})
                    tab==Tab.Listen -> ListenScreen(course,progress,onPlay={ ch ->
                        val cards=course.cards.filter{it.chapter==ch.number}.sortedBy{it.order}
                        narrate("Chapter ${ch.number}: ${ch.title}", cards.joinToString(". "){it.narration(ch.title)})
                    }, onOpen={openChapter=it})
                    tab==Tab.Cases -> CasesScreen(course){openCase=it}
                    tab==Tab.ECG -> EcgScreen(course,progress)
                    tab==Tab.Practice -> PracticeScreen(course,progress)
                }
            }
        }
    }
}

@Composable
fun Header(kicker:String,title:String,sub:String){
    Column(Modifier.fillMaxWidth().padding(20.dp,18.dp,20.dp,10.dp)){
        Text(kicker.uppercase(),fontSize=11.sp,fontWeight=FontWeight.Bold,color=Teal,letterSpacing=1.sp)
        Text(title,fontSize=30.sp,fontWeight=FontWeight.Black,color=Ink)
        Text(sub,fontSize=14.sp,color=Ink.copy(alpha=.65f),modifier=Modifier.padding(top=4.dp))
    }
}

@Composable
fun LearnScreen(course:Course,progress:ProgressStore,onOpen:(Int)->Unit){
    var query by remember { mutableStateOf("") }
    val completed=progress.completed()
    val list=course.chapters.filter{query.isBlank() || it.title.contains(query,true) || it.track.contains(query,true)}
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(bottom=20.dp)){
        item { Header("StreetReady NRP","Paramedic Academy","Learn the medicine first. Practice comes after understanding.") }
        item {
            Row(Modifier.padding(horizontal=20.dp).fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
                Metric("Course","${completed.size}/53",Modifier.weight(1f)); Metric("Teaching cards","636",Modifier.weight(1f)); Metric("Clinical cases","40",Modifier.weight(1f))
            }
            OutlinedTextField(query,{query=it},label={Text("Search chapters")},singleLine=true,modifier=Modifier.fillMaxWidth().padding(20.dp,14.dp,20.dp,8.dp))
        }
        items(list,key={it.id}) { ch ->
            ChapterCard(ch, ch.number in completed, progress.cardIndex(ch.number), onOpen)
        }
    }
}

@Composable fun Metric(label:String,value:String,modifier:Modifier=Modifier){
    Surface(modifier,shape=RoundedCornerShape(18.dp),color=Color.White){Column(Modifier.padding(14.dp)){Text(label,fontSize=11.sp,color=Ink.copy(alpha=.6f));Text(value,fontSize=23.sp,fontWeight=FontWeight.Black,color=Ink)}}
}

@Composable
fun ChapterCard(ch:Chapter,done:Boolean,index:Int,onOpen:(Int)->Unit){
    Surface(Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=6.dp).clickable{onOpen(ch.number)},shape=RoundedCornerShape(22.dp),color=Color.White,shadowElevation=1.dp){
        Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically){
            Box(Modifier.size(48.dp).background(if(done) Teal else Mint,RoundedCornerShape(14.dp)),contentAlignment=Alignment.Center){Text(if(done)"✓" else ch.number.toString(),fontWeight=FontWeight.Black,color=if(done) Color.White else Teal)}
            Column(Modifier.weight(1f).padding(horizontal=14.dp)){Text(ch.track.uppercase(),fontSize=10.sp,color=Teal,fontWeight=FontWeight.Bold);Text(ch.title,fontSize=17.sp,fontWeight=FontWeight.Bold,color=Ink);Text(if(done)"Completed" else "Card ${index+1} of 12 • ~${ch.minutes*2} min",fontSize=12.sp,color=Ink.copy(alpha=.55f))}
            Text("›",fontSize=30.sp,color=Ink.copy(alpha=.3f))
        }
    }
}

@Composable
fun LessonPlayer(course:Course,chapterNum:Int,progress:ProgressStore,narrate:(String,String)->Unit,stopNarration:()->Unit,onBack:()->Unit,onNextChapter:(Int)->Unit){
    val ch=course.chapters.first{it.number==chapterNum}
    val cards=course.cards.filter{it.chapter==chapterNum}.sortedBy{it.order}
    var idx by remember(chapterNum){ mutableIntStateOf(progress.cardIndex(chapterNum).coerceIn(0,cards.lastIndex)) }
    val card=cards[idx]
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(bottom=28.dp)){
        item{
            Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically){TextButton(onClick={stopNarration();onBack()}){Text("← Course")};Spacer(Modifier.weight(1f));Text("${idx+1} / ${cards.size}",fontWeight=FontWeight.Bold,color=Ink.copy(alpha=.65f))}
            LinearProgressIndicator(progress={ (idx+1f)/cards.size },modifier=Modifier.fillMaxWidth().height(4.dp),color=Teal,trackColor=Mint)
        }
        item{
            Column(Modifier.padding(20.dp)){
                Text("CHAPTER ${ch.number} • ${card.kicker.uppercase()}",fontSize=11.sp,fontWeight=FontWeight.Bold,color=Teal)
                Text(card.title,fontSize=31.sp,lineHeight=35.sp,fontWeight=FontWeight.Black,color=Ink,modifier=Modifier.padding(top=5.dp,bottom=16.dp))
                TeachingIllustration(card.visual)
                Text(card.body,fontSize=19.sp,lineHeight=29.sp,color=Ink,modifier=Modifier.padding(top=20.dp,bottom=12.dp))
                card.bullets.forEach{b->Row(Modifier.padding(vertical=6.dp)){Text("•",color=Teal,fontWeight=FontWeight.Black);Text(b,fontSize=16.sp,lineHeight=23.sp,color=Ink,modifier=Modifier.padding(start=9.dp))}}
                Surface(Modifier.fillMaxWidth().padding(top=14.dp),shape=RoundedCornerShape(18.dp),color=Mint){Column(Modifier.padding(16.dp)){Text("FIELD CONNECTION",fontSize=10.sp,fontWeight=FontWeight.Bold,color=Teal);Text(card.callout,fontSize=15.sp,lineHeight=22.sp,color=Ink,modifier=Modifier.padding(top=5.dp))}}
                Row(Modifier.fillMaxWidth().padding(top=18.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    Button(onClick={narrate("Chapter ${ch.number}: ${ch.title}",card.narration(ch.title))},modifier=Modifier.weight(1f)){Text("🔊 Read card")}
                    OutlinedButton(onClick={narrate("Chapter ${ch.number}: ${ch.title}",cards.drop(idx).joinToString(". "){it.narration(ch.title)})},modifier=Modifier.weight(1f)){Text("▶ Continue audio")}
                }
                Text("Medication, procedure, and destination decisions must follow current local protocol/medical direction.",fontSize=11.sp,color=Ink.copy(alpha=.45f),modifier=Modifier.padding(top=12.dp))
            }
        }
        item{
            Row(Modifier.fillMaxWidth().padding(horizontal=20.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)){
                OutlinedButton(onClick={if(idx>0){idx--;progress.setCardIndex(chapterNum,idx)} else onBack()},modifier=Modifier.weight(1f)){Text(if(idx>0)"← Back" else "Course")}
                Button(onClick={
                    if(idx<cards.lastIndex){idx++;progress.setCardIndex(chapterNum,idx)} else {progress.markCompleted(chapterNum); if(chapterNum<53) onNextChapter(chapterNum+1) else onBack()}
                },modifier=Modifier.weight(1f)){Text(if(idx<cards.lastIndex)"Continue →" else if(chapterNum<53)"Complete & next" else "Complete course")}
            }
        }
    }
}

@Composable
fun TeachingIllustration(kind:String){
    Surface(Modifier.fillMaxWidth().height(220.dp),shape=RoundedCornerShape(24.dp),color=Color(0xFF102B29)){
        Canvas(Modifier.fillMaxSize().padding(20.dp)){
            val w=size.width; val h=size.height
            drawCircle(Teal.copy(alpha=.35f),radius=h*.32f,center=Offset(w*.5f,h*.52f))
            when(kind.lowercase()){
                "airway","ventilation","respiratory" -> {
                    drawLine(Color.White,Offset(w*.5f,h*.15f),Offset(w*.5f,h*.55f),10f,StrokeCap.Round)
                    drawOval(Color.White.copy(alpha=.12f),Offset(w*.18f,h*.42f),androidx.compose.ui.geometry.Size(w*.28f,h*.42f),style=Stroke(5f))
                    drawOval(Color.White.copy(alpha=.12f),Offset(w*.54f,h*.42f),androidx.compose.ui.geometry.Size(w*.28f,h*.42f),style=Stroke(5f))
                    drawLine(Gold,Offset(w*.5f,h*.55f),Offset(w*.35f,h*.72f),7f);drawLine(Gold,Offset(w*.5f,h*.55f),Offset(w*.65f,h*.72f),7f)
                }
                "cardiac","ecg","circulation","shock" -> {
                    val p=Path();p.moveTo(w*.14f,h*.55f);p.lineTo(w*.30f,h*.55f);p.lineTo(w*.37f,h*.38f);p.lineTo(w*.43f,h*.73f);p.lineTo(w*.50f,h*.48f);p.lineTo(w*.58f,h*.55f);p.lineTo(w*.86f,h*.55f);drawPath(p,Gold,style=Stroke(7f,cap=StrokeCap.Round))
                }
                "neuro","nervous" -> {
                    drawCircle(Color.White.copy(alpha=.16f),h*.27f,Offset(w*.5f,h*.42f)); for(i in 0..7){val x=w*(.25f+i*.07f);drawLine(Gold,Offset(w*.5f,h*.52f),Offset(x,h*.8f),4f)}
                }
                else -> {
                    for(i in 0..4){drawCircle(if(i%2==0)Gold else Color.White.copy(alpha=.7f),18f,Offset(w*(.18f+i*.16f),h*(.35f+(i%2)*.26f)));if(i<4)drawLine(Teal.copy(alpha=.9f),Offset(w*(.18f+i*.16f)+18,h*(.35f+(i%2)*.26f)),Offset(w*(.18f+(i+1)*.16f)-18,h*(.35f+((i+1)%2)*.26f)),5f)}
                }
            }
        }
    }
}

@Composable
fun ListenScreen(course:Course,progress:ProgressStore,onPlay:(Chapter)->Unit,onOpen:(Int)->Unit){
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(bottom=20.dp)){
        item{Header("Background narration","Listen to the course","Native Android narration continues with the screen off and exposes notification controls.")}
        items(course.chapters,key={it.id}){ch->
            Surface(Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=5.dp),shape=RoundedCornerShape(18.dp),color=Color.White){Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){
                Text(ch.number.toString(),fontWeight=FontWeight.Black,color=Teal,modifier=Modifier.width(35.dp));Column(Modifier.weight(1f)){Text(ch.title,fontWeight=FontWeight.Bold,color=Ink);Text("12 teaching cards • offline",fontSize=12.sp,color=Ink.copy(alpha=.55f))};TextButton(onClick={onOpen(ch.number)}){Text("Read")};Button(onClick={onPlay(ch)}){Text("▶")}
            }}
        }
    }
}

@Composable
fun CasesScreen(course:Course,onOpen:(ClinicalCase)->Unit){
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(bottom=20.dp)){
        item{Header("Clinical Judgment","40 field cases","Reveal the reasoning path one step at a time instead of jumping straight to an answer.")}
        items(course.cases,key={it.id}){c->
            Surface(Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=6.dp).clickable{onOpen(c)},shape=RoundedCornerShape(20.dp),color=Color.White){Column(Modifier.padding(16.dp)){Text("${c.phase.uppercase()} • CH ${c.chapter}",fontSize=10.sp,fontWeight=FontWeight.Bold,color=Teal);Text(c.title,fontSize=19.sp,fontWeight=FontWeight.Bold,color=Ink,modifier=Modifier.padding(top=4.dp));Text(c.scene,maxLines=3,overflow=TextOverflow.Ellipsis,fontSize=14.sp,color=Ink.copy(alpha=.7f),modifier=Modifier.padding(top=7.dp));Text("Work through case →",color=Teal,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=10.dp))}}
        }
    }
}

@Composable
fun CasePlayer(c:ClinicalCase,onBack:()->Unit){
    var reveal by remember(c.id){mutableIntStateOf(0)}
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp)){
        item{TextButton(onClick=onBack){Text("← Cases")};Text(c.phase.uppercase(),fontSize=11.sp,fontWeight=FontWeight.Bold,color=Teal);Text(c.title,fontSize=30.sp,fontWeight=FontWeight.Black,color=Ink);Surface(Modifier.fillMaxWidth().padding(vertical=15.dp),shape=RoundedCornerShape(20.dp),color=Mint){Column(Modifier.padding(16.dp)){Text(c.scene,fontSize=16.sp,lineHeight=23.sp,color=Ink);Text(c.vitals,fontWeight=FontWeight.Bold,color=Teal,modifier=Modifier.padding(top=10.dp))}}}
        items(c.steps.take(reveal)){s->Surface(Modifier.fillMaxWidth().padding(vertical=5.dp),shape=RoundedCornerShape(18.dp),color=Color.White){Column(Modifier.padding(15.dp)){Text(s.first,fontWeight=FontWeight.Bold,color=Ink);Text(s.second,fontSize=14.sp,lineHeight=21.sp,color=Ink.copy(alpha=.72f),modifier=Modifier.padding(top=4.dp))}}}
        item{if(reveal<c.steps.size) Button(onClick={reveal++},modifier=Modifier.fillMaxWidth().padding(top=10.dp)){Text("Reveal ${reveal+1}: ${c.steps[reveal].first}")} else Text("Case complete. Revisit Chapter ${c.chapter} if any reasoning step felt uncertain.",color=Teal,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=15.dp))}
    }
}

@Composable
fun EcgScreen(course:Course,progress:ProgressStore){
    var practice by remember{mutableStateOf(false)}; var target by remember{mutableStateOf(course.ecgs.random())}; var selected by remember{mutableStateOf<EcgPattern?>(null)}
    val stats=progress.ecgStats()
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(bottom=25.dp)){
        item{Header("ECG Lab","48 rhythm & ischemia patterns","Learn the pattern, then identify it from a simplified teaching strip.");Row(Modifier.padding(horizontal=20.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick={practice=false}){Text("Learn")};OutlinedButton(onClick={practice=true;target=course.ecgs.random();selected=null}){Text("Identify")};Text("${stats.first}/${stats.second}",modifier=Modifier.padding(12.dp),fontWeight=FontWeight.Bold,color=Ink)}}
        if(!practice){items(course.ecgs){e->Surface(Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=6.dp),shape=RoundedCornerShape(20.dp),color=Color.White){Column(Modifier.padding(15.dp)){Text(e.name,fontSize=18.sp,fontWeight=FontWeight.Bold,color=Ink);EcgStrip(e.type);Text(e.criteria,fontSize=14.sp,color=Ink.copy(alpha=.75f));Text(e.pearl,fontSize=13.sp,color=Teal,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=6.dp))}}}}
        else item{
            Surface(Modifier.fillMaxWidth().padding(20.dp),shape=RoundedCornerShape(22.dp),color=Color.White){Column(Modifier.padding(16.dp)){Text("Identify this pattern",fontSize=22.sp,fontWeight=FontWeight.Black,color=Ink);EcgStrip(target.type);course.ecgs.shuffled().take(8).let{opts->(if(target in opts)opts else listOf(target)+opts.take(7)).shuffled()}.forEach{e->OutlinedButton(onClick={if(selected==null){selected=e;progress.addEcg(e==target)}},modifier=Modifier.fillMaxWidth().padding(vertical=3.dp)){Text(e.name)}};selected?.let{Text(if(it==target)"Correct — ${target.criteria}" else "This is ${target.name}. ${target.criteria}",color=if(it==target)Teal else MaterialTheme.colorScheme.error,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=10.dp));Button(onClick={target=course.ecgs.random();selected=null},modifier=Modifier.fillMaxWidth().padding(top=8.dp)){Text("Next pattern")}}}}
        }
    }
}

@Composable
fun EcgStrip(type:String){
    Canvas(Modifier.fillMaxWidth().height(145.dp).padding(vertical=10.dp)){
        for(x in 0..size.width.toInt() step 20) drawLine(Color(0x22CC5555),Offset(x.toFloat(),0f),Offset(x.toFloat(),size.height),1f)
        for(y in 0..size.height.toInt() step 20) drawLine(Color(0x22CC5555),Offset(0f,y.toFloat()),Offset(size.width,y.toFloat()),1f)
        val p=Path(); val mid=size.height*.52f; p.moveTo(0f,mid)
        val chaotic=type.contains("vf",true)||type=="artifact"||type=="torsades"
        if(chaotic){for(x in 0..size.width.toInt() step 5){val amp=if(type.contains("fine"))8f else 25f;val y=mid+(sin(x*.13)*amp+sin(x*.047)*amp*.55).toFloat();p.lineTo(x.toFloat(),y)}} else {
            var x=10f; val cycle=if(type.contains("tach",true)||type=="svt"||type=="vtach")65f else if(type.contains("brady",true)||type=="av3"||type=="ivr")135f else 100f
            while(x<size.width-55){p.lineTo(x,mid);p.lineTo(x+14,mid);p.lineTo(x+20,mid-9);p.lineTo(x+27,mid);p.lineTo(x+35,mid+8);p.lineTo(x+40,mid-45);p.lineTo(x+48,mid+55);p.lineTo(x+55,mid);p.lineTo(x+75,mid-16);p.lineTo(x+92,mid);x+=cycle}
        }
        drawPath(p,Ink,style=Stroke(3.4f,cap=StrokeCap.Round))
    }
}

@Composable
fun PracticeScreen(course:Course,progress:ProgressStore){
    var active by remember{mutableStateOf(false)}; var quiz by remember{mutableStateOf(emptyList<Question>())}; var idx by remember{mutableIntStateOf(0)};var answer by remember{mutableStateOf<Set<Int>>(emptySet())};var locked by remember{mutableStateOf(false)}
    val stats=progress.questionStats()
    if(!active){LazyColumn(Modifier.fillMaxSize()){item{Header("Reinforcement","424 original questions","Use practice to reveal what needs another lesson—not as the main course.");Surface(Modifier.fillMaxWidth().padding(20.dp),shape=RoundedCornerShape(22.dp),color=Color.White){Column(Modifier.padding(18.dp)){Text("Lifetime accuracy",color=Ink.copy(alpha=.6f));Text(if(stats.second==0)"—" else "${(stats.first*100/stats.second)}%",fontSize=40.sp,fontWeight=FontWeight.Black,color=Ink);Button(onClick={quiz=course.questions.shuffled().take(20);idx=0;answer=emptySet();locked=false;active=true},modifier=Modifier.fillMaxWidth().padding(top=15.dp)){Text("Start 20-item mixed practice")};Text("The live National Registry exam blueprint should always be checked before exam day; this bank is educational reinforcement, not an official NREMT item bank.",fontSize=11.sp,color=Ink.copy(alpha=.5f),modifier=Modifier.padding(top=12.dp))}}}}
    } else {
        val q=quiz[idx]
        LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp)){
            item{TextButton(onClick={active=false}){Text("← Exit")};Text("${idx+1} / ${quiz.size} • Chapter ${q.chapter}",fontSize=12.sp,fontWeight=FontWeight.Bold,color=Teal);Text(q.text,fontSize=23.sp,lineHeight=31.sp,fontWeight=FontWeight.Black,color=Ink,modifier=Modifier.padding(vertical=15.dp));if(q.type=="multi")Text("Select all that apply.",color=Ink.copy(alpha=.55f));q.choices.forEachIndexed{i,c->val picked=i in answer;Surface(Modifier.fillMaxWidth().padding(vertical=4.dp).clickable(enabled=!locked){answer=if(q.type=="single")setOf(i) else if(picked)answer-i else answer+i},shape=RoundedCornerShape(16.dp),color=if(picked)Mint else Color.White){Text(c,Modifier.padding(15.dp),color=Ink)}};if(!locked)Button(onClick={if(answer.isNotEmpty()){locked=true;val ok=answer.sorted()==q.correct.sorted();progress.addQuestion(ok)}},modifier=Modifier.fillMaxWidth().padding(top=12.dp)){Text("Check answer")}else{val ok=answer.sorted()==q.correct.sorted();Text(if(ok)"Correct" else "Review the reasoning",fontSize=18.sp,fontWeight=FontWeight.Bold,color=if(ok)Teal else MaterialTheme.colorScheme.error,modifier=Modifier.padding(top=16.dp));Text(q.explanation,color=Ink.copy(alpha=.75f),modifier=Modifier.padding(top=6.dp));Button(onClick={if(idx<quiz.lastIndex){idx++;answer=emptySet();locked=false}else active=false},modifier=Modifier.fillMaxWidth().padding(top=12.dp)){Text(if(idx<quiz.lastIndex)"Next" else "Finish")}}}
        }
    }
}
