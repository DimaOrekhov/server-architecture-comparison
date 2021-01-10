package ru.itmo.java.architectures.experiment.schedulers


object NumberUtils {

    operator fun Number.plus(other: Number): Number =
            when (this) {
                is Long   -> this.toLong() + other.toLong()
                is Int    -> this.toInt()  + other.toInt()
                is Short  -> this.toShort() + other.toShort()
                is Byte   -> this.toByte() + other.toByte()
                is Double -> this.toDouble() + other.toDouble()
                is Float  -> this.toFloat() + other.toFloat()
                else      -> throw RuntimeException("Unknown numeric type")
            }

    operator fun Number.compareTo(other: Number): Int =
            when (this) {
                is Long   -> this.toLong().compareTo(other.toLong())
                is Int    -> this.toInt().compareTo(other.toInt())
                is Short  -> this.toShort().compareTo(other.toShort())
                is Byte   -> this.toByte().compareTo(other.toByte())
                is Double -> this.toDouble().compareTo(other.toDouble())
                is Float  -> this.toFloat().compareTo(other.toFloat())
                else      -> throw RuntimeException("Unknown numeric type")
            }
}
