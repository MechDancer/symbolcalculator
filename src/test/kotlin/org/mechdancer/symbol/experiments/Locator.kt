package org.mechdancer.symbol.experiments

import org.mechdancer.algebra.function.vector.plus
import org.mechdancer.algebra.function.vector.times
import org.mechdancer.algebra.implement.vector.Vector3D
import org.mechdancer.algebra.implement.vector.vector3D
import org.mechdancer.symbol.*
import org.mechdancer.symbol.core.Constant.Companion.`-1`
import org.mechdancer.symbol.core.Constant.Companion.`-∞`
import org.mechdancer.symbol.core.Constant.Companion.zero
import org.mechdancer.symbol.core.Expression
import org.mechdancer.symbol.core.FunctionExpression
import org.mechdancer.symbol.core.Variable
import org.mechdancer.symbol.experiments.Locator.State.Preparing
import org.mechdancer.symbol.experiments.Locator.State.Working
import org.mechdancer.symbol.linear.ExpressionVector
import org.mechdancer.symbol.linear.VariableSpace.Companion.xyz
import org.mechdancer.symbol.optimize.dampingNewton
import org.mechdancer.symbol.optimize.get
import org.mechdancer.symbol.optimize.optimize
import kotlin.math.ln

class Locator(beacons: List<Vector3D>) {
    enum class State { Preparing, Working }

    var state = Preparing
        private set

    private var last = vector3D(0, 0, -3)
    private val struct = beacons.map { (xyz.ordinaryField - it.toExpression()).length() }

    operator fun invoke(list: List<Double>): Vector3D? {
        val error = struct
                        .zip(list) { e, l -> if (l > 0) (e - l) `^` 2 else `-1` }
                        .filterIsInstance<FunctionExpression>()
                        .takeIf { it.size >= 3 }
                        ?.let { it.sum() / (2 * it.size) }
                    ?: return run { state = Preparing; null }
        val init = last.toExpression()
        val result = optimize(init, Int.MAX_VALUE, 5e-6, dampingNewton(error, xyz, z[`-∞`, zero]))
        val new = 3 - ln(error[result])
        val old = 3 - ln(error[init])
        val p = result.toVector3D()
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
        private val z = Variable("z")
        private operator fun Expression.get(values: ExpressionVector) =
            substitute(values).toDouble()
    }
}
