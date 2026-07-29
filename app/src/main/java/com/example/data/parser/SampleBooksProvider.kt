package com.example.data.parser

object SampleBooksProvider {

    fun getArtOfWar(): ParsedEpub {
        return ParsedEpub(
            title = "The Art of War",
            author = "Sun Tzu",
            chapters = listOf(
                EpubChapter(
                    chapterIndex = 0,
                    title = "I. Laying Plans",
                    paragraphs = listOf(
                        "Sun Tzu said: The art of war is of vital importance to the State.",
                        "It is a matter of life and death, a road either to safety or to ruin. Hence it is a subject of inquiry which can on no account be neglected.",
                        "The art of war, then, is governed by five constant factors, to be taken into account in one's deliberations, when seeking to determine the conditions obtaining in the field.",
                        "These are: (1) The Moral Law; (2) Heaven; (3) Earth; (4) The Commander; (5) Method and discipline.",
                        "The Moral Law causes the people to be in complete accord with their ruler, so that they will follow him regardless of their lives, undismayed by any danger.",
                        "Heaven signifies night and day, cold and heat, times and seasons. Earth comprises distances, great and small; danger and security; open ground and narrow passes; the chances of life and death.",
                        "The Commander stands for the virtues of wisdom, sincerity, benevolence, courage and strictness.",
                        "All warfare is based on deception. Hence, when able to attack, we must seem unable; when using our forces, we must seem inactive; when we are near, we must make the enemy believe we are far away."
                    )
                ),
                EpubChapter(
                    chapterIndex = 1,
                    title = "II. Waging War",
                    paragraphs = listOf(
                        "Sun Tzu said: In the operations of war, where there are in the field a thousand swift chariots, as many heavy chariots, and a hundred thousand mail-clad soldiers, with provisions enough to carry them a thousand li, the expenditure at home and at the front will reach the total of a thousand ounces of silver per day.",
                        "When you engage in actual fighting, if victory is long in coming, then men's weapons will grow dull and their ardor will be damped. If you lay siege to a town, you will exhaust your strength.",
                        "Again, if the campaign is protracted, the resources of the State will not be equal to the strain.",
                        "Now, when your weapons are dull, your ardor damped, your strength exhausted and your treasure spent, other chieftains will spring up to take advantage of your extremity. Then no man, however wise, will be able to avert the consequences that must ensue.",
                        "In war, then, let your great object be victory, not lengthy campaigns."
                    )
                ),
                EpubChapter(
                    chapterIndex = 2,
                    title = "III. Attack by Stratagem",
                    paragraphs = listOf(
                        "Sun Tzu said: In the practical art of war, the best thing of all is to take the enemy's country whole and intact; to shatter and destroy it is not so good.",
                        "So, too, it is better to recapture an army entire than to destroy it, to capture a regiment, a detachment or a company entire than to destroy them.",
                        "Hence to fight and conquer in all your battles is not supreme excellence; supreme excellence consists in breaking the enemy's resistance without fighting.",
                        "Thus the highest form of generalship is to balk the enemy's plans; the next best is to prevent the junction of the enemy's forces; the next in order is to attack the enemy's army in the field; and the worst policy of all is to besiege walled cities.",
                        "If you know the enemy and know yourself, you need not fear the result of a hundred battles. If you know yourself but not the enemy, for every victory gained you will also suffer a defeat. If you know neither the enemy nor yourself, you will succumb in every battle."
                    )
                ),
                EpubChapter(
                    chapterIndex = 3,
                    title = "IV. Tactical Dispositions",
                    paragraphs = listOf(
                        "Sun Tzu said: The good fighters of old first put themselves beyond the possibility of defeat, and then waited for an opportunity of defeating the enemy.",
                        "To secure ourselves against defeat lies in our own hands, but the opportunity of defeating the enemy is provided by the enemy himself.",
                        "Thus the good fighter is able to secure himself against defeat, but cannot make certain of defeating the enemy.",
                        "Hence the saying: One may know how to conquer without being able to do it.",
                        "Security against defeat implies defensive tactics; ability to defeat the enemy means taking the offensive."
                    )
                ),
                EpubChapter(
                    chapterIndex = 4,
                    title = "V. Energy & Momentum",
                    paragraphs = listOf(
                        "Sun Tzu said: The control of a large force is the same principle as the control of a few men: it is merely a question of dividing up their numbers.",
                        "Fighting with a large army under your command is nowise different from fighting with a small one: it is merely a question of instituting signs and signals.",
                        "In all fighting, the direct method may be used for joining battle, but indirect methods will be needed in order to secure victory.",
                        "Indirect tactics, efficiently applied, are inexhaustible as Heaven and Earth, unending as the flow of rivers and streams; like the sun and moon, they end but to begin anew."
                    )
                )
            )
        )
    }

