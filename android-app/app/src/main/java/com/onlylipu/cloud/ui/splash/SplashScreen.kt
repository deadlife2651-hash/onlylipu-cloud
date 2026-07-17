package com.onlylipu.cloud.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.onlylipu.cloud.data.auth.TokenStore
import com.onlylipu.cloud.navigation.Routes
import com.onlylipu.cloud.ui.theme.AccentCyan
import com.onlylipu.cloud.ui.theme.Graphite950
import com.onlylipu.cloud.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(nav: NavHostController) {
    val scale = remember { Animatable(0.6f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { scale.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
        launch { alpha.animateTo(1f, tween(700)) }
        delay(1400)
        val loggedIn = TokenStore(nav.context).isLoggedIn()
        nav.navigate(if (loggedIn) Routes.DASHBOARD else Routes.LOGIN) {
            popUpTo(Routes.SPLASH) { inclusive = true }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Graphite950),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.scale(scale.value).alpha(alpha.value)
        ) {
            OnlyLipuLogo(modifier = Modifier.size(96.dp))
            Spacer(Modifier.height(20.dp))
            Text(
                "ONLYLIPU CLOUD",
                color = Color(0xFFE8EEF6),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Your Private Computer. Your Cloud Android.",
                color = TextSecondary,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun OnlyLipuLogo(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.5f, h * 0.08f)
            lineTo(w * 0.86f, h * 0.29f)
            lineTo(w * 0.86f, h * 0.71f)
            lineTo(w * 0.5f, h * 0.92f)
            lineTo(w * 0.14f, h * 0.71f)
            lineTo(w * 0.14f, h * 0.29f)
            close()
        }
        val inner = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.5f, h * 0.2f)
            lineTo(w * 0.76f, h * 0.35f)
            lineTo(w * 0.76f, h * 0.65f)
            lineTo(w * 0.5f, h * 0.8f)
            lineTo(w * 0.24f, h * 0.65f)
            lineTo(w * 0.24f, h * 0.35f)
            close()
        }
        drawPath(path, AccentCyan)
        drawPath(inner, Graphite950)
        drawCircle(Color(0xFFE8EEF6), radius = w * 0.09f, center = center)
    }
}
