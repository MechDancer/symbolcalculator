package org.mechdancer.symbol

import org.mechdancer.symbol.Constant.Companion.`-1`
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

// 求导

inline class DExpression(val expression: Expression) {
    operator fun div(v: DExpression) =
        (v.expression as? Variable)
            ?.let { expression d it }
        ?: throw UnsupportedOperationException()
}

fun d(e: Expression) = DExpression(e)

// 代入

class ValueCalculator internal constructor(e: Expression) {
    var expression = e
        private set

    operator fun set(v: Variable, x: Expression) {
        expression = expression.substitute(v, x)
    }

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

operator fun Expression.unaryMinus() = this * `-1`

operator fun Expression.plus(others: Expression) = Sum[this, others]
operator fun Expression.minus(others: Expression) = Sum[this, -others]
operator fun Expression.times(others: Expression) = Product[this, others]
operator fun Expression.div(others: Expression) = Product[this, Power[others, `-1`]]

operator fun Expression.plus(n: Number) = this + Constant(n.toDouble())
operator fun Expression.minus(n: Number) = this - Constant(n.toDouble())
operator fun Expression.times(n: Number) = this * Constant(n.toDouble())
operator fun Expression.div(n: Number) = this * Constant(1 / n.toDouble())

operator fun Number.plus(e: Expression) = e + this
operator fun Number.minus(e: Expression) = -e + this
operator fun Number.times(e: Expression) = e * this
operator fun Number.div(e: Expression) = Power[e, `-1`] * this

fun Expression.pow(n: Int) =
    (2..n).fold(this) { r, _ -> r * this }

// 求和求积

fun Sequence<Expression>.sum() = Sum[toList()]
fun Iterable<Expression>.sum() = Sum[toList()]
fun Array<Expression>.sum() = Sum[asList()]

fun Sequence<Expression>.product() = Product[toList()]
fun Iterable<Expression>.product() = Product[toList()]
fun Array<Expression>.product() = Product[asList()]

fun <T> Sequence<T>.sumBy(block: (T) -> Expression) = Sum[map(block).toList()]
fun <T> Iterable<T>.sumBy(block: (T) -> Expression) = Sum[map(block)]
fun <T> Array<T>.sumBy(block: (T) -> Expression) = Sum[map(block)]

fun <T> Sequence<T>.productBy(block: (T) -> Expression) = Product[map(block).toList()]
fun <T> Iterable<T>.productBy(block: (T) -> Expression) = Product[map(block)]
fun <T> Array<T>.productBy(block: (T) -> Expression) = Product[map(block)]
