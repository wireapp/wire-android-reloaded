package com.wire.android.ui.home.settings.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.common.textfield.WirePasswordTextField
import com.wire.android.ui.common.textfield.WirePrimaryButton
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@Composable
fun BackupAndRestoreScreen(viewModel: BackupAndRestoreViewModel = hiltViewModel()) {
    BackupAndRestoreContent(
        backUpAndRestoreState = viewModel.state,
        onCreateBackup = viewModel::createBackup,
        onSaveBackup = viewModel::saveBackup,
        onCancelBackup = viewModel::cancelBackup,
        onChooseBackupFile = viewModel::chooseBackupFile,
        onRestoreBackup = viewModel::restoreBackup,
        onValidateBackupPassword = viewModel::validateBackupPassword,
        onBackPressed = viewModel::navigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupAndRestoreContent(
    backUpAndRestoreState: BackupAndRestoreState,
    onValidateBackupPassword: (TextFieldValue) -> Unit,
    onCreateBackup: () -> Unit,
    onSaveBackup: () -> Unit,
    onCancelBackup: () -> Unit,
    onChooseBackupFile: () -> Unit,
    onRestoreBackup: () -> Unit,
    onBackPressed: () -> Unit
) {
    val backupAndRestoreStateHolder = rememberBackUpAndRestoreState()

    Scaffold(topBar = {
        WireCenterAlignedTopAppBar(
            elevation = 0.dp,
            title = stringResource(id = R.string.backup_and_restore_screen_title),
            onNavigationPressed = onBackPressed
        )
    }) { internalPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .fillMaxHeight()
                .padding(internalPadding)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Text(
                    text = stringResource(id = R.string.settings_backup_info),
                    style = MaterialTheme.wireTypography.body01,
                    color = MaterialTheme.wireColorScheme.onBackground,
                    modifier = Modifier.padding(MaterialTheme.wireDimensions.spacing16x)
                )
            }
            Surface(
                color = MaterialTheme.wireColorScheme.background,
                shadowElevation = MaterialTheme.wireDimensions.bottomNavigationShadowElevation
            ) {
                Column(
                    modifier = Modifier.padding(MaterialTheme.wireDimensions.spacing16x)
                ) {
                    WirePrimaryButton(
                        text = stringResource(id = R.string.settings_backup_create),
                        onClick = backupAndRestoreStateHolder::showBackupDialog,
                        modifier = Modifier.fillMaxWidth()
                    )
                    VerticalSpace.x8()
                    WirePrimaryButton(
                        text = stringResource(id = R.string.settings_backup_restore),
                        onClick = backupAndRestoreStateHolder::showRestoreDialog,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    if (backupAndRestoreStateHolder.dialogState !is BackupAndRestoreDialog.None) {
        when (backupAndRestoreStateHolder.dialogState) {
            is BackupAndRestoreDialog.Backup -> {
                val backupDialogStateHolder = rememberBackUpDialogState()

                LaunchedEffect(backUpAndRestoreState.isBackupPasswordValid) {
                    backupDialogStateHolder.isBackupPasswordValid = backUpAndRestoreState.isBackupPasswordValid
                }

                LaunchedEffect(backUpAndRestoreState.backupProgress) {
                    backupDialogStateHolder.backupProgress = backUpAndRestoreState.backupProgress
                }

                BackupDialog(
                    backupDialogStateHolder = backupDialogStateHolder,
                    onValidateBackupPassword = onValidateBackupPassword,
                    onCreateBackup = onCreateBackup,
                    onSaveBackup = onSaveBackup,
                    onCancelBackup = onCancelBackup,
                    onDismissDialog = {
                        onCancelBackup()
                        backupAndRestoreStateHolder.dismissDialog()
                    }
                )
            }
            is BackupAndRestoreDialog.Restore ->
                RestoreDialog(
                    onChooseBackupFile = onChooseBackupFile,
                    onRestoreBackup = onRestoreBackup
                )
        }
    }
}

@Composable
fun BackupDialog(
    backupDialogStateHolder: BackupDialogStateHolder,
    onValidateBackupPassword: (TextFieldValue) -> Unit,
    onCreateBackup: () -> Unit,
    onSaveBackup: () -> Unit,
    onCancelBackup: () -> Unit,
    onDismissDialog: () -> Unit
) {
    with(backupDialogStateHolder) {
        when (currentBackupDialogStep) {
            BackUpDialogStep.Inform -> {
                WireDialog(
                    title = "Set an email and password",
                    text = "You need an email and a password in order to back up your conversation history. You can do it from the account page in Settings.",
                    onDismiss = onDismissDialog,
                    optionButton1Properties = WireDialogButtonProperties(
                        onClick = ::nextStep,
                        text = stringResource(id = R.string.label_ok),
                        type = WireDialogButtonType.Primary,
                    )
                )
            }
            is BackUpDialogStep.SetPassword -> {
                LaunchedEffect(isBackupPasswordValid) {
                    if (isBackupPasswordValid) {
                        backupDialogStateHolder.toCreateBackUp()
                        onCreateBackup()
                    }
                }

                WireDialog(
                    title = "Set password",
                    text = "test",
                    onDismiss = onDismissDialog,
                    dismissButtonProperties = WireDialogButtonProperties(
                        onClick = onDismissDialog,
                        text = stringResource(id = R.string.label_cancel),
                        state = WireButtonState.Default
                    ),
                    optionButton1Properties = WireDialogButtonProperties(
                        onClick = { onValidateBackupPassword(backupPassword) },
                        text = stringResource(id = R.string.label_ok),
                        type = WireDialogButtonType.Primary,
                        state = if (isBackupPasswordValid) WireButtonState.Default else WireButtonState.Error
                    )
                ) {
                    WirePasswordTextField(
                        state = if (isBackupPasswordValid) WireTextFieldState.Error("some error") else WireTextFieldState.Default,
                        value = backupPassword,
                        onValueChange = { backupPassword = it }
                    )
                }
            }
            BackUpDialogStep.CreatingBackup -> {
                WireDialog(
                    title = "Creating Backup",
                    text = "test",
                    onDismiss = onDismissDialog,
                    optionButton1Properties = WireDialogButtonProperties(
                        onClick = ::nextStep,
                        text = stringResource(id = R.string.label_ok),
                        type = WireDialogButtonType.Primary,
                    )
                ) {

                }
            }
            BackUpDialogStep.Failure -> {
                WireDialog(
                    title = "Something went wrong",
                    text = "test",
                    onDismiss = onDismissDialog,
                    optionButton1Properties = WireDialogButtonProperties(
                        onClick = ::nextStep,
                        text = stringResource(id = R.string.label_ok),
                        type = WireDialogButtonType.Primary,
                    )
                ) {

                }
            }
        }
    }
}

@Composable
fun RestoreDialog(
    onChooseBackupFile: Any,
    onRestoreBackup: Any
) {
//    when (restoreDialogState) {
//
//    }
}

class BackUpAndRestoreStateHolder() {

    var dialogState: BackupAndRestoreDialog by mutableStateOf(
        BackupAndRestoreDialog.None
    )

    fun showBackupDialog() {
        dialogState = BackupAndRestoreDialog.Backup
    }

    fun showRestoreDialog() {
        dialogState = BackupAndRestoreDialog.Restore
    }

    fun dismissDialog() {
        dialogState = BackupAndRestoreDialog.None
    }

}

sealed class BackupAndRestoreDialog {
    object None : BackupAndRestoreDialog()
    object Backup : BackupAndRestoreDialog()
    object Restore : BackupAndRestoreDialog()
}

@Composable
fun rememberBackUpDialogState(): BackupDialogStateHolder {
    val backupDialogStateHolder = remember { BackupDialogStateHolder({}, { TextFieldValue("") }, {}) }

    return backupDialogStateHolder
}

@Composable
fun rememberBackUpAndRestoreState(): BackUpAndRestoreStateHolder {
    return remember {
        BackUpAndRestoreStateHolder()
    }
}

class BackupDialogStateHolder(
    val onDismiss: () -> Unit,
    val onStartBackup: (TextFieldValue) -> Unit,
    val onSaveBackup: () -> Unit
) {
    companion object {
        private const val INITIAL_STEP_INDEX = 0
    }

    private var currentStepIndex = INITIAL_STEP_INDEX

    private val steps: List<BackUpDialogStep> = listOf(
        BackUpDialogStep.Inform,
        BackUpDialogStep.SetPassword,
        BackUpDialogStep.CreatingBackup,
        BackUpDialogStep.Failure
    )

    var currentBackupDialogStep: BackUpDialogStep by mutableStateOf(steps[INITIAL_STEP_INDEX])

    var backupPassword: TextFieldValue by mutableStateOf(TextFieldValue(""))

    var isBackupPasswordValid: Boolean by mutableStateOf(false)

    var backupProgress: Float by mutableStateOf(0.0f)

    fun nextStep() {
        if (currentStepIndex != steps.lastIndex) {
            currentStepIndex += 1
            currentBackupDialogStep = steps[currentStepIndex]
        }
    }

    fun reset() {
        currentStepIndex = INITIAL_STEP_INDEX
        currentBackupDialogStep = steps[INITIAL_STEP_INDEX]
    }

    fun toCreateBackUp() {
        currentBackupDialogStep = steps[2]
    }

    private fun clearPasswordData() {
        backupPassword = TextFieldValue("")
        isBackupPasswordValid = false
    }

}

sealed interface BackUpDialogStep {
    object Inform : BackUpDialogStep
    object SetPassword : BackUpDialogStep
    object CreatingBackup : BackUpDialogStep
    object Failure : BackUpDialogStep
}

sealed interface RestoreDialogStep {
    object Inform : RestoreDialogStep
    object Failure : RestoreDialogStep, RestoreFailures
    object Restore : RestoreDialogStep
}

sealed interface RestoreFailures {
    object IncompatibleBackup : RestoreFailures
    object WrongBackup : RestoreFailures
    object SomethingWentWrong : RestoreFailures
    object WrongPassword : RestoreFailures
}

class RestoreDialogState() {

}

//@Preview
//@Composable
//fun BackupAndRestoreScreenPreview() {
//    BackupAndRestoreContent({}, {}, {}, {}, {}, {})
//}
