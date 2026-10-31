package org.blueos.arona

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaPlayer
import android.util.Log
import java.io.File
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.sqrt

/**
 * What Arona (or Plana, 0.7.2) says. Voices are their Japanese lines from the game (bluearchive.wiki
 * Arona/audio, Plana/audio), the emotion sounds are the game's SFX Emoticon Motion; texts are shown in ONE
 * language the user picked. Weather and holiday lines are only used when they match; lines without a game voice
 * play their SFX.
 */
object AronaReplies {
    private const val TAG = "BlueOSArona"
    private const val SFX_DIR = "/product/media/blueos/arona/sfx"

    data class Line(
        val voice: String?, val sfx: String?, val emo: String,
        val ru: String, val en: String, val ja: String, val weather: Boolean = false,
    ) {
        fun text(lang: String) = when (lang) { "en" -> en; "ja" -> ja; else -> ru }
    }

    /** 0.7.2: SIT = she sat down at her desk (the widget, a chip only), SIT_EXIT = tapped while sitting there. */
    enum class Kind { TAP, PAT, WAKE, SLEEP, SLEEP_WAKE, GROW, NOTIFICATION, ANGRY, FALL, SHAKE, SIT, SIT_EXIT }

    private val WEATHER = Line(null, null, "", "", "", "", weather = true)

