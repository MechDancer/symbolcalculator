package org.mechdancer.symbol

import org.mechdancer.symbol.core.Constant
import org.mechdancer.symbol.core.Expression
import org.mechdancer.symbol.core.Variable
import org.mechdancer.symbol.linear.ExpressionVector
import org.mechdancer.symbol.linear.VariableSpace
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

// 定义变量

class VariableProperty
    : ReadOnlyProperty<Any?, Variable> {
    override fun getValue(thisRef: Any?, property: KProperty<*>) =
        Variable(property.name)
}

val variable
    get() = VariableProperty()

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

fun point(vararg pairs: Pair<String, Number>) =
    ExpressionVector(pairs.associate { (v, x) ->
        Variable(v) to Constant(x.toDouble())
    })

fun field(vararg pairs: Pair<String, Expression>) =
    ExpressionVector(pairs.associate { (v, e) -> Variable(v) to e })
