package org.mechdancer.symbol

import org.mechdancer.symbol.core.Constant
import org.mechdancer.symbol.core.Expression
import org.mechdancer.symbol.core.Variable
import org.mechdancer.symbol.linear.ExpressionVector
import org.mechdancer.symbol.linear.VariableSpace
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

// 定义变量

class VariableProperty(private val collector: MutableSet<Variable>? = null)
    : ReadOnlyProperty<Any?, Variable> {
    private var memory: Variable? = null
    override fun getValue(thisRef: Any?, property: KProperty<*>) =
        memory ?: Variable(property.name).also {
            memory = it
            collector?.plusAssign(it)
        }
}

val variable
    get() = VariableProperty()

fun variable(collector: MutableSet<Variable>) =
    VariableProperty(collector)

// 定义变量空间

class VariableSpaceProperty(
    private val names: Set<String>,
    private val range: Iterable<Any>)
    : ReadOnlyProperty<Any?, VariableSpace> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): VariableSpace {
        val pre = if (names.isEmpty()) listOf(property.name) else names.filterNot(String::isBlank)
        val post = range.map(Any::toString).toList().takeUnless(Collection<*>::isEmpty) ?: listOf("")

        return pre.flatMap { name -> post.map { i -> "$name$i" } }
            .map(::Variable).toSet().let(::VariableSpace)
    }
}

fun variableSpace(vararg names: String, indices: Iterable<Any> = emptyList()) =
    VariableSpaceProperty(names.toSet(), indices)

fun variables(vararg names: String) =
    VariableSpace(names.map(::Variable).toSet())

fun point(vararg pairs: Pair<Variable, Number>) =
    ExpressionVector(pairs.associate { (v, x) -> v to Constant(x.toDouble()) })

fun field(vararg pairs: Pair<Variable, Expression>) =
    ExpressionVector(pairs.associate { (v, e) -> v to e })

// 变量收集器

fun collector() = mutableSetOf<Variable>()

fun MutableSet<Variable>.toSpace() = VariableSpace(this)
