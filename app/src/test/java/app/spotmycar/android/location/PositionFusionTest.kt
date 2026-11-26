package app.spotmycar.android.location

import app.spotmycar.android.location.PositionFusion.Fix
import app.spotmycar.android.location.PositionFusion.Source
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Scénarios vécus par quelqu'un qui retourne à sa voiture (Rabat, ~34° N). */
class PositionFusionTest {
    private val lat = 34.0
    private val lng = -6.8

    private fun north(meters: Float) = PositionFusion.offset(lat, lng, meters, 0f)

    @Test
    fun `GPS precis gagne toujours, meme apres des pas`() {
        val f = PositionFusion()
        f.onFix(Fix(lat, lng, 5f))
        repeat(10) { f.onStep(0f) }
        assertEquals(Source.STEPS, f.current!!.source)
        val (la, ln) = north(3f)
        assertTrue(f.onFix(Fix(la, ln, 6f)))
        assertEquals(Source.GPS, f.current!!.source)
        assertEquals(6f, f.current!!.accuracy, 0.01f)
    }

    @Test
    fun `chaque pas avance de la longueur du pas dans la direction de la boussole`() {
        val f = PositionFusion()
        f.onFix(Fix(lat, lng, 5f))
        repeat(10) { f.onStep(0f) }
        val e = f.current!!
        val walked = PositionFusion.distance(lat, lng, e.latitude, e.longitude)
        assertEquals(7f, walked, 0.05f)
        assertTrue(e.latitude > lat)
        assertEquals(5f + 10 * PositionFusion.DRIFT_PER_STEP_METERS, e.accuracy, 0.01f)
    }

    @Test
    fun `sous terre, une position wifi qui contredit les pas est ignoree`() {
        val f = PositionFusion()
        f.onFix(Fix(lat, lng, 5f))
        repeat(20) { f.onStep(0f) } // 14 m au nord, incertitude 8 m
        val (la, ln) = north(-80f) // 80 m au sud, annoncee a 30 m
        assertFalse(f.onFix(Fix(la, ln, 30f)))
        assertEquals(Source.STEPS, f.current!!.source)
    }

    @Test
    fun `une position plus precise et coherente avec les pas est acceptee`() {
        val f = PositionFusion()
        f.onFix(Fix(lat, lng, 14f))
        repeat(60) { f.onStep(0f) } // ~42 m au nord, incertitude 23 m
        val (la, ln) = north(45f)
        assertTrue(f.onFix(Fix(la, ln, 18f)))
        assertEquals(Source.APPROXIMATE, f.current!!.source)
    }

    @Test
    fun `sans podometre, une position moins precise suit quand meme la marche`() {
        val f = PositionFusion()
        f.onFix(Fix(lat, lng, 5f))
        val (la, ln) = north(20f)
        assertTrue(f.onFix(Fix(la, ln, 40f)))
        assertFalse(f.onFix(Fix(la, ln, 90f))) // inutilisable : on garde mieux
    }

    @Test
    fun `quand les pas ont trop derive, une position vague reprend la main`() {
        val f = PositionFusion()
        f.onFix(Fix(lat, lng, 10f))
        repeat(400) { f.onStep(0f) } // incertitude 70 m : perdu
        assertFalse(f.current!!.usable)
        val (la, ln) = north(500f)
        assertTrue(f.onFix(Fix(la, ln, 50f)))
        assertTrue(f.current!!.usable)
    }

    @Test
    fun `position perimee ignoree`() {
        val f = PositionFusion()
        assertFalse(f.onFix(Fix(lat, lng, 5f, ageMillis = 30_000L)))
        assertNull(f.current)
    }

    @Test
    fun `derniere position connue sert de depart, vieillie selon son age`() {
        val f = PositionFusion()
        assertTrue(f.onLastKnown(Fix(lat, lng, 10f, ageMillis = 60_000L)))
        assertEquals(10f + 0.3f * 60, f.current!!.accuracy, 0.01f)
        assertTrue(f.onStep(90f)) // les pas peuvent partir de la
        val old = PositionFusion()
        assertFalse(old.onLastKnown(Fix(lat, lng, 10f, ageMillis = 5 * 60_000L)))
        assertNull(old.current)
    }

    @Test
    fun `la derniere position connue ne remplace pas une position fraiche`() {
        val f = PositionFusion()
        f.onFix(Fix(lat, lng, 5f))
        assertFalse(f.onLastKnown(Fix(lat + 0.01, lng, 5f, ageMillis = 1_000L)))
    }

    @Test
    fun `la longueur du pas est mesuree dehors avec le GPS`() {
        val f = PositionFusion()
        f.onFix(Fix(lat, lng, 5f))
        repeat(50) { f.onStep(0f) }
        val (la, ln) = north(40f) // 40 m en 50 pas = 0,8 m
        f.onFix(Fix(la, ln, 5f))
        assertEquals(0.7f * 0.7f + 0.8f * 0.3f, f.stride, 0.01f)
    }

    @Test
    fun `une mesure de pas aberrante est ignoree`() {
        val f = PositionFusion()
        f.onFix(Fix(lat, lng, 5f))
        repeat(30) { f.onStep(0f) }
        val (la, ln) = north(60f) // 2 m par pas : le GPS a saute
        f.onFix(Fix(la, ln, 5f))
        assertEquals(PositionFusion.DEFAULT_STRIDE, f.stride, 0.001f)
    }

    @Test
    fun `sans point de depart, un pas ne fait rien`() {
        val f = PositionFusion()
        assertFalse(f.onStep(0f))
        assertNotNull(f.stride)
    }
}
