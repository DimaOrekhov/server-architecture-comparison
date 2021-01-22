package ru.itmo.java.architectures.server.tasks

class InsertionSortIntArrayTest : SortIntArrayTest() {
    override fun sort(array: IntArray): IntArray = InsertionSortIntArrayTask(array, inPlace = false).call()

    override fun inPlaceSort(array: IntArray): IntArray = InsertionSortIntArrayTask(array, inPlace = true).call()
}
