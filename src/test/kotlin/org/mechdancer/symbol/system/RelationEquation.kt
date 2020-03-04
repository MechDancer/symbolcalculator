package org.mechdancer.symbol.system

import org.mechdancer.symbol.core.Constant

/** 关系方程 */
data class RelationEquation(val a: Beacon, val b: Beacon, val distance: Double) {
    fun toExpression() =
        (a.toVector() - b.toVector()).length() - Constant(distance)
}
