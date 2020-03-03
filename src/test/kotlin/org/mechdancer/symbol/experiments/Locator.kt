package org.mechdancer.symbol.experiments

import org.mechdancer.algebra.function.vector.plus
import org.mechdancer.algebra.function.vector.times
import org.mechdancer.algebra.implement.vector.Vector3D
import org.mechdancer.algebra.implement.vector.vector3D
import org.mechdancer.symbol.*
import org.mechdancer.symbol.core.Constant
import org.mechdancer.symbol.core.Constant.Companion.`-1`
import org.mechdancer.symbol.core.Expression
import org.mechdancer.symbol.core.FunctionExpression
import org.mechdancer.symbol.core.Variable
import org.mechdancer.symbol.experiments.Locator.State.Preparing
import org.mechdancer.symbol.experiments.Locator.State.Working
import org.mechdancer.symbol.linear.ExpressionVector
import org.mechdancer.symbol.optimize.dampingNewton
import kotlin.math.ln

class Locator(beacons: List<Vector3D>) {
    enum class State { Preparing, Working }

    var state = Preparing
        private set

    private var last = vector3D(0, 0, -3)
    private val struct = beacons.map { (space.ordinaryField - it.toPoint()).length() }

    operator fun invoke(list: List<Double>): Vector3D? {
        val error = struct
                        .zip(list) { e, l -> if (l > 0) (e - l) `^` 2 else `-1` }
                        .filterIsInstance<FunctionExpression>()
                        .takeIf { it.size >= 3 }
                        ?.let { it.sum() / it.size }
                    ?: return run { state = Preparing; null }
        val f = dampingNewton(error, space)

        val init = last.toPoint()
        val (result, _) = recurrence(init to .0) { (p, _) -> f(p) }
            .firstOrLast { (_, s) -> s < 5e-6 }
        val new = 3 - ln(error[result])
        val old = 3 - ln(error[init])
        val p = result.toVector().run { if (z > 0) copy(z = -this.z) else this }
        when (state) {
            Preparing -> {
                last = when {
                    new > 6   -> {
                        state = Working
                        (old / new).let { k -> last * k + p * (1 - k) }
                    }
                    new > old -> (old / new).let { k -> last * k + p * (1 - k) }.copy(z = -3.0)
                    else      -> last
                }
            }
            Working   ->
                if (new > old) last = (old / new).let { k -> last * k + p * (1 - k) }
        }
        return p
    }

    private companion object {
        private val vx = Variable("x")
        private val vy = Variable("y")
        private val vz = Variable("z")
        private val space = variables("x", "y", "z")

        private fun Vector3D.toPoint() =
            ExpressionVector(mapOf(vx to Constant(x),
                                   vy to Constant(y),
                                   vz to Constant(z)))

        private fun ExpressionVector.toVector() =
            vector3D(this[vx]!!.toDouble(),
                     this[vy]!!.toDouble(),
                     this[vz]!!.toDouble())

        private operator fun Expression.get(values: ExpressionVector) =
            substitute(values).toDouble()
    }
}
