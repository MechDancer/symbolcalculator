package org.mechdancer.symbol

import org.mechdancer.symbol.Multinomial.Companion.multinomial
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

// 求导

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

// 代入

class ValueCalculator internal constructor(e: Expression) {
    var expression = e
        private set

    operator fun set(v: Variable, x: Number) {
        expression = expression.substitute(v, Constant(x.toDouble()))
    }
}

fun Expression.substitute(block: ValueCalculator.() -> Unit) =
    ValueCalculator(this).apply(block).expression

// 定义变量

class VariableProperty : ReadOnlyProperty<Any?, Variable> {
    override fun getValue(thisRef: Any?, property: KProperty<*>) =
        Variable(property.name)
}

val variable get() = VariableProperty()

// 表达式运算

operator fun Expression.unaryMinus() = this * Constant(-1.0)

operator fun Expression.plus(n: Number) = this + Constant(n.toDouble())
operator fun Expression.minus(n: Number) = this + Constant(-n.toDouble())
operator fun Expression.times(n: Number) = this * Constant(n.toDouble())
operator fun Expression.div(n: Number) = this * Constant(1 / n.toDouble())

operator fun Number.plus(e: Expression) = e + this
operator fun Number.minus(e: Expression) = -e + this
operator fun Number.times(e: Expression) = e * this

fun Expression.pow(n: Int) =
    (2..n).fold(this) { r, _ -> r * this }

// 表达式集求和

fun Sequence<Expression>.sum() = multinomial(this)
fun Iterable<Expression>.sum() = multinomial(this)
fun Array<Expression>.sum() = multinomial(*this)

fun <T> Sequence<T>.sumBy(block: (T) -> Expression) = multinomial(map(block))
fun <T> Iterable<T>.sumBy(block: (T) -> Expression) = multinomial(map(block))
fun <T> Array<T>.sumBy(block: (T) -> Expression) = multinomial(map(block))
