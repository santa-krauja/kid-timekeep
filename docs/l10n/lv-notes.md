# Latvian translation notes

Source: `app/src/main/res/values/strings.xml` (English, default) and
`app/src/main/res/values-lv/strings.xml` (Latvian). `TranslationCompletenessTest` fails the
build if a translatable key is missing in Latvian or its placeholders differ.

## Style

- Informal **tu** throughout ("Izvēlies", "Pieskaries", "Dod taimerim nosaukumu"): the app is
  for families and children.
- Buttons and menu items use the infinitive ("Sākt", "Saglabāt", "Dzēst"), as Android does.
- Quotes: „…“ (U+201E / U+201C).
- Abbreviations stay as in English: `min`, `s`, `h` (e.g. "5 min", "30 s", "1 h").
- The app name "Kid Timekeep" is not translated.

## Plurals (CLDR `lv`)

| quantity | numbers | minūte | sekunde | stunda |
|---|---|---|---|---|
| zero | 0, 10–20, 30, 40, … 100, 110–120 … | minūšu | sekunžu | stundu |
| one | 1, 21, 31, 41 … (not 11) | minūte | sekunde | stunda |
| other | everything else (2–9, 22–29 …) | minūtes | sekundes | stundas |

So "1 minūte 30 sekunžu", "21 minūte", "11 minūšu", "2 stundas".

## Glossary

| English | Latviešu | Note |
|---|---|---|
| timer | taimeris | |
| preset | saglabātais taimeris | "Saglabātie taimeri", "Dzēst saglabāto taimeri?", "Labot taimeri", "Saglabāt sarakstā"; delete message „%s“ tiks izdzēsts no saraksta (agrees with implied "taimeris") |
| look (pictures + sand colours) | izskats | "Mīļākie izskati", "Katru reizi cits izskats" |
| picture | attēls | |
| sand | smiltis (pl.) | "Augšējās smiltis"; sand colour in TalkBack = "krāsa" |
| hourglass | smilšu pulkstenis | |
| Left / Passed | Atlicis / Pagājis | standalone labels |
| "%s left" | "Vēl %s" | avoids gender/number agreement with the clock or phrase |
| Start / Pause / Go on | Sākt / Pauze / Turpināt | |
| Start over | Sākt no jauna | |
| Again | Vēlreiz | |
| Time's up! | Laiks beidzies! | also notification text and channel name |
| Done! | Gatavs! | |
| Shuffle | Cits izskats | random look button (matches "Katru reizi cits izskats") |
| Theme | Motīvs | as in Android ("Tumšais motīvs") |
| Emoji | emocijzīmes | |

Pictures: Sirds, Zvaigzne, Zieds, Smaidiņš, Saule, Mēness, Zivs, Kaķis, Suns, Mašīna, Koks,
Tauriņš, Ābols, Varavīksne, Vienradzis, Raķete.
Sand colours: Lavanda, Debesis, Piparmētra, Persiks, Smilškrāsa, Rozā, Nakts, Citrons.
Seed presets (written once, in the device language at first launch): Zobu tīrīšana,
Ģērbšanās, Lasīšana (all verbal nouns, so they read like names).

## Please double-check (native speaker)

1. **"Saglabātais taimeris" for preset** (section header "Saglabātie taimeri", "Labot taimeri",
   "Saglabāt sarakstā"). Earlier draft used "sagatave", which was dropped: it suggests a blank
   or draft. A shorter header "Saglabātie" is an option.
2. **"Vēl %s"** for "%s left" on cards and in TalkBack ("Zobu tīrīšana, vēl 2 minūtes").
   The large timer label uses "Atlicis" as in the spec (TalkBack reads it as a separate node).
3. **"Pagājis 1:30 no 5:00"** (Passed … of …). Alternatives: "Pagājis: 1:30 no 5:00", "1:30 no 5:00".
4. **TalkBack look description:** "Augšā Sirds, krāsa Lavanda; apakšā Zvaigzne, krāsa
   Debesis". "krāsa" + noun colour name avoids declension and the "smiltis Smiltis" echo.
5. **Stepper descriptions:** "Stundas: vairāk" / "Stundas: mazāk" / "Stundas: 2". This avoids
   the genitive ("vairāk stundu") that would need separate strings.
6. **"Cits izskats"** for the shuffle-look button (playful alternative: "Pārsteigums!").
7. **"Sākt: %s"** for the start-preset button description (the colon avoids declining the
   user's preset name). Alternative: "Sākt taimeri „%s“".
8. **Colour names:** "Debesis" (Sky) is a plural noun, "Smilškrāsa" replaces "Smiltis" to avoid
   "Augšējās smiltis: Smiltis", "Rozā" is an indeclinable adjective.
9. **Zero plural form** uses the genitive ("11 minūšu", "vēl 10 minūšu"). If everyday
   nominative ("11 minūtes") is preferred, make the zero items equal to the other items and
   update `DurationPhraseTest`.
10. **Start over dialog:** "Smilšu pulkstenis apgriezīsies, un taimeris atkal sāks skaitīt no 5:00."
