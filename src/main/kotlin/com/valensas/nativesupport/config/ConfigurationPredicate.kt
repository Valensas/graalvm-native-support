package com.valensas.nativesupport.config

interface ConfigurationPredicateValue {
    val value: String

    fun exists() = ExistsPredicate(value)

    fun doesNotExist() = !exists()
}

interface ConfigurationPredicate<T> {
    companion object {
        fun <T> constant(value: Boolean) = ConstantPredicate<T>(value)
    }

    fun test(value: T): Boolean

    operator fun not() = NotPredicate(this)

    infix fun and(predicate: ConfigurationPredicate<T>) =
        OperatorPredicate(
            this,
            predicate,
            Boolean::and
        )

    infix fun or(predicate: ConfigurationPredicate<T>) =
        OperatorPredicate(
            this,
            predicate,
            Boolean::or
        )

    fun masterSwitch(value: Boolean?) = MasterSwitchPredicate(value, this)
}

class MasterSwitchPredicate<T>(
    private val value: Boolean?,
    private val other: ConfigurationPredicate<T>
) : ConfigurationPredicate<T> {
    override fun test(value: T): Boolean = this.value ?: other.test(value)
}

class ConstantPredicate<T>(
    private val value: Boolean
) : ConfigurationPredicate<T> {
    override fun test(value: T): Boolean = this.value
}

class NotPredicate<T>(
    private val original: ConfigurationPredicate<T>
) : ConfigurationPredicate<T> {
    override fun test(value: T): Boolean = !original.test(value)
}

class ExistsPredicate<T>(
    private val value: T
) : ConfigurationPredicate<Collection<T>> {
    override fun test(value: Collection<T>): Boolean = value.contains(this.value)
}

class OperatorPredicate<T>(
    private val lhs: ConfigurationPredicate<T>,
    private val rhs: ConfigurationPredicate<T>,
    private val operator: (Boolean, Boolean) -> Boolean
) : ConfigurationPredicate<T> {
    override fun test(value: T): Boolean = operator(lhs.test(value), rhs.test(value))
}