    private val ARONA: Map<Kind, List<Line>> = mapOf(
        Kind.TAP to listOf(
            Line("Work_Sit_Exit_1", "Surprise", "!", "А?", "Eh?", "あっ？"),
            Line("Work_Sit_Exit_2", "Question", "?", "Хм?", "Huh?", "あれ？"),
            Line("Work_Watch_Exit_1", "Exclaim", "!", "Ой!", "Ah!", "あっ！"),
            Line("Work_In_1", "Respond", "♪", "Сэнсэй! Я вас ждала!", "Sensei! I've been waiting for you!", "先生！お待ちしておりました！"),
            Line("Work_Talk_7_1", "Twinkle", "✦", "Вперёд, Сэнсэй! У вас всё получится!", "You got this, Sensei!", "さあ、頑張りましょう！先生！"),
            Line("Work_Talk_5", "Sweat", "💧", "Берегите здоровье, Сэнсэй, я волнуюсь!", "Take care of your health too!", "先生の健康が心配です！"),
            // 0.7.2: more of her lines from the game
            Line("Work_In_2", "Respond", "♪", "Ну что, пора за работу!", "Let's get to work!", "さあ、お仕事を始める時間です！"),
            Line("Work_In_4", "Twinkle", "✦", "Я помогу вам с любой работой, Сэнсэй!", "I can give you a hand with whatever you want!", "私が先生のお仕事を手伝います！"),
            Line("Work_Talk_4", "Exclaim", "!", "Дел очень много! Но у вас всё получится!", "There's lots of work to do, but I know you can do it!", "解決するべきことがいっぱいですね！頑張りましょう！"),
            Line("Work_Talk_6", "Sweat", "💧", "Ух… Сколько же тут работы.", "Oh… That's a lot of work.", "うわぁ…。ものすごい量の仕事ですね。"),
            Line("Work_Talk_3", "Think", "…", "Столько дел ждёт вас… Взрослым непросто, да?", "Here's everything on your docket. Adults have it rough, huh?", "解決していただかねばならないお仕事がたくさんです。大人って大変ですね。"),
            Line("Academy_Talk_1", "Question", "?", "Какой урок начнём сегодня?", "What class will you be teaching today?", "今日は何の授業を始めますか？"),
            Line("Academy_Talk_5", "Sweat", "💧", "Работа Сэнсэя, оказывается, такая тяжёлая…", "Is teaching supposed to be this hard…?", "先生のお仕事はこんなにハードなものなんですね…。"),
            Line("Work_Watch_Talk_1", "Think", "…", "Интересно, что там, далеко за небом…", "I wonder what's out there…", "遥か空の向こうには、何があるんだろう…。"),
            Line("Work_Watch_Talk_3", "Think", "…", "…Хм-м.", "…Hmm.", "…うーん。"),
            Line("Work_Watch_Exit_2", "Question", "?", "А?", "Huh?", "あれ。"),
            Line("EventArchive_Idle_1", "Think", "…", "Если вспомнить, столько всего было…", "Thinking about it, an awful lot has happened…", "思い返すと色んな事がありましたね……"),
            Line("EventArchive_Idle_2", "Question", "?", "Сэнсэй, а какое воспоминание вам дороже всего?", "What's the most memorable moment for you, Sensei?", "先生はどの思い出が一番印象深いですか？"),
            Line("EventArchive_Idle_3", "Heart", "♥", "Хи-хи. Как вспомнишь — было весело, правда?", "Heehee. It's really nice to reminisce with you!", "ふふ。こうして振り返ってみると、楽しかったですね。"),
            WEATHER),
        Kind.PAT to listOf(
            Line("Work_Sit_In_1", "Music", "♪", "Рун-рун~", "Mm-hmm…", "ルンルン～"),
            Line("Work_Sit_Talk_1", "Music", "♪", "Ла-ла-ла~", "Lalala!", "ランララン、ララ～ン"),
            Line("Work_Sit_Talk_2", "Heart", "♥", "Хм-хм-хм~", "Hmm hmm hmm…", "ふんふんふん～"),
            Line("Work_Sleep_Talk_2", "Shy", "♥", "…хи-хи-хи", "…Heeheehee.", "…うひひひひ。"),
            Line("Work_Sit_Talk_3", "Music", "♪", "Интересно, что же сегодня нас ждёт~♪", "Oh, I wonder what today will bring! ♬", "今日はまたっ、どんなことがっ待ってるのかな～♪")),
        Kind.WAKE to listOf(
            Line("Attendance_Enter_1", "Respond", "♪", "С возвращением, Сэнсэй. Я вас ждала!", "Welcome, Sensei. I've been waiting for you.", "先生、おかえりなさい。お待ちしておりました！"),
            Line("Attendance_Enter_3", "Exclaim", "!", "Сэнсэй! С возвращением!", "Sensei, you're back!", "先生！おかえりなさい！"),
            Line("Attendance_Enter_4", "Twinkle", "✦", "Спасибо за ваш труд сегодня, Сэнсэй!", "Welcome back, Sensei!", "先生、今日もお疲れさまです♪"),
            Line("Attendance_Enter_2", "Respond", "♪", "Сэнсэй, и сегодня рассчитываю на вас!", "Sensei! I'm ready when you are.", "先生、今日もよろしくお願いしますね！"),
            WEATHER, WEATHER),
        Kind.SLEEP to listOf(
            Line("Work_Sleep_In_1", "Zzz", "z", "Мням… клубничное молоко… хи-хи", "Zzz. Strawberry milk… Heeheehee.", "むにゃ、イチゴミルクは…うひひ。"),
            Line("Work_Sleep_Talk_4", "Zzz", "z", "Я вовсе не дремлю… мням", "Me? Doze off? Never… Zzz.", "居眠りしているわけではないですよぉ…むにゃ。"),
            Line("Work_Sleep_Talk_3", "Zzz", "z", "Мням…", "Zzz.", "むにゃ。"),
            Line("Work_Sleep_In_2", "Zzz", "z", "Столько я не съем…", "Eat all that? No, I couldn't…", "そんなに食べられません……。"),
            Line("Work_Sleep_Talk_1", "Zzz", "z", "Ох, Сэнсэй…", "Sensei, you're so…", "先生ったら…。"),
            Line("Work_Sleep_Talk_5", "Zzz", "z", "Хи-хи… ням…", "Heehee. Yummy…", "うひっ、にゃあ…。")),
        Kind.SLEEP_WAKE to listOf(
            Line("Work_Sleep_Exit_1", "Surprise", "!", "А? Что?!", "Wh-What? Huh?!", "んっ、うひゃっ？"),
            Line("Work_Sleep_Exit_3", "Question", "?", "…М?", "…Huh?", "…んぃ？")),
        Kind.GROW to listOf(
            Line("Work_In_1", "Heart", "♥", "Сэнсэй! Я вас ждала!", "Sensei! I've been waiting for you!", "先生！お待ちしておりました！"),
            Line("AttendanceEvent17_Enter_2", "Heart", "♥", "Вот, держите! Мы всегда будем вместе, Сэнсэй!", "This way! Let us always stand together, Sensei!", "こちらをどうぞ！これからもずっと一緒ですよ、先生！"),
            Line("AttendanceEvent18_Enter_2", "Heart", "♥", "Вот, Сэнсэй. Будьте со мной и дальше!", "Here, Sensei. Please stay with me from now on too!", "どうぞ、先生。これからも一緒にいてくださいね！")),
        Kind.NOTIFICATION to listOf(
            Line(null, null, "!", "Сэнсэй, вам уведомление!", "Sensei, you've got a notification!", "先生、お知らせです！")),
        Kind.ANGRY to listOf(
            Line(null, "Steam", "💢", "Сэнсэй, ну хватит!", "Sensei, that's enough!", "もう、先生ったら！")),
        Kind.FALL to listOf(
            Line("Work_Sleep_Exit_1", "Surprise", "!", "А? Что?!", "Wh-What? Huh?!", "んっ、うひゃっ？"),
            Line("Work_Watch_Exit_1", "Sweat", "💧", "Ой!", "Ah!", "あっ！")),
        Kind.SHAKE to listOf(
            Line("Work_Sleep_Exit_2", "Upset", "💢", "Ай?!", "Ah?!", "あひゃっ！？"),
            Line("Work_Sleep_Talk_6", "Angry", "💢", "Сэнсэй, ну ведите себя прилично…", "No, Sensei… You can't do that…", "先生、ちゃんとしてください…")),
        // 0.7.2: at her desk — the game's own lines for that pose
        Kind.SIT to listOf(
            Line("Work_Sit_In_1", "Music", "♪", "Рун-рун~", "Mm-hmm…", "ルンルン～"),
            Line("Work_Sit_Talk_1", "Music", "♪", "Ла-ла-ла~", "Lalala!", "ランララン、ララ～ン"),
            Line("Work_Sit_Talk_2", "Music", "♪", "Хм-хм-хм~", "Hmm hmm hmm…", "ふんふんふん～"),
            Line("Work_Sit_Talk_3", "Music", "♪", "Интересно, что же сегодня нас ждёт~♪", "Oh, I wonder what today will bring! ♬", "今日はまたっ、どんなことがっ待ってるのかな～♪")),
        Kind.SIT_EXIT to listOf(
            Line("Work_Sit_Exit_1", "Surprise", "!", "А?", "Eh?", "あっ？"),
            Line("Work_Sit_Exit_2", "Question", "?", "Хм?", "Huh?", "あれ？"),
            Line("Work_Sit_Exit_3", "Exclaim", "!", "Ой!", "Ah!", "あっ！")),
    )

