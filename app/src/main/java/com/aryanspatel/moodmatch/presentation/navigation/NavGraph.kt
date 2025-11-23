package com.aryanspatel.moodmatch.presentation.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aryanspatel.moodmatch.data.datastore.UserPreference
import com.aryanspatel.moodmatch.presentation.screens.ChatScreen
import com.aryanspatel.moodmatch.presentation.screens.MoodMatchScreen
import com.aryanspatel.moodmatch.presentation.screens.OnboardingScreen
import com.aryanspatel.moodmatch.presentation.viewmodels.MatchViewModel
import com.aryanspatel.moodmatch.presentation.viewmodels.OnboardingViewModel
import kotlinx.coroutines.flow.first

@Composable
fun NavGraph() {

    val context = LocalContext.current
    val navController = rememberNavController()
    val onBoardingViewModel = hiltViewModel<OnboardingViewModel>()
    val matchViewModel = hiltViewModel<MatchViewModel>()

    var startDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val onboardingFinished = UserPreference
            .getOnboardingStatus(context)
            .first() // suspend, runs off the main compose body

        startDestination = if (onboardingFinished) {
            Destinations.MoodMatchScreen.name
        } else {
            Destinations.OnboardingScreen.name
        }
    }

    if (startDestination == null) {
        Box(modifier = Modifier.fillMaxSize())
        return
    }

    NavHost(
        navController = navController,
        startDestination = startDestination!!,
        enterTransition = {fadeIn(animationSpec = tween(0))},
        exitTransition = {fadeOut(animationSpec = tween(0))},
        popEnterTransition = {fadeIn(animationSpec = tween(0))},
        popExitTransition = {fadeOut(animationSpec = tween(0))}
    ){
        composable(route = Destinations.OnboardingScreen.name) {
            OnboardingScreen(
                viewModel = onBoardingViewModel,
                onStartMatchClick = {
                    navController.navigate(Destinations.MoodMatchScreen.name){
                        popUpTo(0)
                    }
                }
            )
        }

        composable(route = Destinations.MoodMatchScreen.name) {
            MoodMatchScreen(
                viewModel = matchViewModel,
                onMatchFound = {navController.navigate(Destinations.ChatScreen.name)}
            )
        }

        composable(route = Destinations.ChatScreen.name) {
            ChatScreen(
                viewModel = matchViewModel,
                onLeave = {navController.popBackStack()}
            )
        }
    }

}