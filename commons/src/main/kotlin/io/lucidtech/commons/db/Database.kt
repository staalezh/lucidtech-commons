package io.lucidtech.commons.db


interface Identifiable {
    val id: String
}

interface Validatable {
    fun isValid(): Boolean
}
