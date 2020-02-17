package org.mechdancer.symbol

class Field(private val expressions: Map<Variable, Expression>) {
    val dim get() = expressions.size
    operator fun get(v: Variable) = expressions[v]

    val length by lazy {
        expressions.entries.sumBy { (_, e) -> Power[e, Constant(2.0)] }
    }

    override fun toString() = expressions.toString()
}
