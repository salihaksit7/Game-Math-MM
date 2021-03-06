package com.salihaksit.mm.viewmodel

import androidx.lifecycle.MutableLiveData
import com.salihaksit.core.GameEntity
import com.salihaksit.domain.GetGameUseCase
import com.salihaksit.domain.PlayGameUseCase
import com.salihaksit.domain.TimeUseCase
import com.salihaksit.mm.data.CellViewEntity
import com.salihaksit.mm.view.adapter.BaseRecyclerAdapter
import com.salihaksit.mm.view.adapter.ItemClickListener
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import javax.inject.Inject

class GameVM @Inject constructor(
    private val gameUseCase: GetGameUseCase,
    private val playGameUseCase: PlayGameUseCase,
    private val timeUseCase: TimeUseCase
) : BaseViewModel(),
    ItemClickListener<CellViewEntity> {

    private val cellList = MutableList(9) { CellViewEntity("") }
    private val timeDisposable = CompositeDisposable()
    private lateinit var gameEntity: GameEntity

    val adapter = BaseRecyclerAdapter(cellList, this)
    val firstNumber = MutableLiveData<String>().apply { value = "" }
    val endNumber = MutableLiveData<String>().apply { value = "" }
    val remainingMoveCount = MutableLiveData<Int>().apply { value = 0 }
    val scheduleLayoutAnimation = MutableLiveData<Boolean>()
    val infoStatus = MutableLiveData<Int>().apply { value = NO_INFO }

    val remainingTime = MutableLiveData<String>().apply { value = "" }
    val solvedGameCount = MutableLiveData<Int>().apply { value = 0 }
    val visibilityTimer = MutableLiveData<Boolean>().apply { value = false }
    val showTimeOutDialog = MutableLiveData<Boolean>().apply { value = false }

    init {
        gameUseCase.observeGameCreation().subscribe({
            gameEntity = it
            setGame()
        }, { println(it) }).addTo(disposableList)

        newGame()
    }

    fun newGame() {
        gameUseCase.createNewGame()
    }

    fun restart() {
        setGame()
    }

    fun showTimer(gameWithTime: Boolean) {
        if (gameWithTime.not())
            return

        visibilityTimer.value = true
        getTimer()
    }

    fun resetTimer() {
        solvedGameCount.value = 0

        clearTimeDisposable()
        getTimer()
    }

    private fun getTimer() {
        timeUseCase.getCountDownTimer(GAME_DURATION)
            .doAfterTerminate { doOnTimeOut() }
            .subscribe({
                remainingTime.postValue("$it")
            }, {})
            .addTo(timeDisposable)
    }

    private fun doOnTimeOut() {
        showTimeOutDialog.postValue(true)
    }

    private fun clearTimeDisposable(){
        timeDisposable.clear()
    }

    private fun setGame() {
        infoStatus.value = NO_INFO

        firstNumber.value = gameEntity.firstNumber
        endNumber.value = gameEntity.endNumber
        remainingMoveCount.value = gameEntity.moveCount

        val size = gameEntity.uniqueElements.size
        cellList.forEachIndexed { index, cellViewEntity ->
            if (index < size)
                cellViewEntity.option = gameEntity.uniqueElements[index]
            else
                cellViewEntity.option = ""
        }

        cellList.shuffle()
        adapter.setList(cellList)
        scheduleLayoutAnimation.value = true
    }

    override fun onItemClick(item: CellViewEntity) {
        if (item.option.isEmpty())
            return

        if (infoStatus.value != NO_INFO)
            return

        if (remainingMoveCount.value == 0)
            return

        val tmp = playGameUseCase.getCalculatedPhrase(
            firstNumber.value!!,
            item.option
        )

        firstNumber.value = tmp
        remainingMoveCount.value = (remainingMoveCount.value!! - 1)

        if (tmp == endNumber.value) {
            infoStatus.value = SUCCESS_INFO
            solvedGameCount.value = (solvedGameCount.value!! + 1)
        } else if (remainingMoveCount.value == 0) {
            infoStatus.value = WARNING_INFO
        }

    }

    override fun onCleared() {
        super.onCleared()
        clearTimeDisposable()
    }

    companion object {
        const val NO_INFO = 0
        const val SUCCESS_INFO = 1
        const val WARNING_INFO = 2

        const val GAME_DURATION = 30L
    }
}