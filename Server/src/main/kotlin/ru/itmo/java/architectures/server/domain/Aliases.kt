package ru.itmo.java.architectures.server.domain

import ru.itmo.java.architectures.server.tasks.InsertionSortTask

typealias SortCallable<V> = InsertionSortTask<V>
