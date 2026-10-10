package lv.zarin.timekeep.domain.control

enum class Control { PAUSE, ADD_MINUTE, EDIT, DELETE }

fun interface ControlPolicy {
    fun isAllowed(control: Control): Boolean
}

object AllowAllControlPolicy : ControlPolicy {
    override fun isAllowed(control: Control): Boolean = true
}
