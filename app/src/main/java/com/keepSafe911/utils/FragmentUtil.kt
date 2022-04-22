import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.keepSafe911.R

const val fragmentContainerId = R.id.frame_container

fun FragmentManager.inTransaction(func: FragmentTransaction.() -> FragmentTransaction) {
    beginTransaction().func().commitAllowingStateLoss()
}

fun AppCompatActivity.pushFragment(
    fragment: Fragment, addToBackStack: Boolean = false, ignoreIfCurrent: Boolean,
    justAdd: Boolean = true, animationType: AnimationType?
) {

    val currentFragment = supportFragmentManager.findFragmentById(fragmentContainerId)

    if (ignoreIfCurrent && currentFragment != null) {
        if (fragment.javaClass.canonicalName == currentFragment.tag) {
            return
        }
    }

    supportFragmentManager.inTransaction {

        when (animationType) {
            AnimationType.none -> {
                // Do Nothing
            }
            AnimationType.rightInLeftOut ->
                setCustomAnimations(R.anim.right_in, R.anim.zoom_exit, R.anim.zoom_enter, R.anim.left_out)
            AnimationType.fadeInfadeOut ->
                setCustomAnimations(
                    android.R.anim.fade_in,
                    android.R.anim.fade_out,
                    android.R.anim.fade_in,
                    android.R.anim.fade_out
                )
            AnimationType.leftInfadeOut ->
                setCustomAnimations(R.anim.left_in, R.anim.left_out, R.anim.right_in, R.anim.right_out)
            AnimationType.noneInLeftOut ->
                setCustomAnimations(0, 0, android.R.anim.fade_in, R.anim.left_out)
            AnimationType.bottomtotop ->
                setCustomAnimations(
                    R.anim.bottom_to_top_enter, 0,
                    R.anim.top_to_bottom_enter, 0
                )
        }

        if (currentFragment != null) {
            hide(currentFragment)
        }

        if (addToBackStack) {
            addToBackStack(fragment.javaClass.canonicalName)
        }

        if (justAdd) {
            add(fragmentContainerId, fragment, fragment.javaClass.canonicalName)
        } else {
            replace(fragmentContainerId, fragment, fragment.javaClass.canonicalName)
        }
    }

    hideKeyboard()
}


fun AppCompatActivity.addFragment(
    fragment: Fragment, addToBackStack: Boolean = false, ignoreIfCurrent: Boolean = true,
    animationType: AnimationType = AnimationType.none
) {
    pushFragment(fragment, addToBackStack, ignoreIfCurrent, animationType = animationType)
}

fun AppCompatActivity.replaceFragment(
    fragment: Fragment, ignoreIfCurrent: Boolean = true,
    animationType: AnimationType = AnimationType.none
) {
    pushFragment(fragment, ignoreIfCurrent = ignoreIfCurrent, justAdd = false, animationType = animationType)
}

fun AppCompatActivity.clearBackStack() {
    supportFragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    val fragmentList = supportFragmentManager.fragments
    if (fragmentList != null && !fragmentList.isEmpty()) {
        fragmentList.filterNotNull().forEach { supportFragmentManager.inTransaction { remove(it) } }
    }
}

fun AppCompatActivity.currentFragment(): Fragment {
    return supportFragmentManager.findFragmentById(R.id.frame_container)!!

}