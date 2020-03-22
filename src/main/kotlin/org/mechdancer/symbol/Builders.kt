package org.mechdancer.symbol

import org.mechdancer.algebra.implement.vector.Vector2D
import org.mechdancer.algebra.implement.vector.Vector3D
import org.mechdancer.symbol.core.Constant
import org.mechdancer.symbol.core.Variable
import org.mechdancer.symbol.core.VariableSpace
import org.mechdancer.symbol.linear.NamedExpressionVector
import java.util.*
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

fun point(vararg pairs: Pair<Variable, Number>) =
    NamedExpressionVector(pairs.associate { (v, x) -> v to Constant(x.toDouble()) })

// 变量收集器

fun variableCollector() = sortedSetOf<Variable>()

fun SortedSet<Variable>.toSpace() = VariableSpace(toList())

fun Vector2D.toExpression(
    vx: Variable = Variable("x"),
    vy: Variable = Variable("y")
) =
    NamedExpressionVector(mapOf(vx to Constant(x),
                                vy to Constant(y)))

fun Vector3D.toExpression(
    vx: Variable = Variable("x"),
    vy: Variable = Variable("y"),
    vz: Variable = Variable("z")
) =
    NamedExpressionVector(mapOf(vx to Constant(x),
                                vy to Constant(y),
                                vz to Constant(z)))

fun NamedExpressionVector.toVector2D(
    x: Variable = Variable("x"),
    y: Variable = Variable("y")
) =
    Vector2D(expressions.getValue(x).toDouble(),
             expressions.getValue(y).toDouble())

fun NamedExpressionVector.toVector3D(
    x: Variable = Variable("x"),
    y: Variable = Variable("y"),
    z: Variable = Variable("z")
) =
    Vector3D(expressions.getValue(x).toDouble(),
             expressions.getValue(y).toDouble(),
             expressions.getValue(z).toDouble())
