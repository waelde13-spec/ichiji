#!/usr/bin/env python3
"""Generates app/src/main/assets/curriculum.json and strokes.json.

Usage: python3 tools/build_data.py /path/to/kanjivg/kanji

Stroke data comes from KanjiVG (http://kanjivg.tagaini.net),
Copyright (C) Ulrich Apel, licensed CC BY-SA 3.0.
"""
import json, os, re, sys

HIRA = [
    ("あ","a","あめ","ame","rain"),("い","i","いぬ","inu","dog"),("う","u","うみ","umi","sea"),
    ("え","e","えき","eki","station"),("お","o","おと","oto","sound"),
    ("か","ka","かさ","kasa","umbrella"),("き","ki","きた","kita","north"),("く","ku","くも","kumo","cloud"),
    ("け","ke","けさ","kesa","this morning"),("こ","ko","こえ","koe","voice"),
    ("さ","sa","さかな","sakana","fish"),("し","shi","しお","shio","salt"),("す","su","すし","sushi","sushi"),
    ("せ","se","せかい","sekai","world"),("そ","so","そら","sora","sky"),
    ("た","ta","たこ","tako","octopus"),("ち","chi","ちかい","chikai","near"),("つ","tsu","つき","tsuki","moon"),
    ("て","te","て","te","hand"),("と","to","とり","tori","bird"),
    ("な","na","なつ","natsu","summer"),("に","ni","にく","niku","meat"),("ぬ","nu","ぬの","nuno","cloth"),
    ("ね","ne","ねこ","neko","cat"),("の","no","のり","nori","seaweed"),
    ("は","ha","はな","hana","flower"),("ひ","hi","ひと","hito","person"),("ふ","fu","ふね","fune","boat"),
    ("へ","he","へや","heya","room"),("ほ","ho","ほし","hoshi","star"),
    ("ま","ma","まど","mado","window"),("み","mi","みみ","mimi","ear"),("む","mu","むし","mushi","insect"),
    ("め","me","め","me","eye"),("も","mo","もり","mori","forest"),
    ("や","ya","やま","yama","mountain"),("ゆ","yu","ゆき","yuki","snow"),("よ","yo","よる","yoru","night"),
    ("ら","ra","さくら","sakura","cherry blossom"),("り","ri","りす","risu","squirrel"),("る","ru","はる","haru","spring"),
    ("れ","re","これ","kore","this"),("ろ","ro","いろ","iro","color"),
    ("わ","wa","わたし","watashi","I, me"),("を","o","みずを のむ","mizu o nomu","drink water (を marks the object)"),
    ("ん","n","ほん","hon","book"),
]
KATA = [
    ("ア","a","アイス","aisu","ice cream"),("イ","i","インク","inku","ink"),("ウ","u","ウール","ūru","wool"),
    ("エ","e","エアコン","eakon","air conditioner"),("オ","o","オレンジ","orenji","orange"),
    ("カ","ka","カメラ","kamera","camera"),("キ","ki","キロ","kiro","kilo"),("ク","ku","クラス","kurasu","class"),
    ("ケ","ke","ケーキ","kēki","cake"),("コ","ko","コーヒー","kōhī","coffee"),
    ("サ","sa","サラダ","sarada","salad"),("シ","shi","シャツ","shatsu","shirt"),("ス","su","スープ","sūpu","soup"),
    ("セ","se","セーター","sētā","sweater"),("ソ","so","ソファ","sofa","sofa"),
    ("タ","ta","タクシー","takushī","taxi"),("チ","chi","チーズ","chīzu","cheese"),("ツ","tsu","ツアー","tsuā","tour"),
    ("テ","te","テレビ","terebi","television"),("ト","to","トマト","tomato","tomato"),
    ("ナ","na","ナイフ","naifu","knife"),("ニ","ni","テニス","tenisu","tennis"),("ヌ","nu","カヌー","kanū","canoe"),
    ("ネ","ne","ネクタイ","nekutai","necktie"),("ノ","no","ノート","nōto","notebook"),
    ("ハ","ha","ハム","hamu","ham"),("ヒ","hi","ヒーター","hītā","heater"),("フ","fu","フォーク","fōku","fork"),
    ("ヘ","he","ヘリコプター","herikoputā","helicopter"),("ホ","ho","ホテル","hoteru","hotel"),
    ("マ","ma","マスク","masuku","mask"),("ミ","mi","ミルク","miruku","milk"),("ム","mu","ゲーム","gēmu","game"),
    ("メ","me","メニュー","menyū","menu"),("モ","mo","メモ","memo","memo"),
    ("ヤ","ya","タイヤ","taiya","tire"),("ユ","yu","ユーモア","yūmoa","humor"),("ヨ","yo","ヨーグルト","yōguruto","yogurt"),
    ("ラ","ra","ラジオ","rajio","radio"),("リ","ri","リモコン","rimokon","remote control"),("ル","ru","ルール","rūru","rule"),
    ("レ","re","レモン","remon","lemon"),("ロ","ro","ロボット","robotto","robot"),
    ("ワ","wa","ワイン","wain","wine"),("ヲ","o","ヲ","o","rare: kept for stylized text"),
    ("ン","n","パン","pan","bread"),
]
HIRA_NOTES = {"を": "Pronounced o. Used almost only as the object particle.",
              "ん": "The only kana that is a consonant on its own.",
              "し": "Not si: the s softens to shi.", "ち": "Not ti: say chi.",
              "つ": "Not tu: say tsu.", "ふ": "Halfway between f and h, lips barely touching."}
