package org.mechdancer.symbol

import org.mechdancer.symbol.Multinomial.Companion.multinomial
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

operator fun Expression.plus(others: Expression) =
    multinomial(this, others)

inline class DExpression(val expression: Expression) {
    operator fun div(others: DExpression) =
        (others.expression as? Variable)
            ?.let(expression::d)
        ?: throw NotImplementedError()

    companion object {
        fun d(e: Expression) = DExpression(e)
    }
}

class ValueCalculator internal constructor(e: Expression) {
    var expression = e
        private set

    operator fun set(v: Variable, x: Number) {
        expression = expression.substitute(v, Constant(x.toDouble()))
    }
}

fun Expression.substitute(block: ValueCalculator.() -> Unit) =
    ValueCalculator(this).apply(block).expression

class VariableProperty : ReadOnlyProperty<Any?, Variable> {
    override fun getValue(thisRef: Any?, property: KProperty<*>) =
        Variable(property.name)
}

val variable get() = VariableProperty()

operator fun Number.times(e: Expression) =
    e * Constant(toDouble())

operator fun Expression.times(n: Number) =
    this * Constant(n.toDouble())

operator fun Number.plus(e: Expression) =
    e + Constant(toDouble())

operator fun Expression.plus(n: Number) =
    this + Constant(n.toDouble())