    // Only when they match the real weather. The game has voices for clear and cloudy only.
    private val ARONA_WEATHER = mapOf(
        Weather.Kind.CLEAR to Line("Work_Watch_In_1", "Twinkle", "☀", "Отлично! Сегодня снова хорошая погода.", "Another day means another clear sky.", "うん、よし！今日も良い天気ですね。"),
        Weather.Kind.CLOUDY to Line("Work_Watch_In_2", "Think", "…", "Хмм… интересно, будет ли дождь.", "Hmm… Maybe it'll rain.", "うーん…雨、降るのかな。"),
        Weather.Kind.RAIN to Line(null, "Sweat", "☔", "Дождь идёт… Не забудьте зонтик, Сэнсэй!", "It's raining… Don't forget your umbrella, Sensei!", "雨が降っていますね…先生、傘を忘れないでください！"),
        Weather.Kind.SNOW to Line(null, "Twinkle", "❄", "Снег! Оденьтесь потеплее, Сэнсэй!", "Snow! Dress warmly, Sensei!", "雪です！先生、暖かくしてくださいね！"),
        Weather.Kind.STORM to Line(null, "Surprise", "⚡", "Гроза… Лучше побыть дома, Сэнсэй.", "A thunderstorm… Better stay inside, Sensei.", "雷です…先生、おうちにいましょう。"),
    )