KATA_NOTES = {"シ": "Strokes rise from the bottom left. Compare ツ, whose strokes fall from the top.",
              "ツ": "Strokes fall from the top. Compare シ.",
              "ソ": "The long stroke falls from the top right. Compare ン.",
              "ン": "The long stroke rises from the bottom left. Compare ソ.",
              "ヲ": "Almost never used. Learn to recognize it and move on."}

RULES = [
    ("が","g-row","ga gi gu ge go","Two dots (dakuten) on the か row make a g sound.","ごはん","gohan","rice, a meal","が"),
    ("ざ","z-row","za ji zu ze zo","Dakuten on the さ row: s becomes z. じ is read ji.","かぜ","kaze","wind","ざ"),
    ("だ","d-row","da ji zu de do","Dakuten on the た row: t becomes d. ぢ and づ are rare.","でんわ","denwa","telephone","だ"),
    ("ば","b-row","ba bi bu be bo","Dakuten on the は row: h becomes b.","かばん","kaban","bag","ば"),
    ("ぱ","p-row","pa pi pu pe po","A small circle (handakuten) on the は row: h becomes p.","さんぽ","sanpo","a walk","ぱ"),
    ("パ","katakana marks","ga, za, da, ba, pa…","Katakana take the same marks: カ→ガ, ハ→バ→パ.","パン","pan","bread","パ"),
    ("っ","small tsu","(pause)","A small っ doubles the next consonant: a short stop before it.","きって","kitte","stamp","っ"),
    ("ッ","small tsu (katakana)","(pause)","Same rule in katakana. Common in loanwords.","ロボット","robotto","robot","ッ"),
    ("きゃ","small ya yu yo","kya kyu kyo","A small ゃ ゅ ょ after an i-column kana blends into one sound.","きょう","kyō","today","ゃ"),
    ("しゃ","sh, ch, j combos","sha shu sho · cha chu cho · ja ju jo","し, ち and じ blend with small ゃ ゅ ょ.","しゃしん","shashin","photograph","ゅ"),
    ("おう","long vowels","ā ī ū ē ō","Hiragana stretch a vowel by writing it again. おう and えい are read ō and ē.","おかあさん","okāsan","mother",None),
    ("ー","long mark","(stretch)","Katakana stretch a vowel with a dash.","コーヒー","kōhī","coffee","ー"),
    ("は","particles","wa · o · e","As particles, は is read wa, を is o, and へ is e.","わたしは","watashi wa","as for me…",None),
    ("ファ","new sounds","fa ti wi je","Small ァ ィ ゥ ェ ォ build sounds Japanese borrowed: ファ, ティ, ウィ, ジェ.","パーティー","pātī","party","ァ"),
]

