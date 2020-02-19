package org.mechdancer.symbol

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
        val post = range.map(Any::toString).toList().takeUnless(Collection<*>::isEmpty) ?: emptyList()

        return pre.flatMap { name -> post.map { i -> "$name$i" } }
            .map(::Variable).toSet().let(::VariableSpace)
    }
}

fun variableSpace(vararg names: String, indices: Iterable<Any> = emptyList()) =
    VariableSpaceProperty(names.toSet(), indices)

fun variables(vararg names: String) =
    VariableSpace(names.map(::Variable).toSet())

fun point(vararg pairs: Pair<String, Number>) =
    Field(pairs.associate { (v, x) -> Variable(v) to Constant(x.toDouble()) })
