package ru.itmo.java.architectures.server.tasks

class InsertionSortTest: SortTest() {
    override fun <V : Comparable<V>> sort(array: Array<V>): Array<V> = InsertionSortTask(array).call()
}
