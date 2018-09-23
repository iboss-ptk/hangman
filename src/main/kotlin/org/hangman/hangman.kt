package org.hangman

import io.reactivex.Observable

const val MAX_LIFE = 7

enum class Status { IN_PROGRESS, WIN, LOSE }

data class Result(
    val status: Status,
    val selectedLetters: List<Char>,
    val lifeLeft: Int,
    val knownSecretWord: String
)

fun knownSecretWord(secretWord: String, letters: List<Char>) =
    secretWord.fold("") { acc, curr ->
        acc + if (letters.contains(curr)) curr else "_"
    }


fun reactiveHangman(secretWord: String, letters: Observable<Char>): Observable<Result> {
    val initialResult = Result(
        Status.IN_PROGRESS,
        listOf(),
        MAX_LIFE,
        knownSecretWord(secretWord, listOf())
    )

    return letters.scan(initialResult) { acc, curr ->
        val selectedLetters = acc.selectedLetters + curr
        val lifeLeft = if (secretWord.contains(curr)) acc.lifeLeft else acc.lifeLeft - 1
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
}
