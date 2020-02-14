package org.mechdancer.v2

import org.mechdancer.v2.Product.Builders.product
import org.mechdancer.v2.Sum.Builders.sum
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

// 求导

inline class DVariable(val variable: Variable) {
    companion object {
        fun d(v: Variable) = DVariable(v)
    }
}

inline class DExpression(val expression: Expression) {
    operator fun div(v: DVariable) =
        expression.d(v.variable)

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

operator fun Expression.plus(others: Expression) = sum(this, others)
operator fun Expression.times(others: Expression) = product(this, others)

operator fun Expression.plus(n: Number) = this + Constant(n.toDouble())
operator fun Expression.minus(n: Number) = this + Constant(-n.toDouble())
operator fun Expression.times(n: Number) = this * Constant(n.toDouble())
operator fun Expression.div(n: Number) = this * Constant(1 / n.toDouble())

operator fun Expression.unaryMinus() = this * -1

operator fun Number.plus(e: Expression) = e + this
operator fun Number.minus(e: Expression) = -e + this
operator fun Number.times(e: Expression) = e * this

fun Expression.pow(n: Int) =
    (2..n).fold(this) { r, _ -> r * this }

// 求和求积

fun Sequence<Expression>.sum() = sum(this)
fun Iterable<Expression>.sum() = sum(this)
fun Array<Expression>.sum() = sum(*this)

fun Sequence<Expression>.product() = product(this)
fun Iterable<Expression>.product() = product(this)
fun Array<Expression>.product() = product(*this)

fun <T> Sequence<T>.sumBy(block: (T) -> Expression) = sum(map(block))
fun <T> Iterable<T>.sumBy(block: (T) -> Expression) = sum(map(block))
fun <T> Array<T>.sumBy(block: (T) -> Expression) = sum(map(block))

fun <T> Sequence<T>.productBy(block: (T) -> Expression) = product(map(block))
fun <T> Iterable<T>.productBy(block: (T) -> Expression) = product(map(block))
fun <T> Array<T>.productBy(block: (T) -> Expression) = product(map(block))
