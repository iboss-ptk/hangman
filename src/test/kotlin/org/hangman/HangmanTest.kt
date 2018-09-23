package org.hangman

import io.kotlintest.properties.assertAll
import io.kotlintest.shouldBe
import io.kotlintest.specs.WordSpec
import io.reactivex.rxkotlin.toObservable
import java.util.*

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
            val letters = listOf<Char>().toObservable()
            reactiveHangman("bigbear", letters)
                .test()
                .assertValueAt(0, Result(Status.IN_PROGRESS, listOf(), 7, "_______" ))
        }

        "เวลา t1: ผู้เล่นเดาตัวอักษรแรกมาว่าเป็น ‘b’ - เดาถูก => letterStream = [… ‘b’] แสดง 'b__b___'" {
            val letters = listOf('b')
            reactiveHangman("bigbear", letters.toObservable())
                .test()
                .assertValueAt(1, Result(Status.IN_PROGRESS, letters, 7, "b__b___" ))
        }

        "เวลา t2: ผู้เล่นเดาตัวอักษรที่สองมาเป็น ‘o’ - เดาผิด => letterStream = [… ‘b’ … ‘o’] เลือดเหลือ 6" {
            val letters = listOf('b', 'o')
            reactiveHangman("bigbear", letters.toObservable())
                    .test()
                    .assertValueAt(2, Result(Status.IN_PROGRESS, letters, 6, "b__b___" ))
        }

        "เวลา t3.a: ชนะ เดาตัวอักษรลับถูกทั้งหมด - letterStream => [… ‘b’ … ‘o’ … ‘i’ … ‘g’ … ‘a’ … ‘e’ … ‘y’ … ‘r’]" {
            val letters = listOf('b', 'o', 'i', 'g', 'a', 'e', 'y', 'r')
            reactiveHangman("bigbear", letters.toObservable())
                    .test()
                    .assertValueAt(8, Result(Status.WIN, letters, 5, "bigbear" ))
        }

        "เวลา t3.b: แพ้ ตอบผิดจนครบ 7 ครั้ง - letterStream => [… ‘b’ … ‘o’ … ‘a’ … ‘e’ … ‘n’ … ‘u’ … ‘t’ … ‘z’ … ‘x’ … ‘v’]" {
            val letters = listOf('b', 'o', 'a', 'e', 'n', 'u', 't', 'z', 'x', 'v')
            reactiveHangman("bigbear", letters.toObservable())
                    .test()
                    .assertValueAt(10, Result(Status.LOSE, letters, 0, "b__bea_" ))
        }
    }
})