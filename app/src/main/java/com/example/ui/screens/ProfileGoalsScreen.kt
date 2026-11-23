package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.BmiCategory
import com.example.data.model.UserProfile
import com.example.data.model.WeightUnit
import com.example.data.repository.FitnessCalculator
import com.example.ui.theme.ActiveAmber
import com.example.ui.theme.CalorieCoral
import com.example.ui.theme.GoalEmerald
import com.example.ui.theme.ProgressBlue
import com.example.ui.viewmodel.FitTrackUiState
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileGoalsScreen(
    uiState: FitTrackUiState,
    onSaveProfile: (UserProfile) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBack() }

    val profile = uiState.userProfile
    val weightUnit = uiState.userSettings.weightUnit

    var name by remember(profile.name) { mutableStateOf(profile.name) }
    var ageText by remember(profile.age) { mutableStateOf(profile.age.toString()) }
    var heightText by remember(profile.heightCm) {
        mutableStateOf(profile.heightCm.roundToInt().toString())
    }
    var weightText by remember(profile.weightKg, weightUnit) {
        val displayWeight = FitnessCalculator.kgToDisplayWeight(profile.weightKg, weightUnit)
        mutableStateOf(String.format("%.1f", displayWeight))
    }
    var stepGoalText by remember(profile.dailyStepGoal) {
        mutableStateOf(profile.dailyStepGoal.toString())
    }

    // Validation
    val ageValue = ageText.toIntOrNull()
    val ageError = when {
        ageText.isBlank() -> "Age is required"
        ageValue == null || ageValue < 10 || ageValue > 120 -> "Enter a realistic age (10–120)"
        else -> null
    }

    val heightValue = heightText.toDoubleOrNull()
    val heightError = when {
        heightText.isBlank() -> "Height is required"
        heightValue == null || heightValue < 90.0 || heightValue > 250.0 -> "Enter height between 90–250 cm"
        else -> null
    }

    val weightDisplayValue = weightText.toDoubleOrNull()
    val weightKgValue = weightDisplayValue?.let {
        FitnessCalculator.displayWeightToKg(it, weightUnit)
    }
    val weightError = when {
        weightText.isBlank() -> "Weight is required"
        weightKgValue == null || weightKgValue < 25.0 || weightKgValue > 300.0 -> {
            if (weightUnit == WeightUnit.KG) {
                "Enter weight between 25–300 kg"
            } else {
                "Enter weight between 55–660 lbs"
            }
        }
        else -> null
    }

    val goalValue = stepGoalText.toIntOrNull()
    val goalError = when {
        stepGoalText.isBlank() -> "Daily step goal is required"
        goalValue == null || goalValue < 500 || goalValue > 100_000 -> "Enter goal between 500–100,000"
        else -> null
    }

    val nameError = if (name.length > 40) "Name must be 40 characters or fewer" else null

    val isFormValid = nameError == null && ageError == null && heightError == null &&
        weightError == null && goalError == null

    val liveBmi = if (heightValue != null && weightKgValue != null) {
        FitnessCalculator.calculateBmi(heightValue, weightKgValue)
    } else {
        null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile & Goals") },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("profile_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Live BMI Card
                BmiCard(bmiResult = liveBmi)

                // Personal Info Section
                Text(
                    text = "Personal Details",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    placeholder = { Text("e.g. Alex") },
                    singleLine = true,
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_name_input")
                )

                OutlinedTextField(
                    value = ageText,
                    onValueChange = { if (it.all(Char::isDigit) && it.length <= 3) ageText = it },
                    label = { Text("Age (years)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = ageError != null,
                    supportingText = ageError?.let { { Text(it) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_age_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = heightText,
                        onValueChange = { value ->
                            if (value.count { it == '.' } <= 1 &&
                                value.all { it.isDigit() || it == '.' } &&
                                value.length <= 5
                            ) {
                                heightText = value
                            }
                        },
                        label = { Text("Height (cm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        isError = heightError != null,
                        supportingText = heightError?.let { { Text(it) } },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("profile_height_input")
                    )

                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { value ->
                            if (value.count { it == '.' } <= 1 &&
                                value.all { it.isDigit() || it == '.' } &&
                                value.length <= 6
                            ) {
                                weightText = value
                            }
                        },
                        label = { Text("Weight (${weightUnit.shortLabel})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        isError = weightError != null,
                        supportingText = weightError?.let { { Text(it) } },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("profile_weight_input")
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Daily Step Goal Section
                Text(
                    text = "Daily Step Goal",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                OutlinedTextField(
                    value = stepGoalText,
                    onValueChange = { if (it.all(Char::isDigit) && it.length <= 6) stepGoalText = it },
                    label = { Text("Target steps per day") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = goalError != null,
                    supportingText = goalError?.let { { Text(it) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_step_goal_input")
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(5000, 6000, 8000, 10000, 12000).forEach { preset ->
                        FilterChip(
                            selected = goalValue == preset,
                            onClick = { stepGoalText = preset.toString() },
                            label = { Text("%,d".format(preset)) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (isFormValid && ageValue != null && heightValue != null &&
                            weightKgValue != null && goalValue != null
                        ) {
                            onSaveProfile(
                                UserProfile(
                                    name = name.trim(),
                                    age = ageValue,
                                    heightCm = heightValue,
                                    weightKg = weightKgValue,
                                    dailyStepGoal = goalValue
                                )
                            )
                            onNavigateBack()
                        }
                    },
                    enabled = isFormValid,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("save_profile_button")
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null)
                    Spacer(Modifier.widthIn(min = 8.dp))
                    Text("Save Profile & Goals", style = MaterialTheme.typography.titleMedium)
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun BmiCard(bmiResult: com.example.data.model.BmiResult?) {
    val categoryColor = when (bmiResult?.category) {
        BmiCategory.UNDERWEIGHT -> ProgressBlue
        BmiCategory.HEALTHY -> GoalEmerald
        BmiCategory.OVERWEIGHT -> ActiveAmber
        BmiCategory.OBESE -> CalorieCoral
        null -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("bmi_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Body Mass Index (BMI)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = bmiResult?.let { String.format("%.1f", it.value) } ?: "--",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                bmiResult?.let { result ->
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = categoryColor.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = result.category.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = categoryColor,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4-zone BMI color bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Box(modifier = Modifier.weight(1f).height(8.dp).background(ProgressBlue))
                Box(modifier = Modifier.weight(1.4f).height(8.dp).background(GoalEmerald))
                Box(modifier = Modifier.weight(1f).height(8.dp).background(ActiveAmber))
                Box(modifier = Modifier.weight(1f).height(8.dp).background(CalorieCoral))
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("<18.5", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("18.5–24.9", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("25–29.9", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("30+", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