    // 0.7.2: the game's holiday greetings, on that day only (when she greets Sensei after the screen comes on)
    private val ARONA_HALLOWEEN = listOf(
        Line("AttendanceEvent04_Enter_1", "Heart", "🎃", "С Хэллоуином! С-сладостей у меня нет… но для вас, Сэнсэй, я приготовила особый подарок!", "Happy Halloween! I-I don't have any candy… but I prepared a special present for you, Sensei!", "ハッピー・ハロウィーン！お、お菓子は無いのですが……先生のために、特別なプレゼントをご用意しましたよ！"),
        Line("AttendanceEvent04_Enter_2", "Twinkle", "🎃", "Сладость или гадость! Сейчас как набедокурю~! Ой, но сначала — вот вам подарок, Сэнсэй!", "Trick or treat! I'm gonna play a prank~! Oh, but first, here's a present for you, Sensei!", "トリック・オア・トリート！いたずらしちゃいますよ〜！あ、その前にこちら、先生へのプレゼントです！"))
    private val ARONA_CHRISTMAS = listOf(
        Line("AttendanceEvent05_Enter_1", "Twinkle", "🎄", "С Рождеством! Подарок я тоже приготовила!", "Merry Christmas! I've got a present ready for you too!", "メリークリスマス！プレゼントもちゃーんと用意しましたよ！"),
        Line("AttendanceEvent12_Enter_1", "Heart", "🎄", "С Рождеством, Сэнсэй! Я приготовила подарок!", "Merry Christmas, Sensei! You have a special Christmas gift waiting for you!", "メリークリスマス、先生！プレゼントを用意しました！"))

