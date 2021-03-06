package com.salihaksit.gamecreator

import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject
import kotlin.random.Random

class Game @Inject constructor(private val gameFilters: GameFilters) {

    private val compositeDisposable = CompositeDisposable()

    var source = PublishSubject.create<Pair<Int, List<String>>>()

    fun getOptimumGame() {
        Observable.just(getNewGame())
            .subscribe(
                {
                    for (filter in gameFilters.filters()) {
                        if (filter.isValid(it).not())
                            throw Exception()
                    }

                    source.onNext(it)
                    clearDisposables()
                },
                {
                    getOptimumGame()
                }
            )
            .addTo(compositeDisposable)
    }

    private fun getRandomStartNumber(): Int {
        return Random.nextInt(MAX_START_NUMBER)
    }

    // max num is 9 min is 2
    private fun getRandomNumbers(): IntArray {
        return intArrayOf(
            Random.nextInt(MAX_RANDOM_NUMBER) + MIN_NUMBER,
//            Random.nextInt(MAX_RANDOM_NUMBER) + MIN_NUMBER,
            Random.nextInt(MAX_RANDOM_NUMBER) + MIN_NUMBER
        )
    }

    private fun getRandomMoveCount(): Int {
        return Random.nextInt(MAX_RANDOM_MOVE_COUNT) + MIN_MOVE_COUNT
    }

    private fun getRandomOperator(): Operator {
        val arr = arrayListOf(ADDITION(), SUBTRACTION(), MULTIPLICATION(), DIVISION(), CUT())
        return arr[Random.nextInt(arr.size)]
    }

    private fun getNewGame(): Pair<Int, List<String>> {
        val numbers = getRandomNumbers()
        val numberCount = numbers.size
        val moveCount = getRandomMoveCount()
        val moves = arrayListOf<String>()
        var endNumber = getRandomStartNumber()

        moves.add("$endNumber")

        var previousNumber = Int.MIN_VALUE
        var previousOperator = CUT() as Operator
        for (i in 0 until moveCount + 1) {
            val number = numbers[Random.nextInt(numberCount)]
            var operator = getRandomOperator()
            if (number == previousNumber && operator.isReverseWith(previousOperator)) {
                operator = operator.getNonReversedOperator()
            }

            endNumber = operator.calculate(endNumber, number)
            moves.add(if (operator is CUT) operator.type else "${operator.type}$number")
            previousNumber = number
            previousOperator = operator
        }

        return Pair(endNumber, moves)
    }


    private fun clearDisposables() {
        compositeDisposable.clear()
    }

    companion object {
        const val MAX_START_NUMBER = 100
        const val MIN_NUMBER = 2
        const val MAX_RANDOM_NUMBER = 8
        const val MIN_MOVE_COUNT = 2
        const val MAX_RANDOM_MOVE_COUNT = 6
    }

}