package org.hangman

import io.kotlintest.properties.assertAll
import io.kotlintest.shouldBe
import io.kotlintest.specs.WordSpec
import io.reactivex.Observable
import io.reactivex.rxkotlin.toObservable
import io.reactivex.schedulers.TestScheduler
import java.util.*
import java.util.concurrent.TimeUnit

fun IntRange.random() =
        if (endInclusive < start) 0 else Random().nextInt((endInclusive + 1) - start) +  start

class HangmanTest: WordSpec({
    "Hangman.knownSecretWord" should {
        fun noMatchLetters(secretWord: String, randomLetters: String) =
                randomLetters.toList().filter { !secretWord.contains(it) }.joinToString("")

        "returns all _ with length of secretWord when no guessed letter match" {
            assertAll { secretWord: String, randomLetters: String ->
                val noMatchLetterList =  noMatchLetters(secretWord, randomLetters).toList()
                knownSecretWord(secretWord, noMatchLetterList) shouldBe "_".repeat(secretWord.length)
            }
        }

        "returns secretWord when part of guessed letters match whole secretWord" {
            assertAll { secretWord: String, randomLetters: String ->
                val matchAllGuaranteedLetters = (secretWord + randomLetters).toList().shuffled()
                knownSecretWord(secretWord, matchAllGuaranteedLetters) shouldBe secretWord
            }
        }

        "returns string with same '_' count when adding more unmatched letters" {
            assertAll { secretWord: String, randomLetters: String, moreRandomLetters: String ->
                val correctGuessed = secretWord
                        .toList()
                        .shuffled()
                        .take((1..secretWord.length).random())
                        .joinToString("")

                val mixedLetters = (correctGuessed + noMatchLetters(secretWord, randomLetters)).toList().shuffled()
                val expected = knownSecretWord(
                        secretWord, mixedLetters + noMatchLetters(secretWord, moreRandomLetters).toList()
                ).count { it == '_'}

                knownSecretWord(secretWord, mixedLetters).count { it == '_' } shouldBe expected
            }
        }

        "returns string that contains all matched letters" {
            assertAll { secretWord: String, randomLetters: String ->
                val correctGuessed = secretWord
                        .toList()
                        .shuffled()
                        .take((1..secretWord.length).random())
                        .filter { it != '_' }
                        .joinToString("")

                val mixedLetters = (correctGuessed + noMatchLetters(secretWord, randomLetters)).toList().shuffled()

                knownSecretWord(secretWord, mixedLetters).filter { it != '_' }.toSet() shouldBe correctGuessed.toSet()
            }
        }
    }

    "Hangman.reactiveHangman" should {

        "เวลา t0: เกมเริ่ม - ผู้เล่นยังไม่ได้เลือกตัวอักษร - letterStream = […]" {
            val timer: Observable<Long> = Observable.interval(1, TimeUnit.SECONDS)
            val letters = listOf<Char>()

            reactiveHangman("bigbear", letters.toObservable(), timer)
                .test()
                .assertValueAt(letters.count(), Result(Status.IN_PROGRESS, listOf(), 7, "_______" ))
        }

        "เวลา t1: ผู้เล่นเดาตัวอักษรแรกมาว่าเป็น ‘b’ - เดาถูก => letterStream = [… ‘b’] แสดง 'b__b___'" {
            val timer: Observable<Long> = Observable.interval(1, TimeUnit.SECONDS)
            val letters = listOf('b')

            reactiveHangman("bigbear", letters.toObservable(), timer)
                .test()
                .assertValueAt(letters.count(), Result(Status.IN_PROGRESS, letters, 7, "b__b___" ))
        }

        "เวลา t2: ผู้เล่นเดาตัวอักษรที่สองมาเป็น ‘o’ - เดาผิด => letterStream = [… ‘b’ … ‘o’] เลือดเหลือ 6" {
            val timer: Observable<Long> = Observable.interval(1, TimeUnit.SECONDS)
            val letters = listOf('b', 'o')

            reactiveHangman("bigbear", letters.toObservable(), timer)
                    .test()
                    .assertValueAt(letters.count(), Result(Status.IN_PROGRESS, letters, 6, "b__b___" ))
        }

        "เวลา t3.a: ชนะ เดาตัวอักษรลับถูกทั้งหมด - letterStream => [… ‘b’ … ‘o’ … ‘i’ … ‘g’ … ‘a’ … ‘e’ … ‘y’ … ‘r’]" {
            val timer: Observable<Long> = Observable.interval(1, TimeUnit.SECONDS)
            val letters = listOf('b', 'o', 'i', 'g', 'a', 'e', 'y', 'r')

            reactiveHangman("bigbear", letters.toObservable(), timer)
                    .test()
                    .assertValueAt(letters.count(), Result(Status.WIN, letters, 5, "bigbear" ))
        }

        "เวลา t3.b: แพ้ ตอบผิดจนครบ 7 ครั้ง - letterStream => [… ‘b’ … ‘o’ … ‘a’ … ‘e’ … ‘n’ … ‘u’ … ‘t’ … ‘z’ … ‘x’ … ‘v’]" {
            val timer: Observable<Long> = Observable.interval(1, TimeUnit.SECONDS)
            val letters = listOf('b', 'o', 'a', 'e', 'n', 'u', 't', 'z', 'x', 'v')

            reactiveHangman("bigbear", letters.toObservable(), timer)
                    .test()
                    .assertValueAt(letters.count(), Result(Status.LOSE, letters, 0, "b__bea_" ))
        }
    }

    "Hangman.reactiveHangman with timer" should {
        "ลดเลือด ถ้าไม่ตอบใน 5 วิแรก" {
            val letters = listOf<Char>()
            val timer = listOf<Long>(0, 1, 2, 3, 4, 5)

            reactiveHangman("bigbear", letters.toObservable(), timer.toObservable())
                    .test()
                    .assertValueAt(5, Result(Status.IN_PROGRESS, letters, 6, "_______" ))
        }

        "ลดเลือดอีก ถ้าไม่ตอบใน 10 วิแรก" {
            val scheduler = TestScheduler()
            val letters = listOf<Char>()
            val timer = Observable.interval(1, TimeUnit.SECONDS, scheduler)

            var result = Result()

            reactiveHangman("bigbear", letters.toObservable(), timer).subscribe {
                result = it
            }

            scheduler.advanceTimeTo(10, TimeUnit.SECONDS)

            result.shouldBe(Result(Status.IN_PROGRESS, letters, 5, "_______" ))
        }

        "เมื่อตอบแล้วจะนับเวลาใหม่" {
            val scheduler = TestScheduler()
            val letters = listOf('b')
            val timer = Observable.interval(1, TimeUnit.SECONDS, scheduler)

            val lettersObservable = letters
                    .toObservable()
                    .delay(4, TimeUnit.SECONDS, scheduler)


            var result = Result()

            reactiveHangman("bigbear", lettersObservable, timer).subscribe {
                result = it
            }

            scheduler.advanceTimeTo(6, TimeUnit.SECONDS)

            result.shouldBe(Result(Status.IN_PROGRESS, letters, 7, "b__b___" ))
        }

        "เมื่อเลือดลด ก็จะนับเวลาใหม่" {
            val scheduler = TestScheduler()
            val letters = listOf('x')
            val timer = Observable.interval(1, TimeUnit.SECONDS, scheduler)

            val lettersObservable = letters
                    .toObservable()
                    .delay(4, TimeUnit.SECONDS, scheduler)


            var result = Result()

            reactiveHangman("bigbear", lettersObservable, timer).subscribe {
                result = it
            }

            scheduler.advanceTimeTo(6, TimeUnit.SECONDS)

            result.shouldBe(Result(Status.IN_PROGRESS, letters, 6, "_______" ))
        }

        "หยุดนาฬิกาเมื่อมีผลแพ้ชนะ" {
            val scheduler = TestScheduler()
            val letters = "bigbears".toList()
            val timer = Observable.interval(1, TimeUnit.SECONDS, scheduler)

            val lettersObservable = letters
                    .toObservable()
                    .delay(1, TimeUnit.SECONDS, scheduler)

            var isCompleted = false

            reactiveHangman("bigbear", lettersObservable, timer)
                    .doOnComplete { isCompleted = true }
                    .subscribe()

            scheduler.advanceTimeTo(1, TimeUnit.SECONDS)

            isCompleted.shouldBe(true)
        }
    }
})