    // 0.7.2: Plana — calm, precise, a little awkward (her lines from the game's Shittim Chest)
    private val PLANA: Map<Kind, List<Line>> = mapOf(
        Kind.TAP to listOf(
            Line("Work_In_1_2", "Respond", "…", "Сэнсэй. Я вас ждала.", "Sensei. I've been waiting for you.", "先生。お待ちしておりました。"),
            Line("Work_In_2", "Respond", "…", "Пора за работу, Сэнсэй.", "It's time for work, Sensei.", "お仕事を始める時間です。先生。"),
            Line("Work_In_3", "Question", "?", "С какой задачи начнём, Сэнсэй?", "Any task you'd like to start in particular, Sensei?", "どのお仕事から始めますか。先生？"),
            Line("Work_In_4", "Think", "…", "Ожидаю. Накопилось много задач.", "I'm on standby… There are many tasks that need to be addressed.", "待機中。解決しなければならない作業が多数存在しています。"),
            Line("Work_Talk_5", "Think", "…", "Поняла. Сейчас у вас нет срочных дел. Вам скучно.", "Understood. You don't have much to do right now. You're bored.", "理解しました。先生は今特にやるべきことがないのですね。暇なのですね。"),
            Line("Work_PlanaSitPeek_Talk_1", "Question", "?", "…Чувствую на себе взгляд.", "…We're being watched.", "……視線を感じます。"),
            Line("Work_Cabinet_Talk_2", "Think", "…", "Хм… вот как это устроено.", "Hmm… That's how it works.", "ふむ……そういう構造なんですね。"),
            Line("Work_Sit_Exit_1", "Surprise", "!", "А…!", "Oh!", "あ……！"),
            WEATHER),
        Kind.PAT to listOf(
            Line("Work_Sit_In_1", "Shy", "…", "Хм-м…", "Um…", "うーん……。"),
            Line("Work_Sit_Talk_1", "Heart", "…", "……", "……", "……。"),
            Line("Work_PlanaSitPeek_Talk_2", "Shy", "…", "…Это неловко.", "…This is tricky.", "……困ります。"),
            Line("Work_Cabinet_Talk_1", "Heart", "…", "Хм… понятно.", "Hmm, indeed.", "ふむ……なるほど。")),
        Kind.WAKE to listOf(
            Line("Work_In_1_1", "Respond", "✓", "Подключение подтверждено.", "Connection confirmed.", "接続確認。"),
            Line("MultiFloorRaid_Enter_1_2", "Respond", "…", "Добро пожаловать, Сэнсэй.", "Welcome, Sensei.", "ようこそ、先生。"),
            Line("MultiFloorRaid_Enter_2", "Respond", "✓", "Подключение Сэнсэя подтверждено. Рассчитываю на вас.", "Connection process verified. Thank you, Sensei.", "先生の接続プロセスを確認。よろしくお願いします。"),
            Line("MultiFloorRaid_Enter_1_1", "Respond", "✓", "Проверка завершена.", "Verification complete.", "認証完了。"),
            WEATHER),
        Kind.SLEEP to listOf(
            Line(null, "Zzz", "z", "…Zzz", "…Zzz", "……すぅ。")),
        Kind.SLEEP_WAKE to listOf(
            Line("Work_PlanaWatchSky_Exit_1", "Surprise", "!", "…Ах.", "…Oh.", "……あっ。"),
            Line("Work_Cabinet_Exit_1", "Surprise", "!", "…А.", "…Oh.", "……あ。")),
        Kind.GROW to listOf(
            Line("MultiFloorRaid_Idle_2", "Respond", "…", "Ожидаю. Ваши указания, Сэнсэй.", "Standing by. Please issue your commands, Sensei.", "待機中。先生、ご命令を。"),
            Line("MultiFloorRaid_Idle_3", "Twinkle", "✦", "Сейчас я непобедима. Вдвое сильнее обычного.", "I'm twice as strong as usual. I feel invincible.", "今の私は無敵です、普段の2倍は強いです。"),
            Line("MultiFloorRaid_Idle_4", "Respond", "…", "Вместе с Ароной-сэмпай я поддержу вас, Сэнсэй.", "I'll join my power with Arona's to support you, Sensei.", "アロナ先輩と力を合わせて、先生をサポートします。")),
        Kind.NOTIFICATION to listOf(
            Line(null, null, "!", "Сэнсэй, уведомление.", "Sensei, a notification.", "先生、通知です。")),
        Kind.ANGRY to listOf(
            Line("Work_Talk_4", "Steam", "💢", "Ошибка. Непонятное действие. Не тыкайте в меня, пожалуйста. Я сломаюсь.", "I'm confused. I don't understand this behavior… Please don't poke me or I'll break down.", "混乱。理解できない行動です。つつかないでください。故障します。")),
        Kind.FALL to listOf(
            Line("Work_Sit_Exit_1", "Surprise", "!", "А…!", "Oh!", "あ……！")),
        Kind.SHAKE to listOf(
            Line("Work_Talk_4", "Upset", "💢", "Ошибка. Непонятное действие. Не тыкайте в меня, пожалуйста. Я сломаюсь.", "I'm confused. I don't understand this behavior… Please don't poke me or I'll break down.", "混乱。理解できない行動です。つつかないでください。故障します。")),
    )

    private val PLANA_WEATHER = mapOf(
        Weather.Kind.CLEAR to Line(null, "Think", "☀", "Ясно. Хорошая погода.", "Clear skies. Good weather.", "晴れ。良い天気です。"),
        Weather.Kind.CLOUDY to Line(null, "Think", "☁", "Облачно. Возможен дождь.", "Cloudy. Rain is possible.", "曇り。雨の可能性があります。"),
        Weather.Kind.RAIN to Line("Work_Umbrella_In_1", "Think", "☔", "Если пойдёт дождь, то вот это…", "When it rains…", "雨が降ったらこれで……"),
        Weather.Kind.SNOW to Line(null, "Twinkle", "❄", "Снег. Оденьтесь потеплее, Сэнсэй.", "Snow. Please dress warmly, Sensei.", "雪です。先生、暖かくしてください。"),
        Weather.Kind.STORM to Line(null, "Sweat", "⚡", "Гроза. Рекомендую остаться дома.", "A thunderstorm. I recommend staying inside.", "雷です。屋内にいることを推奨します。"),
    )

