package org.mechdancer.v2

import org.mechdancer.v2.Power.Builders.pow

inline class Variable(private val name: String) {
    fun toPower() = pow(this, Constant(1.0))
    override fun toString() = name
}
