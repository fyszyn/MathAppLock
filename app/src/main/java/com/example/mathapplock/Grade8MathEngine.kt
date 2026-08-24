package com.example.mathapplock

import android.content.Context
import kotlin.math.abs
import kotlin.math.pow
import kotlin.random.Random

class Grade8MathEngine(private val stringProvider: StringProvider) {

    constructor(context: Context) : this(StringProvider { resId, args ->
        context.getString(resId, *args)
    })

    fun interface StringProvider {
        fun getString(resId: Int, vararg formatArgs: Any): String
    }

    /**
     * Generates a random Grade 8 math question from the 4 supported types.
     */
    fun generateRandomQuestion(): MathQuestion {
        return when (Random.nextInt(1, 5)) {
            1 -> generateLinearEquation()
            2 -> generateBasicExponent()
            3 -> generateSquareRoot()
            else -> generateGeometryQuestion()
        }
    }

    /**
     * 1. Linear Equations (e.g., ax + b = c, resolving to a clean integer x)
     */
    fun generateLinearEquation(): MathQuestion {
        // Target answer x: non-zero integer between -10 and 10
        var x = Random.nextInt(-10, 11)
        while (x == 0) {
            x = Random.nextInt(-10, 11)
        }

        // Coefficient a: non-zero integer between -10 and 10, excluding -1 and 1
        var a = Random.nextInt(-10, 11)
        while (a == 0 || a == -1 || a == 1) {
            a = Random.nextInt(-10, 11)
        }

        // Offset b: non-zero integer between -20 and 20
        var b = Random.nextInt(-20, 21)
        while (b == 0) {
            b = Random.nextInt(-20, 21)
        }

        // Compute c = ax + b
        val c = a * x + b

        // Format LHS expression: e.g. "3x + 5" or "3x - 5"
        val bSign = if (b > 0) "+" else "-"
        val bAbs = abs(b)
        val lhs = "${a}x $bSign $bAbs"

        val questionText = stringProvider.getString(R.string.question_linear, lhs, c)
        return MathQuestion(questionText, x)
    }

    /**
     * 2. Basic Exponents (e.g., evaluating x^y where the result is less than 500)
     */
    fun generateBasicExponent(): MathQuestion {
        // Base x between 2 and 22
        val x = Random.nextInt(2, 23)

        // Exponent y selected based on x to guarantee x^y < 500
        val y = when (x) {
            2 -> Random.nextInt(2, 9)       // 2^2 = 4 to 2^8 = 256
            3 -> Random.nextInt(2, 6)       // 3^2 = 9 to 3^5 = 243
            4 -> Random.nextInt(2, 5)       // 4^2 = 16 to 4^4 = 256
            in 5..7 -> Random.nextInt(2, 4) // 5^2=25 to 5^3=125, 6^2=36 to 6^3=216, 7^2=49 to 7^3=343
            else -> 2                       // 8^2 = 64 up to 22^2 = 484
        }

        val answer = x.toDouble().pow(y.toDouble()).toInt()
        val questionText = stringProvider.getString(R.string.question_exponent, x, y)
        return MathQuestion(questionText, answer)
    }

    /**
     * 3. Perfect Squares and Square Roots (e.g., evaluating square root of perfect squares up to 144)
     */
    fun generateSquareRoot(): MathQuestion {
        // Base n between 1 and 12
        val n = Random.nextInt(1, 13)
        val perfectSquare = n * n

        val questionText = stringProvider.getString(R.string.question_square_root, perfectSquare)
        return MathQuestion(questionText, n)
    }

    /**
     * 4. Geometry (missing angle, or side of a right triangle)
     */
    fun generateGeometryQuestion(): MathQuestion {
        return if (Random.nextBoolean()) {
            generateTriangleAngleQuestion()
        } else {
            generatePythagoreanTripleQuestion()
        }
    }

    private fun generateTriangleAngleQuestion(): MathQuestion {
        // Select two angles A, B >= 15 such that A + B <= 165
        val a = Random.nextInt(15, 151)
        val bLimit = 180 - a - 15
        val b = if (bLimit > 15) Random.nextInt(15, bLimit) else 15

        val c = 180 - (a + b)
        val questionText = stringProvider.getString(R.string.question_geometry_angle, a, b)
        return MathQuestion(questionText, c)
    }

    private fun generatePythagoreanTripleQuestion(): MathQuestion {
        // Primitive triples: (leg1, leg2, hypotenuse)
        val primitiveTriples = listOf(
            Triple(3, 4, 5),
            Triple(5, 12, 13),
            Triple(8, 15, 17),
            Triple(7, 24, 25)
        )
        val triple = primitiveTriples[Random.nextInt(primitiveTriples.size)]
        val k = Random.nextInt(1, 5) // scaling factor between 1 and 4

        val a = triple.first * k
        val b = triple.second * k
        val c = triple.third * k

        return if (Random.nextBoolean()) {
            // Ask for hypotenuse (c)
            val questionText = stringProvider.getString(R.string.question_geometry_hypotenuse, a, b)
            MathQuestion(questionText, c)
        } else {
            // Ask for leg2 (b) given hypotenuse (c) and leg1 (a)
            val questionText = stringProvider.getString(R.string.question_geometry_leg, c, a)
            MathQuestion(questionText, b)
        }
    }
}
