package com.waseefakhtar.doseapp.feature.home

import android.Manifest
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults.cardColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.firebase.analytics.FirebaseAnalytics
import com.waseefakhtar.doseapp.R
import com.waseefakhtar.doseapp.analytics.AnalyticsEvents
import com.waseefakhtar.doseapp.analytics.AnalyticsHelper
import com.waseefakhtar.doseapp.domain.model.Medication
import com.waseefakhtar.doseapp.feature.addmedication.navigation.AddMedicationDestination
import com.waseefakhtar.doseapp.feature.home.viewmodel.HomeState
import com.waseefakhtar.doseapp.feature.home.viewmodel.HomeViewModel
import com.waseefakhtar.doseapp.util.getTimeRemaining
import java.util.Calendar

@Composable
fun HomeRoute(
    navController: NavController,
    askNotificationPermission: Boolean,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val analyticsHelper = AnalyticsHelper.getInstance(LocalContext.current)
    val state = viewModel.state
    PermissionDialog(analyticsHelper, askNotificationPermission)
    HomeScreen(navController, analyticsHelper,  state, viewModel)
}

@Composable
fun HomeScreen(navController: NavController, analyticsHelper: AnalyticsHelper,  state: HomeState, viewModel: HomeViewModel) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DailyMedications(navController, analyticsHelper, state, viewModel)
    }
}

