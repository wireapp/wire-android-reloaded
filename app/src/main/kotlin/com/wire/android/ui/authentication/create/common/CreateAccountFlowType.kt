package com.wire.android.ui.authentication.create.common

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.wire.android.R
import kotlinx.parcelize.Parcelize

@Parcelize
enum class CreateAccountFlowType(
    val routeArg: String,
    @StringRes val titleResId: Int,
    val overviewResources: OverviewResources,
    val emailResources: EmailResources,
    val summaryResources: SummaryResources
): Parcelable {
    CreatePersonalAccount(
        routeArg = "create_personal_account",
        titleResId = R.string.create_personal_account_title,
        overviewResources = OverviewResources(
            overviewContentTitleResId = null,
            overviewContentTextResId = R.string.create_personal_account_text,
            overviewContentIconResId = R.drawable.ic_create_personal_account,
            overviewLearnMoreTextResId = R.string.label_learn_more
        ),
        emailResources = EmailResources(
            emailSubtitleResId = R.string.create_personal_account_email_text
        ),
        summaryResources = SummaryResources(
            summaryTitleResId = R.string.create_personal_account_title,
            summaryTextResId = R.string.create_personal_account_summary_text,
            summaryIconResId = R.drawable.ic_create_personal_account_success
        )
    ),
    CreateTeam(
        routeArg = "create_team",
        titleResId = R.string.create_team_title,
        overviewResources = OverviewResources(
            overviewContentTitleResId = R.string.create_team_content_title,
            overviewContentTextResId = R.string.create_team_text,
            overviewContentIconResId = R.drawable.ic_create_team,
            overviewLearnMoreTextResId = R.string.create_team_learn_more
        ),
        emailResources = EmailResources(
            emailSubtitleResId = R.string.create_team_email_text
        ),
        summaryResources = SummaryResources(
            summaryTitleResId = R.string.create_team_summary_title,
            summaryTextResId = R.string.create_team_summary_text,
            summaryIconResId = R.drawable.ic_create_team_success
        )
    );
    companion object {
        fun fromRouteArg(routeArg: String?) = values().firstOrNull { it.routeArg == routeArg }
    }
}

@Parcelize
data class OverviewResources(
    @StringRes val overviewContentTitleResId: Int?,
    @StringRes val overviewContentTextResId: Int,
    @DrawableRes val overviewContentIconResId: Int,
    @StringRes val overviewLearnMoreTextResId: Int
): Parcelable
@Parcelize
data class SummaryResources(
    @StringRes val summaryTitleResId: Int,
    @StringRes val summaryTextResId: Int,
    @DrawableRes val summaryIconResId: Int
): Parcelable
@Parcelize
data class EmailResources(@StringRes val emailSubtitleResId: Int): Parcelable
