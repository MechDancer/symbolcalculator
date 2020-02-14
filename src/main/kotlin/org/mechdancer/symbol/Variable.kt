package org.mechdancer.symbol

import org.mechdancer.symbol.Power.Builders.pow

inline class Variable(private val name: String) {
    fun toPower() = pow(this, Constant(1.0))
    override fun toString() = name
}