    fun pick(context: Context, kind: Kind, c: Character = Character.ARONA): Line {
        val cr = context.contentResolver
        val table = if (c == Character.PLANA) PLANA else ARONA
        val all = table[kind] ?: table.getValue(if (kind == Kind.SIT_EXIT) Kind.TAP else Kind.PAT)
        if (kind == Kind.WAKE && c == Character.ARONA) holiday()?.let { if (Math.random() < 0.6) return it.random() }
        var line = all.random()
        if (line.weather) {
            val w = if (BlueOSSettings.on(cr, BlueOSSettings.WEATHER_TALK)) Weather.read(cr)?.takeIf { it.fresh } else null
            val lines = if (c == Character.PLANA) PLANA_WEATHER else ARONA_WEATHER
            line = w?.let { lines[it.kind] } ?: all.filter { !it.weather }.random()
        }
        return line
    }

    private fun holiday(): List<Line>? {
        val now = java.util.Calendar.getInstance()
        val m = now.get(java.util.Calendar.MONTH)
        val d = now.get(java.util.Calendar.DAY_OF_MONTH)
        return when {
            m == java.util.Calendar.OCTOBER && d == 31 -> ARONA_HALLOWEEN
            m == java.util.Calendar.DECEMBER && (d == 24 || d == 25) -> ARONA_CHRISTMAS
            else -> null
        }
    }

    /**
     * 0.7.1: her face for a line — one of the model's expression animations (Arona "00"…"32", Plana "00"…"20",
     * looked through one by one in spine-models/expressions.html), by the line's emotion; null = keep her face.
     */
    fun face(kind: Kind, line: Line, c: Character = Character.ARONA): String? {
        if (c == Character.PLANA) return planaFace(kind, line)
        if (kind == Kind.FALL) return "28"                     // a round, shocked eye
        if (kind == Kind.NOTIFICATION) return "20"
        // 0.7.2: patting is a pleasure — the shy face (blush, sweat, open mouth) looked like horror there
        if (kind == Kind.PAT || kind == Kind.SIT) return if (line.sfx == "Music") "10" else "03"
        return when (line.sfx) {
            "Surprise" -> "02"                                 // wide eyes, «o» mouth
            "Question" -> "14"
            "Exclaim" -> "20"
            "Respond" -> "04"                                  // happy, talking
            "Twinkle" -> "21"                                  // stars in her eyes
            "Sweat" -> "16"                                    // worried
            "Music" -> "10"                                    // eyes shut, singing
            "Heart" -> "03"                                    // eyes shut, blushing smile
            "Shy" -> "19"
            "Steam" -> "12"                                    // >_<
            "Upset" -> "18"
            "Angry" -> "23"                                    // flat, annoyed eyes
            "Think" -> "26"
            else -> null
        }
    }

    /** Plana's faces are quieter: mostly her calm look, a blush, «o», a squint, >_<, spiral eyes. */
    private fun planaFace(kind: Kind, line: Line): String? {
        if (kind == Kind.FALL) return "10"                     // wide open mouth
        if (kind == Kind.NOTIFICATION) return "08"
        if (kind == Kind.PAT) return if (line.sfx == "Shy") "18" else "16"   // blushing / eyes shut, a small smile
        return when (line.sfx) {
            "Surprise" -> "05"                                 // small «o»
            "Question" -> "07"
            "Exclaim" -> "10"
            "Respond" -> "01"
            "Twinkle" -> "08"
            "Sweat" -> "11"
            "Music" -> "17"
            "Heart" -> "16"
            "Shy" -> "18"
            "Steam" -> "14"                                    // >_<
            "Upset" -> "15"
            "Angry" -> "12"                                    // squint
            "Think" -> "02"
            else -> null
        }
    }

    private var player: MediaPlayer? = null

    /** A voice line that started playing: its file (path) and length (her mouth follows it). */
    data class Voice(val name: String, val ms: Int)