    fun getPrideAndPrejudice(): ParsedEpub {
        return ParsedEpub(
            title = "Pride and Prejudice",
            author = "Jane Austen",
            chapters = listOf(
                EpubChapter(
                    chapterIndex = 0,
                    title = "Chapter 1",
                    paragraphs = listOf(
                        "It is a truth universally acknowledged, that a single man in possession of a good fortune, must be in want of a wife.",
                        "However little known the feelings or views of such a man may be on his first entering a neighbourhood, this truth is so well fixed in the minds of the surrounding families, that he is considered the rightful property of some one or other of their daughters.",
                        "\"My dear Mr. Bennet,\" said his lady to him one day, \"have you heard that Netherfield Park is let at last?\"",
                        "Mr. Bennet replied that he had not.",
                        "\"But it is,\" returned she; \"for Mrs. Long has just been here, and she told me all about it.\"",
                        "Mr. Bennet made no answer.",
                        "\"Do you not want to know who has taken it?\" cried his wife impatiently.",
                        "\"You want to tell me, and I have no objection to hearing it.\"",
                        "This was invitation enough.",
                        "\"Why, my dear, you must know, Mrs. Long says that Netherfield is taken by a young man of large fortune from the north of England; that he came down on Monday in a chaise and four to see the place, and was so much delighted with it, that he agreed with Mr. Morris immediately.\""
                    )
                ),
                EpubChapter(
                    chapterIndex = 1,
                    title = "Chapter 2",
                    paragraphs = listOf(
                        "Mr. Bennet was among the earliest of those who waited on Mr. Bingley. He had always intended to visit him, though to the last always assuring his wife that he should not go.",
                        "The astonishment of the ladies was just what he wished; that of Mrs. Bennet perhaps surpassing the rest.",
                        "\"How good it was in you, my dear Mr. Bennet! But I knew I should persuade you at last. I was sure you loved your girls too well to neglect such an acquaintance.\"",
                        "Now opened a new chapter of lively conversations and eager anticipations at Longbourn."
                    )
                )
            )
        )
    }

    fun getLuminaGuideText(): ParsedEpub {
        return ParsedEpub(
            title = "Lumina Reader Guide & Classics",
            author = "Lumina Editorial",
            chapters = listOf(
                EpubChapter(
                    chapterIndex = 0,
                    title = "Page 1: Welcome to Lumina Reader",
                    paragraphs = listOf(
                        "Welcome to Lumina Reader — your lightweight, elegant, offline-first reading companion.",
                        "Lumina is designed to provide an unparalleled reading experience for both PDF documents and EPUB books.",
                        "Features Include:",
                        "1. High-Quality Natural Speech: Support for HD Neural voice engines with natural human-like expression.",
                        "2. Real-Time Sentence Tracking: Sentences highlight automatically as the narrator reads aloud.",
                        "3. Custom Color Themes: Choose between Light Paper, Dark Obsidian, Sepia Vintage, and OLED Night modes.",
                        "4. Full Offline Capability: All books, reading positions, bookmarks, and local highlights are stored securely on your device."
                    )
                ),
                EpubChapter(
                    chapterIndex = 1,
                    title = "Page 2: Using Text-to-Speech & Highlights",
                    paragraphs = listOf(
                        "To start listening to any chapter or page, tap the floating Play button at the bottom of the screen.",
                        "As the voice speaks, each sentence will glow with a soft highlight and automatically scroll into view.",
                        "You can tap on any sentence in the book to immediately jump speech playback to that location.",
                        "Tap and hold or press the Highlight button to save custom notes and color-coded quotes into your personal collection."
                    )
                ),
                EpubChapter(
                    chapterIndex = 2,
                    title = "Page 3: Importing Your Own Books",
                    paragraphs = listOf(
                        "Lumina Reader allows you to import any EPUB or PDF file directly from your Android device.",
                        "Tap the '+' button on the main Library screen to browse your storage or Downloads folder.",
                        "Once imported, your custom books will instantly appear in your library with personalized reading progress tracking."
                    )
                )
            )
        )
    }
}
