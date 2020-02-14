package org.mechdancer.v2

import org.mechdancer.v2.Power.Builders.pow
import org.mechdancer.v2.Product.Builders.product
import org.mechdancer.v2.Sum.Builders.sum
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

// 求导

inline class DVariable(val variable: Variable)

inline class DExpression(val expression: Expression) {
    operator fun div(v: DVariable) = expression.d(v.variable)
}

fun d(e: Expression) = DExpression(e)
fun d(e: Variable) = DVariable(e)

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

fun Number.toConstant() = Constant(toDouble())
fun Variable.pow(n: Number) = pow(this, n.toConstant())
operator fun Expression.unaryMinus() = product(this, (-1).toConstant())

operator fun Expression.plus(others: Expression) = sum(this, others)
operator fun Expression.minus(others: Expression) = sum(this, -others)
operator fun Expression.times(others: Expression) = product(this, others)

operator fun Expression.plus(n: Number) = this + n.toConstant()
operator fun Expression.minus(n: Number) = this - n.toConstant()
operator fun Expression.times(n: Number) = this * n.toConstant()
operator fun Expression.div(n: Number) = this * Constant(1 / n.toDouble())

operator fun Expression.plus(v: Variable) = this + v.toPower()
operator fun Expression.minus(v: Variable) = this + v.toPower()
operator fun Expression.times(v: Variable) = this * v.toPower()
operator fun Expression.div(v: Variable) = this * v.toPower()

operator fun Number.plus(e: Expression) = toConstant() + e
operator fun Number.minus(e: Expression) = toConstant() - e
operator fun Number.times(e: Expression) = toConstant() * e

operator fun Number.plus(v: Variable) = toConstant() + v.toPower()
operator fun Number.minus(v: Variable) = toConstant() - v.toPower()
operator fun Number.times(v: Variable) = toConstant() * v.toPower()
operator fun Number.div(v: Variable) = toConstant() * v.pow(-1)

operator fun Variable.plus(e: Expression) = toPower() + e
operator fun Variable.minus(e: Expression) = toPower() - e
operator fun Variable.times(e: Expression) = toPower() * e

operator fun Variable.plus(n: Number) = toPower() + n
operator fun Variable.minus(n: Number) = toPower() - n
operator fun Variable.times(n: Number) = toPower() * n
operator fun Variable.div(n: Number) = toPower() / n

operator fun Variable.plus(others: Variable) = toPower() + others
operator fun Variable.minus(others: Variable) = toPower() - others
operator fun Variable.times(others: Variable) = toPower() * others
operator fun Variable.div(others: Variable) = toPower() / others

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
