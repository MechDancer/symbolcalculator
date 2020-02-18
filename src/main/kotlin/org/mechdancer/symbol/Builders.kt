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
    private val range: IntRange)
    : ReadOnlyProperty<Any?, VariableSpace> {
    override fun getValue(thisRef: Any?, property: KProperty<*>) =
        when {
            names.isEmpty() -> range.map { "${property.name}$it" }
            else            -> names.flatMap { name -> range.map { i -> "$name$i" } }
        }.map(::Variable).toSet().let(::VariableSpace)
}

fun variableSpace(names: Set<String> = emptySet(), range: IntRange) =
    VariableSpaceProperty(names, range)

fun variables(vararg names: String) =
    VariableSpace(names.map(::Variable).toSet())

fun point(vararg pairs: Pair<String, Number>) =
    Field(pairs.associate { (v, x) -> Variable(v) to Constant(x.toDouble()) })
