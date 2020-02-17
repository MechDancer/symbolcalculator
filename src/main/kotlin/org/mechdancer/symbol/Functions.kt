package org.mechdancer.symbol

import org.mechdancer.symbol.Constant.Companion.`-1`
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

// 求导

inline class DExpression(private val expression: Expression) {
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

    operator fun set(from: Expression, to: Expression) {
        expression = expression.substitute(from, to)
    }

    operator fun set(from: Expression, x: Number) {
        expression = expression.substitute(from, Constant(x.toDouble()))
    }
}

fun Expression.substitute(block: ValueCalculator.() -> Unit) =
    ValueCalculator(this).apply(block).expression

// 定义变量

class VariableProperty
    : ReadOnlyProperty<Any?, Variable> {
    override fun getValue(thisRef: Any?, property: KProperty<*>) =
        Variable(property.name)
}

val variable
    get() = VariableProperty()

class VariableVariableSpaceProperty(
    private val names: Set<String>,
    private val range: IntRange)
    : ReadOnlyProperty<Any?, VariableSpace> {
    override fun getValue(thisRef: Any?, property: KProperty<*>) =
        when {
            names.isEmpty() -> range.map { "${property.name}$it" }
            else            -> names.flatMap { name -> range.map { i -> "$name$i" } }
        }.map(::Variable).toSet().let(::VariableSpace)
}

fun variableSpace(names: Set<String> = emptySet(), range: IntRange) =
    VariableVariableSpaceProperty(names, range)

fun variables(vararg names: String) =
    VariableSpace(names.map(::Variable).toSet())

// 表达式运算

operator fun Expression.unaryMinus() = this * `-1`
fun Expression.reciprocal() = Power[this, `-1`]
fun sqrt(e: Expression) = Power[e, Constant(.5)]

operator fun Expression.plus(others: Expression) = Sum[this, others]
operator fun Expression.minus(others: Expression) = Sum[this, -others]
operator fun Expression.times(others: Expression) = Product[this, others]
operator fun Expression.div(others: Expression) = Product[this, others.reciprocal()]

operator fun Expression.plus(n: Number) = this + Constant(n.toDouble())
operator fun Expression.minus(n: Number) = this - Constant(n.toDouble())
operator fun Expression.times(n: Number) = this * Constant(n.toDouble())
operator fun Expression.div(n: Number) = this / Constant(n.toDouble())

operator fun Number.plus(e: Expression) = e + this
operator fun Number.minus(e: Expression) = -e + this
operator fun Number.times(e: Expression) = e * this
operator fun Number.div(e: Expression) = e.reciprocal() * this

infix fun Expression.pow(n: Number) =
    if (n is Int && n < 5) Product[List(n) { this }] else Power[this, Constant(n.toDouble())]

infix fun Expression.pow(n: Constant) =
    pow((n.value.toInt() as Number).takeIf { it.toDouble() == n.value } ?: n.value)

infix fun Constant.pow(e: Expression) = Exponential[this, e]
infix fun Number.pow(e: Expression) = Exponential[Constant(toDouble()), e]

fun ln(e: Expression) = Ln[e]
fun log(base: Constant) = { e: Expression -> Ln[base, e] }
fun log(base: Number) = log(Constant(base.toDouble()))

// 求和求积

fun Sequence<Expression>.sum() = Sum[toList()]
fun Iterable<Expression>.sum() = Sum[toList()]
fun Collection<Expression>.sum() = Sum[this]
fun Array<Expression>.sum() = Sum[asList()]

fun Sequence<Expression>.product() = Product[toList()]
fun Iterable<Expression>.product() = Product[toList()]
fun Collection<Expression>.product() = Product[this]
fun Array<Expression>.product() = Product[asList()]

fun <T> Sequence<T>.sumBy(block: (T) -> Expression) = Sum[map(block).toList()]
fun <T> Iterable<T>.sumBy(block: (T) -> Expression) = Sum[map(block)]
fun <T> Array<T>.sumBy(block: (T) -> Expression) = Sum[map(block)]

fun <T> Sequence<T>.productBy(block: (T) -> Expression) = Product[map(block).toList()]
fun <T> Iterable<T>.productBy(block: (T) -> Expression) = Product[map(block)]
fun <T> Array<T>.productBy(block: (T) -> Expression) = Product[map(block)]
