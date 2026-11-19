package com.remgose.rem.core

/** Phien ban Ren'Py dang so sanh duoc, vd 8.3.7 / 7.3.5. */
data class RenPyVersion(val major: Int, val minor: Int, val patch: Int) : Comparable<RenPyVersion> {
    override fun compareTo(other: RenPyVersion): Int =
        compareValuesBy(this, other, { it.major }, { it.minor }, { it.patch })
    override fun toString() = "$major.$minor.$patch"
    val isSeven: Boolean get() = major == 7

    companion object {
        private val RX = Regex("""(\d+)\.(\d+)\.(\d+)""")
        fun parse(text: String?): RenPyVersion? {
            if (text.isNullOrBlank()) return null
            val m = RX.find(text) ?: return null
            return RenPyVersion(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt())
        }
    }
}