# char, reading kana, romaji, meaning, parts, story, word, word romaji, word meaning
KANJI = [
 [1,"一","いち","ichi","one",[],"A single stroke laid flat: one.","一月","ichigatsu","January"],
 [1,"二","に","ni","two",[],"Two strokes stacked, the lower one longer: two.","二月","nigatsu","February"],
 [1,"三","さん","san","three",[],"Three strokes, three.","三月","sangatsu","March"],
 [1,"口","くち","kuchi","mouth",[],"An open mouth, seen head on: a square.","口","kuchi","mouth"],
 [1,"日","ひ","hi","sun, day",["口","一"],"A mouth (口) with one (一) line across it, the sun.","あさ日","asahi","the morning sun"],
 [1,"月","つき","tsuki","moon, month",[],"A crescent with two bands of light: the moon.","三日月","mikazuki","crescent moon"],
 [1,"目","め","me","eye",["日","一"],"The sun (日) with one more line: an eye turned on its side.","目","me","eye"],
 [1,"木","き","ki","tree",[],"A trunk, branches across the top, roots spreading below.","木","ki","tree"],
 [1,"人","ひと","hito","person",[],"Two legs, mid stride: a person.","あの人","ano hito","that person"],
 [1,"山","やま","yama","mountain",[],"Three peaks, the middle one tallest.","山みち","yamamichi","mountain path"],
 [1,"川","かわ","kawa","river",[],"Three lines of water running downhill.","川","kawa","river"],
 [2,"十","じゅう","jū","ten",["一"],"One (一) crossed by a vertical stroke: ten.","十","jū","ten"],
 [2,"田","た","ta","rice field",["口","十"],"A field (口) split by paths (十) into four paddies.","田んぼ","tanbo","rice paddy"],
 [2,"力","ちから","chikara","power",[],"An arm bent and flexed.","力","chikara","strength"],
 [2,"男","おとこ","otoko","man",["田","力"],"Power (力) in the field (田): the one sent to work it.","男の人","otoko no hito","man"],
 [2,"子","こ","ko","child",[],"A swaddled baby with its arms out.","子ども","kodomo","child"],
 [2,"女","おんな","onna","woman",[],"A figure kneeling, legs folded.","女の人","onna no hito","woman"],
 [2,"好","すき","suki","like",["女","子"],"A woman (女) beside her child (子): what she loves.","好き","suki","to like"],
 [2,"大","おお","ō","big",["一","人"],"A person (人) stretching both arms wide (一): big.","大きい","ōkii","big"],
 [2,"小","ちい","chii","small",[],"A tiny hook between two little drops.","小さい","chiisai","small"],
 [2,"火","ひ","hi","fire",["人"],"A person (人) with sparks flying off both sides: fire.","火","hi","fire"],
 [2,"水","みず","mizu","water",[],"A main stream with splashes either side.","水","mizu","water"],
 [3,"本","ほん","hon","book, origin",["木","一"],"A tree (木) with a line at its root: the origin, and the book.","日本","nihon","Japan"],
 [3,"休","やす","yasu","rest",["亻","木"],"A person (亻) leaning on a tree (木): rest.","休み","yasumi","holiday"],
 [3,"林","はやし","hayashi","grove",["木","木"],"Two trees (木 木): a grove.","林","hayashi","grove"],
 [3,"森","もり","mori","forest",["木","木","木"],"Three trees: a forest.","森","mori","forest"],
 [3,"明","あか","aka","bright",["日","月"],"Sun (日) and moon (月) together: bright.","明るい","akarui","bright"],
 [3,"白","しろ","shiro","white",["丿","日"],"A ray (丿) glancing off the sun (日): white light.","白い","shiroi","white"],
 [3,"百","ひゃく","hyaku","hundred",["一","白"],"One (一) over white (白): a hundred.","百","hyaku","hundred"],
 [3,"上","うえ","ue","above",["一"],"A mark rising above the ground line (一).","上","ue","above"],
 [3,"下","した","shita","below",["一"],"A mark hanging under the line (一).","下","shita","below"],
 [3,"中","なか","naka","inside",["口"],"A stroke straight through the middle of a mouth (口).","中","naka","inside"],
 [3,"早","はや","haya","early",["日","十"],"The sun (日) above a cross (十), clearing the horizon: early.","早い","hayai","early"],
]
PART_MEANINGS = {"一":"one","口":"mouth","日":"sun","十":"ten","田":"field","力":"power","女":"woman",
                 "子":"child","人":"person","木":"tree","亻":"person (side form)","月":"moon","白":"white","丿":"a slash"}