@Composable
fun Greeting() {
    Column {
        // TODO: Add greeting based on time of day e.g. Good Morning, Good Afternoon, Good evening.
        // TODO: Get name from DB and show user's first name.
        Text(
            text = "Good morning,",
            style = MaterialTheme.typography.displaySmall
        )
        Text(
            text = "Kathryn!",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.displaySmall
        )
        Spacer(modifier = Modifier.padding(8.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyOverviewCard(navController: NavController, analyticsHelper: AnalyticsHelper, medicationsToday: List<Medication>) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(36.dp),
        colors = cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.tertiary
        ),
        onClick = {
            analyticsHelper.logEvent(AnalyticsEvents.ADD_MEDICATION_CLICKED_DAILY_OVERVIEW)
            navController.navigate(AddMedicationDestination.route)
        }
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .padding(24.dp, 24.dp, 0.dp, 16.dp)
                    .fillMaxWidth(.36F),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Text(
                    text = "Your plan for today",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                )

                Text(
                    text = "${medicationsToday.filter { it.medicationTaken }.size} of ${medicationsToday.size} completed",
                    style = MaterialTheme.typography.titleSmall,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Image(
                    painter = painterResource(id = R.drawable.doctor), contentDescription = ""
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmptyCard(navController: NavController, analyticsHelper: AnalyticsHelper) {
    analyticsHelper.logEvent(AnalyticsEvents.EMPTY_CARD_SHOWN)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        shape = RoundedCornerShape(36.dp),
        colors = cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.tertiary
        ),
        onClick = {
            analyticsHelper.logEvent(AnalyticsEvents.ADD_MEDICATION_CLICKED_EMPTY_CARD)
            navController.navigate(AddMedicationDestination.route)
        }
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .padding(24.dp, 24.dp, 0.dp, 16.dp)
                    .fillMaxWidth(.50F),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Text(
                    text = "Welcome!",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                )

                Text(
                    text = "No medications added yet? Tap to get started and stay on top of your medication schedule.",
                    style = MaterialTheme.typography.titleSmall,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.Bottom
            ) {
                Image(
                    painter = painterResource(id = R.drawable.doctor), contentDescription = ""
                )
            }
        }
    }
}

@Composable
fun DailyMedications(navController: NavController, analyticsHelper: AnalyticsHelper, state: HomeState, viewModel: HomeViewModel) {

    val medicationList = state.medications.sortedBy { it.date }
    val combinedList: List<MedicationListItem> = mutableListOf<MedicationListItem>().apply {
        val calendar = Calendar.getInstance()
        val medicationsToday = medicationList.filter {
            val medicationDate = it.date
            calendar.time = medicationDate
            val medicationDay = calendar.get(Calendar.DAY_OF_YEAR)

            val todayCalendar = Calendar.getInstance()
            val todayDay = todayCalendar.get(Calendar.DAY_OF_YEAR)

            medicationDay == todayDay
        }

        add(MedicationListItem.DailyOverviewItem(medicationsToday, medicationList.isEmpty()))

        if (medicationsToday.isNotEmpty()) {
            add(MedicationListItem.HeaderItem("Today"))
            addAll(medicationsToday.map { MedicationListItem.MedicationItem(it) })
        }

        // Find medications for this week and add "This Week" header
        val startOfWeekThisWeek = Calendar.getInstance()
        startOfWeekThisWeek.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val endOfWeekThisWeek = startOfWeekThisWeek.clone() as Calendar
        endOfWeekThisWeek.add(Calendar.DAY_OF_WEEK, 6)
        val medicationsThisWeek = medicationList.filter {
            val medicationDate = it.date // Change this to the appropriate attribute
            medicationDate in startOfWeekThisWeek.time..endOfWeekThisWeek.time && !medicationsToday.contains(it)
        }
        if (medicationsThisWeek.isNotEmpty()) {
            add(MedicationListItem.HeaderItem("This Week"))
            addAll(medicationsThisWeek.map { MedicationListItem.MedicationItem(it) })
        }

        // Find medications for next week and add "Next Week" header
        val startOfWeekNextWeek = Calendar.getInstance()
        startOfWeekNextWeek.time = endOfWeekThisWeek.time // Use the end of current week as start of next week
        startOfWeekNextWeek.add(Calendar.DAY_OF_MONTH, 1)
        val endOfWeekNextWeek = startOfWeekNextWeek.clone() as Calendar
        endOfWeekNextWeek.add(Calendar.DAY_OF_MONTH, 6)
        val medicationsNextWeek = medicationList.filter {
            val medicationDate = it.date // Change this to the appropriate attribute
            medicationDate in startOfWeekNextWeek.time..endOfWeekNextWeek.time
        }
        if (medicationsNextWeek.isNotEmpty()) {
            add(MedicationListItem.HeaderItem("Next Week"))
            addAll(medicationsNextWeek.map { MedicationListItem.MedicationItem(it) })
        }
    }

    LazyColumn(
        modifier = Modifier,
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(
            items = combinedList,
            itemContent = {
                when (it) {
                    is MedicationListItem.DailyOverviewItem -> {
                        when (it.isMedicationListEmpty) {
                            true -> EmptyCard(navController, analyticsHelper)
                            false -> DailyOverviewCard(navController, analyticsHelper, it.medicationsToday)
                        }
                    }
                    is MedicationListItem.HeaderItem -> {
                        Text(
                            modifier = Modifier
                                .padding(4.dp, 12.dp, 8.dp, 0.dp)
                                .fillMaxWidth(),
                            text = it.headerText.uppercase(),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    is MedicationListItem.MedicationItem -> {
                        MedicationCard(it.medication, viewModel, analyticsHelper)
                    }
                }
            }
        )
    }
}

sealed class MedicationListItem {
    data class DailyOverviewItem(val medicationsToday: List<Medication>, val isMedicationListEmpty: Boolean) : MedicationListItem()
    data class MedicationItem(val medication: Medication) : MedicationListItem()
    data class HeaderItem(val headerText: String) : MedicationListItem()
}

@Composable
fun MedicationCard(medication: Medication, viewModel: HomeViewModel, analyticsHelper: AnalyticsHelper) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(30.dp),
        colors = cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(
                modifier = Modifier
                    .padding(16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = medication.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = medication.timesOfDay.joinToString(", ")
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = getTimeRemaining(medication),
                    fontWeight = FontWeight.Bold
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.End
            ) {

                Button(
                    onClick = {
                        analyticsHelper.logEvent(AnalyticsEvents.TAKE_MEDICATION_CLICKED)
                        viewModel.takeMedication(medication)
                    },
                    enabled = !medication.medicationTaken
                ) {
                    if (medication.medicationTaken) {
                        Text(
                            text = "Taken"
                        )
                    } else {
                        Text(
                            text = "Take now"
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionDialog(analyticsHelper: AnalyticsHelper, askNotificationPermission: Boolean) {
    if (askNotificationPermission && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)) {
        val notificationPermissionState = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS) { isGranted ->
            when (isGranted) {
                true -> analyticsHelper.logEvent(AnalyticsEvents.NOTIFICATION_PERMISSION_GRANTED)
                false -> analyticsHelper.logEvent(AnalyticsEvents.NOTIFICATION_PERMISSION_REFUSED)
            }

        }
        if (!notificationPermissionState.status.isGranted) {
            analyticsHelper.logEvent(AnalyticsEvents.NOTIFICATION_PERMISSION_DIALOG_SHOWN)
            AlertDialog(
                icon = {
                    Icon(imageVector = Icons.Default.Notifications, contentDescription = "Notifications")
                },
                title = {
                    Text(text = "Notification Permission Required")
                },
                text = {
                    Text(text = "To ensure you never miss your medication, please grant the notification permission.")
                },
                onDismissRequest = {
                    analyticsHelper.logEvent(AnalyticsEvents.NOTIFICATION_PERMISSION_DIALOG_DISMISSED)
                },
                confirmButton = {
                    Button(
                        onClick = {
                            notificationPermissionState.launchPermissionRequest()
                            analyticsHelper.logEvent(AnalyticsEvents.NOTIFICATION_PERMISSION_DIALOG_ALLOW_CLICKED)
                        }
                    ) {
                        Text("Allow")
                    }
                }
            )
        }
    }
}
