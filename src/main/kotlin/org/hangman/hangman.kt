package org.hangman

import io.reactivex.Observable
import io.reactivex.rxkotlin.toObservable
import java.util.*
import java.util.concurrent.TimeUnit

const val MAX_LIFE = 7
const val COUNTDOWN_START = 5L

enum class Status { IN_PROGRESS, WIN, LOSE }

sealed class InputEvent
data class Timer(val time: Long) : InputEvent()
data class InputLetter(val letter: Char) : InputEvent()

data class HangmanEvent(val letter: Char?, val countdown: Long)

data class Result(
        val status: Status = Status.IN_PROGRESS,
        val selectedLetters: List<Char> = listOf(),
        val lifeLeft: Int = MAX_LIFE,
        val knownSecretWord: String = ""
)

fun knownSecretWord(secretWord: String, letters: List<Char>) =
        secretWord.map { if (letters.contains(it)) it else '_' }.joinToString("")


fun reactiveHangman(secretWord: String, letters: Observable<Char>, timer: Observable<Long>): Observable<Result> {
    val initialResult = Result(
            Status.IN_PROGRESS,
            listOf(),
            MAX_LIFE,
            knownSecretWord(secretWord, listOf())
    )

    val hangmanEvent = Observable.merge(timer.map(::Timer), letters.map(::InputLetter))
            .scan(HangmanEvent(null, COUNTDOWN_START)) { res, e ->
                when (e) {
                    is Timer -> if (res.countdown == 1L)
                        HangmanEvent(null, COUNTDOWN_START)
                    else
                        HangmanEvent(null, res.countdown - 1L)
                    is InputLetter -> HangmanEvent(e.letter, COUNTDOWN_START)
                }
            }

    val results = hangmanEvent
            .scan(initialResult) { acc, (letter, countdown) ->
                val selectedLetters = if (letter != null) acc.selectedLetters + letter else acc.selectedLetters
                val lifeLeft = if (letter != null && !secretWord.contains(letter) || countdown == 1L) acc.lifeLeft - 1 else acc.lifeLeft
                val status = when {
                    lifeLeft <= 0 -> Status.LOSE
                    secretWord.all(selectedLetters::contains) -> Status.WIN
                    else -> Status.IN_PROGRESS
                }
                Result(
                        status,
                        selectedLetters,
                        lifeLeft,
                        knownSecretWord(secretWord, selectedLetters)
                )
            }

    return results
            .skip(1)
            .takeUntil { it.status != Status.IN_PROGRESS }
}


fun main(args: Array<String>) {
    val letters = Scanner(System.`in`)
            .toObservable()
            .map { it.first() }

    val timer = Observable.interval(1, TimeUnit.SECONDS)

    reactiveHangman("bigbear", letters, timer)
            .subscribe {
                println("status: ${it.status}")
                println("life: ${it.lifeLeft}")
                println()
                println(it.knownSecretWord)
            }
}