def build():
    cards = []
    for i, (c, r, w, wr, wm) in enumerate(HIRA):
        cards.append(dict(id="h%02d" % i, kind="hira", ch=c, rd=r, w=w, wr=wr, wm=wm,
                          note=HIRA_NOTES.get(c, ""), st=c))
    for i, (c, r, w, wr, wm) in enumerate(KATA):
        cards.append(dict(id="k%02d" % i, kind="kata", ch=c, rd=r, w=w, wr=wr, wm=wm,
                          note=KATA_NOTES.get(c, ""), st=c, pair=HIRA[i][0]))
    for i, (c, name, rd, note, w, wr, wm, st) in enumerate(RULES):
        d = dict(id="r%02d" % i, kind="rule", ch=c, rd=rd, name=name, note=note, w=w, wr=wr, wm=wm)
        if st: d["st"] = st
        cards.append(d)
    for i, (lv, c, kana, rd, mean, parts, story, w, wr, wm) in enumerate(KANJI):
        cards.append(dict(id="j%02d" % i, kind="kanji", ch=c, kana=kana, rd=rd, mean=mean, lv=lv,
                          parts=[[p, PART_MEANINGS[p]] for p in parts], note=story, w=w, wr=wr, wm=wm, st=c))
    return cards

def strokes_for(kvg_dir, ch):
    f = os.path.join(kvg_dir, "%05x.svg" % ord(ch))
    if not os.path.exists(f): return None
    s = open(f, encoding="utf-8").read()
    pat = re.compile(r'<path[^>]*id="kvg:%05x-s(\d+)"[^>]*d="([^"]+)"' % ord(ch))
    found = sorted(((int(n), d) for n, d in pat.findall(s)))
    if not found:  # attribute order may differ
        pat = re.compile(r'<path[^>]*d="([^"]+)"[^>]*id="kvg:%05x-s(\d+)"' % ord(ch))
        found = sorted(((int(n), d) for d, n in pat.findall(s)))
    return [d for _, d in found]

if __name__ == "__main__":
    kvg = sys.argv[1]
    out = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets")
    os.makedirs(out, exist_ok=True)
    cards = build()
    need = set(c["st"] for c in cards if c.get("st")) | set(PART_MEANINGS) | {"字"}
    strokes = {}
    for ch in sorted(need):
        s = strokes_for(kvg, ch)
        if s: strokes[ch] = s
        else: print("no strokes for", ch)
    json.dump(cards, open(os.path.join(out, "curriculum.json"), "w", encoding="utf-8"), ensure_ascii=False, separators=(",", ":"))
    json.dump(strokes, open(os.path.join(out, "strokes.json"), "w", encoding="utf-8"), ensure_ascii=False, separators=(",", ":"))
    kinds = {}
    for c in cards: kinds[c["kind"]] = kinds.get(c["kind"], 0) + 1
    print(len(cards), "cards", kinds, "|", len(strokes), "stroke sets")