    /**
     * Plays the line by the user's choice: voice (or its SFX if the game has no voice for it), emotion SFX, or
     * nothing. Silent when the phone is on vibrate/silent, and never for notifications (the system has a sound).
     * Returns the voice if one is playing now (not for SFX).
     */
    fun play(context: Context, kind: Kind, line: Line, c: Character = Character.ARONA): Voice? {
        if (kind == Kind.NOTIFICATION) return null
        val am = context.getSystemService(AudioManager::class.java)
        if (am.ringerMode != AudioManager.RINGER_MODE_NORMAL) return null
        val file = when (BlueOSSettings.int(context.contentResolver, BlueOSSettings.ARONA_VOICE)) {
            0 -> line.voice?.let { File(c.voiceDir, "$it.ogg") } ?: line.sfx?.let { sfx(it) }
            1 -> line.sfx?.let { sfx(it) }
            else -> null
        } ?: return null
        if (!file.exists()) return null
        return try {
            player?.release()
            val p = MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                setDataSource(file.path)
                setOnCompletionListener { it.release(); if (player === it) player = null }
                prepare()
                start()
            }
            player = p
            if (file.parent == c.voiceDir) Voice(file.path, p.duration) else null
        } catch (e: Exception) {
            Log.w(TAG, "Can't play $file", e)
            null
        }
    }

    // ---------------------------------------------------------------- 0.7.1: loudness of the voices, for her mouth

    /** One loudness value per this many ms. */
    const val ENV_MS = 40
    private val envelopes = ConcurrentHashMap<String, FloatArray>()
    private val warmed = ConcurrentHashMap.newKeySet<Character>()

    /** Loudness 0…1 of a voice file (its path) every [ENV_MS] ms; null until decoded (in the background, [warmUp]). */
    fun envelope(path: String): FloatArray? = envelopes[path]

    /** Decodes every voice of [c] once per process, off the main thread (~50 short files). */
    fun warmUp(c: Character = Character.ARONA) {
        if (!warmed.add(c)) return
        Thread({
            File(c.voiceDir).listFiles()?.forEach { f ->
                try { envelopes[f.path] = decode(f) } catch (e: Exception) { Log.w(TAG, "Can't measure $f", e) }
            }
        }, "BlueOSAronaVoice").start()
    }

    private fun decode(f: File): FloatArray {
        val ex = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            ex.setDataSource(f.path)
            val fmt = ex.getTrackFormat(0)
            ex.selectTrack(0)
            val rate = fmt.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var ch = fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            val c = MediaCodec.createDecoderByType(fmt.getString(MediaFormat.KEY_MIME)!!)
            codec = c
            c.configure(fmt, null, null, 0)
            c.start()
            val win = (rate * ENV_MS / 1000).coerceAtLeast(1)
            val rms = ArrayList<Float>()
            var acc = 0.0
            var n = 0
            val info = MediaCodec.BufferInfo()
            var inDone = false
            var guard = 0
            while (guard++ < 20000) {
                if (!inDone) {
                    val i = c.dequeueInputBuffer(10_000)
                    if (i >= 0) {
                        val size = ex.readSampleData(c.getInputBuffer(i)!!, 0)
                        if (size < 0) { c.queueInputBuffer(i, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM); inDone = true }
                        else { c.queueInputBuffer(i, 0, size, ex.sampleTime, 0); ex.advance() }
                    }
                }
                val o = c.dequeueOutputBuffer(info, 10_000)
                if (o == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) ch = c.outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                if (o < 0) continue
                val pcm = c.getOutputBuffer(o)!!.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                while (pcm.remaining() >= ch) {
                    var s = 0f
                    repeat(ch) { s += pcm.get() / 32768f }
                    s /= ch
                    acc += s * s
                    if (++n == win) { rms += sqrt(acc / n).toFloat(); acc = 0.0; n = 0 }
                }
                c.releaseOutputBuffer(o, false)
                if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
            }
            val peak = rms.maxOrNull()?.takeIf { it > 0f } ?: 1f
            return FloatArray(rms.size) { (rms[it] / peak).coerceIn(0f, 1f) }
        } finally {
            codec?.let { runCatching { it.stop() }; it.release() }
            ex.release()
        }
    }

    private fun sfx(name: String) = File(SFX_DIR, "SFX_Emoticon_Motion_$name.wav")
}
