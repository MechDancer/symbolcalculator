package org.mechdancer.symbol.system

import org.mechdancer.symbol.core.Variable
import org.mechdancer.symbol.linear.ExpressionVector

/**
 * 标签结构
 *
 * @param id 序号
 * @param state 状态码，使用最后一次移动的时间戳，纯粹的固定标签取 -1
 */
data class Beacon(
    val id: Int,
    val state: Long = -1
) : Comparable<Beacon> {
    fun toVector() =
        prefix
            .associateWith { Variable("$it$id${if (state < 0) "" else "_$state"}") }
            .let(::ExpressionVector)

    override fun compareTo(other: Beacon) =
        state.compareTo(other.state)
            .takeIf { it != 0 }
        ?: id.compareTo(other.id)

    private companion object {
        val prefix = listOf("x", "y", "z").map(::Variable)
    }
}
