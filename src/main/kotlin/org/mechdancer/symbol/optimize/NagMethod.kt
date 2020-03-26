package org.mechdancer.symbol.optimize

import org.mechdancer.algebra.core.Vector
import org.mechdancer.algebra.function.vector.minus
import org.mechdancer.algebra.function.vector.plus
import org.mechdancer.algebra.function.vector.times
import org.mechdancer.algebra.implement.vector.listVectorOfZero

/** NAG 方法 := 增益 + 低通滤波 + PD 控制 */
@Suppress("NonAsciiCharacters")
class NagMethod(
    dim: Int,
    private val η: Double,
    private val γ: Double
) : LinearController {
    override val dim get() = memory.dim
    private var memory = listVectorOfZero(dim)
    override fun invoke(signal: Vector) =
        (memory * γ + signal * η).let {
            val d = it - memory
            memory = it
            it + d * γ
        }
}
