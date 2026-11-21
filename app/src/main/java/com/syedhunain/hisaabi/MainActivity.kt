package com.syedhunain.hisaabi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.syedhunain.hisaabi.ui.BottomNav
import com.syedhunain.hisaabi.ui.EntryScreen
import com.syedhunain.hisaabi.ui.HisaabiTheme
import com.syedhunain.hisaabi.ui.HomeScreen
import com.syedhunain.hisaabi.ui.KhataFormScreen
import com.syedhunain.hisaabi.ui.KhataScreen
import com.syedhunain.hisaabi.ui.LocalStr
import com.syedhunain.hisaabi.ui.LockScreen
import com.syedhunain.hisaabi.ui.ReportsScreen
import com.syedhunain.hisaabi.ui.SettingsScreen
import com.syedhunain.hisaabi.ui.Str
import com.syedhunain.hisaabi.ui.Tab

sealed interface Screen {
    data object Home : Screen
    data object Reports : Screen
    data object Settings : Screen
    data class KhataDetail(val id: Long) : Screen
    data class KhataForm(val id: Long?) : Screen
    data class EntryForm(val khataId: Long, val gave: Boolean, val txnId: Long?) : Screen
}

class MainActivity : ComponentActivity() {
    internal val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HisaabiTheme(dark = vm.prefs.dark) {
                CompositionLocalProvider(LocalStr provides Str(vm.prefs.romanUrdu)) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                        contentColor = MaterialTheme.colorScheme.onBackground,
                    ) {
                        App(vm)
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Ask for the PIN again if the app was in the background for more than 30 seconds.
        if (vm.lastStoppedAt > 0 && System.currentTimeMillis() - vm.lastStoppedAt > 30_000) {
            vm.unlocked = false
        }
    }

    override fun onStop() {
        super.onStop()
        vm.lastStoppedAt = System.currentTimeMillis()
    }
}

@Composable
fun App(vm: AppViewModel) {
    if (vm.prefs.pinHash.isNotEmpty() && !vm.unlocked) {
        LockScreen(check = { vm.prefs.checkPin(it) }, onUnlocked = { vm.unlocked = true })
        return
    }

    val stack = remember { mutableStateListOf<Screen>(Screen.Home) }
    val current = stack.last()
    val depth = stack.size

    fun push(s: Screen) {
        stack.add(s)
    }

    fun pop() {
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    fun replaceTop(s: Screen) {
        stack[stack.lastIndex] = s
    }

    fun tab(t: Tab) {
        val target = when (t) {
            Tab.KHATAY -> Screen.Home
            Tab.REPORTS -> Screen.Reports
            Tab.SETTINGS -> Screen.Settings
        }
        stack.clear()
        stack.add(target)
    }

    BackHandler(enabled = depth > 1 || current != Screen.Home) {
        if (depth > 1) pop() else tab(Tab.KHATAY)
    }

    AnimatedContent(
        targetState = current to depth,
        transitionSpec = {
            val from = initialState.second
            val to = targetState.second
            when {
                to > from -> (slideInHorizontally(tween(300)) { it / 3 } + fadeIn(tween(300))) togetherWith
                    (slideOutHorizontally(tween(300)) { -it / 8 } + fadeOut(tween(200)))
                to < from -> (slideInHorizontally(tween(300)) { -it / 8 } + fadeIn(tween(300))) togetherWith
                    (slideOutHorizontally(tween(300)) { it / 3 } + fadeOut(tween(200)))
                else -> fadeIn(tween(220)) togetherWith fadeOut(tween(180))
            }
        },
        label = "nav",
    ) { (screen, _) ->
        when (screen) {
            Screen.Home -> HomeScreen(
                vm = vm,
                onOpen = { push(Screen.KhataDetail(it)) },
                onNew = { push(Screen.KhataForm(null)) },
                bottomBar = { BottomNav(Tab.KHATAY) { tab(it) } },
            )
            Screen.Reports -> ReportsScreen(
                vm = vm,
                onOpenKhata = { push(Screen.KhataDetail(it)) },
                bottomBar = { BottomNav(Tab.REPORTS) { tab(it) } },
            )
            Screen.Settings -> SettingsScreen(
                vm = vm,
                bottomBar = { BottomNav(Tab.SETTINGS) { tab(it) } },
            )
            is Screen.KhataDetail -> KhataScreen(
                vm = vm,
                khataId = screen.id,
                onBack = { pop() },
                onEdit = { push(Screen.KhataForm(screen.id)) },
                onEntry = { gave, txnId -> push(Screen.EntryForm(screen.id, gave, txnId)) },
            )
            is Screen.KhataForm -> KhataFormScreen(
                vm = vm,
                khataId = screen.id,
                onBack = { pop() },
                onSaved = { id, isNew ->
                    if (isNew) replaceTop(Screen.KhataDetail(id)) else pop()
                },
            )
            is Screen.EntryForm -> EntryScreen(
                vm = vm,
                khataId = screen.khataId,
                initialGave = screen.gave,
                txnId = screen.txnId,
                onBack = { pop() },
            )
        }
    }
}
