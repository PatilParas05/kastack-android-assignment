package dev.paraspatil.luminaai.presentation.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onFinishOnboarding: () -> Unit,
    viewModel: OnboardingViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // We use userScrollEnabled = false to force them to use our validated buttons
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = false,
                modifier = Modifier.weight(1f)
            ) { page ->
                when (page) {
                    0 -> ValueProps(
                        onNext = {
                            coroutineScope.launch { pagerState.animateScrollToPage(1) }
                        }
                    )
                    1 -> CollectInfo(
                        name = uiState.name,
                        age = uiState.age,
                        phone = uiState.phone,
                        otp = uiState.otp,
                        errorMessage = uiState.errorMessage,
                        onNameChange = viewModel::updateName,
                        onAgeChange = viewModel::updateAge,
                        onPhoneChange = viewModel::updatePhone,
                        onOtpChange = viewModel::updateOtp,
                        onNext = {
                            if (viewModel.validateStep2()) {
                                coroutineScope.launch { pagerState.animateScrollToPage(2) }
                            }
                        },
                        onBack = {
                            coroutineScope.launch { pagerState.animateScrollToPage(0) }
                        }
                    )
                    2 -> Personality(
                        selectedTraits = uiState.selectedTraits,
                        availableTraits = viewModel.availableTraits,
                        errorMessage = uiState.errorMessage,
                        onTraitToggle = viewModel::toggleTrait,
                        onFinish = {
                            viewModel.validateAndSave(onComplete = onFinishOnboarding)
                        },
                        onBack = {
                            coroutineScope.launch { pagerState.animateScrollToPage(1) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ValueProps(onNext: () -> Unit) {
    val props = listOf(
        "Welcome to Lumina AI",
        "Your completely offline assistant",
        "Always private, always fast",
        "Let's get to know you"
    )
    var visibleIndex by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        for (i in props.indices) {
            delay(1000)
            visibleIndex = i
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        props.forEachIndexed { index, prop ->
            AnimatedVisibility(
                visible = index <= visibleIndex,
                enter = fadeIn(animationSpec = tween(500))
            ) {
                Text(
                    text = prop,
                    fontSize = if (index == 0) 28.sp else 20.sp,
                    fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        AnimatedVisibility(visible = visibleIndex == props.lastIndex) {
            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Get Started")
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun ValuePropsPreview() {
    MaterialTheme {
        ValueProps(onNext = {})
    }
}

@Composable
fun CollectInfo(
    name: String,
    age: String,
    phone: String,
    otp: String,
    errorMessage: String?,
    onNameChange: (String) -> Unit,
    onAgeChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onOtpChange: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Tell us about yourself", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = age,
            onValueChange = onAgeChange,
            label = { Text("Age") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = phone,
            onValueChange = onPhoneChange,
            label = { Text("Phone (10 digits)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = otp,
            onValueChange = onOtpChange,
            label = { Text("OTP (Hint: 1234)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        errorMessage?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onBack) { Text("Back") }
            Button(onClick = onNext) { Text("Verify & Next") }
        }
    }
}
@Preview(showBackground = true)
@Composable
fun CollectInfoPreview(){
    MaterialTheme {
        CollectInfo(
            name = "John Doe",
            age = "25",
            phone = "1234567890",
            otp = "1234",
            errorMessage = null,
            onNameChange = {},
            onAgeChange = {},
            onPhoneChange = {},
            onOtpChange = {},
            onNext = {},
            onBack = {}
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Personality(
    selectedTraits: List<String>,
    availableTraits: List<String>,
    errorMessage: String?,
    onTraitToggle: (String) -> Unit,
    onFinish: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Build your Persona", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Select exactly 3 traits for your AI", fontSize = 16.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(24.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            availableTraits.forEach { trait ->
                val isSelected = selectedTraits.contains(trait)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onTraitToggle(trait) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = trait,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        errorMessage?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(32.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onBack) { Text("Back") }
            Button(onClick = onFinish) { Text("Finish Setup") }
        }
    }
}
@Preview(showBackground = true)
@Composable
fun PersonalityPreview(){
    MaterialTheme {
        Personality(
            selectedTraits = listOf("Friendly", "Creative"),
            availableTraits = listOf("Friendly", "Professional", "Witty", "Empathetic", "Direct", "Creative"),
            errorMessage = null,
            onTraitToggle = {},
            onBack = {},
            onFinish = {}
        )
    }
}