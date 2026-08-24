package com.example.mathapplock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class Grade8MathEngineTest {

    private val testStringProvider = Grade8MathEngine.StringProvider { resId, args ->
        when (resId) {
            R.string.question_linear -> "linear:${args[0]}:${args[1]}"
            R.string.question_exponent -> "exponent:${args[0]}:${args[1]}"
            R.string.question_square_root -> "square_root:${args[0]}"
            R.string.question_geometry_angle -> "geom_angle:${args[0]}:${args[1]}"
            R.string.question_geometry_leg -> "geom_leg:${args[0]}:${args[1]}"
            R.string.question_geometry_hypotenuse -> "geom_hypo:${args[0]}:${args[1]}"
            else -> "unknown:$resId"
        }
    }

    private val engine = Grade8MathEngine(testStringProvider)

    @Test
    fun testLinearEquationGeneration() {
        for (i in 1..1000) {
            val question = engine.generateLinearEquation()
            val text = question.questionText
            val answer = question.correctAnswer

            // Verify x is non-zero and in [-10, 10]
            assertTrue("x should be between -10 and 10", answer in -10..10)
            assertTrue("x should be non-zero", answer != 0)

            // Verify question text starts with "linear:"
            assertTrue("Question text should be formatted as linear:LHS:c", text.startsWith("linear:"))

            // Parse LHS and c
            val parts = text.split(":")
            val lhs = parts[1]
            val c = parts[2].toInt()

            // LHS is expected to be e.g. "aX + b" or "aX - b"
            // Let's parse a and b
            val lhsParts = lhs.split(" ")
            val aText = lhsParts[0].removeSuffix("x")
            val a = aText.toInt()
            val op = lhsParts[1]
            val b = lhsParts[2].toInt()
            val signedB = if (op == "+") b else -b

            // Check coefficient constraints
            assertTrue("a should be between -10 and 10", a in -10..10)
            assertTrue("a should not be 0, -1, or 1", a != 0 && a != -1 && a != 1)

            // Check offset constraints
            assertTrue("b should be between -20 and 20", signedB in -20..20)
            assertTrue("b should be non-zero", signedB != 0)

            // Verify calculation: ax + b = c
            assertEquals("a * x + b should equal c", c, a * answer + signedB)
        }
    }

    @Test
    fun testBasicExponentGeneration() {
        for (i in 1..1000) {
            val question = engine.generateBasicExponent()
            val text = question.questionText
            val answer = question.correctAnswer

            assertTrue(text.startsWith("exponent:"))
            val parts = text.split(":")
            val x = parts[1].toInt()
            val y = parts[2].toInt()

            // Verify x and y bounds
            assertTrue("Base x should be >= 2", x >= 2)
            assertTrue("Exponent y should be >= 2", y >= 2)

            // Verify value matches answer
            var expectedAnswer = 1
            for (j in 1..y) {
                expectedAnswer *= x
            }
            assertEquals("Calculated exponent should match correctAnswer", expectedAnswer, answer)

            // Verify result is less than 500
            assertTrue("Result x^y should be less than 500", answer < 500)
        }
    }

    @Test
    fun testSquareRootGeneration() {
        for (i in 1..1000) {
            val question = engine.generateSquareRoot()
            val text = question.questionText
            val answer = question.correctAnswer

            assertTrue(text.startsWith("square_root:"))
            val parts = text.split(":")
            val perfectSquare = parts[1].toInt()

            // Verify bounds
            assertTrue("Answer should be between 1 and 12", answer in 1..12)
            assertEquals("Perfect square should be answer squared", perfectSquare, answer * answer)
            assertTrue("Perfect square should be <= 144", perfectSquare <= 144)
        }
    }

    @Test
    fun testGeometryGeneration() {
        for (i in 1..1000) {
            val question = engine.generateGeometryQuestion()
            val text = question.questionText
            val answer = question.correctAnswer

            if (text.startsWith("geom_angle:")) {
                val parts = text.split(":")
                val a = parts[1].toInt()
                val b = parts[2].toInt()

                assertTrue("Angle a should be >= 15", a >= 15)
                assertTrue("Angle b should be >= 15", b >= 15)
                assertTrue("Third angle answer should be > 0", answer > 0)
                assertEquals("Sum of angles should be 180", 180, a + b + answer)
            } else if (text.startsWith("geom_hypo:")) {
                val parts = text.split(":")
                val leg1 = parts[1].toInt()
                val leg2 = parts[2].toInt()

                // Answer is hypotenuse
                assertEquals("Pythagorean theorem should hold", answer * answer, leg1 * leg1 + leg2 * leg2)
            } else if (text.startsWith("geom_leg:")) {
                val parts = text.split(":")
                val hypo = parts[1].toInt()
                val leg1 = parts[2].toInt()

                // Answer is other leg
                assertEquals("Pythagorean theorem should hold", hypo * hypo, leg1 * leg1 + answer * answer)
            } else {
                assertTrue("Unknown geometry question type: $text", false)
            }
        }
    }

    @Test
    fun testRandomQuestionTypeSupport() {
        var hasLinear = false
        var hasExponent = false
        var hasSquareRoot = false
        var hasGeometry = false

        for (i in 1..200) {
            val question = engine.generateRandomQuestion()
            val text = question.questionText
            when {
                text.startsWith("linear:") -> hasLinear = true
                text.startsWith("exponent:") -> hasExponent = true
                text.startsWith("square_root:") -> hasSquareRoot = true
                text.startsWith("geom_") -> hasGeometry = true
            }
        }

        assertTrue("Should have generated linear questions", hasLinear)
        assertTrue("Should have generated exponent questions", hasExponent)
        assertTrue("Should have generated square root questions", hasSquareRoot)
        assertTrue("Should have generated geometry questions", hasGeometry)
    }
}
