package com.wire.android.feature.conversation.data.remote

import com.google.gson.annotations.SerializedName

data class ConversationsResponse(
    @SerializedName("has_more")
    val hasMore: Boolean,

    @SerializedName("conversations")
    val conversations: List<ConversationResponse>
)

data class ConversationResponse(
    @SerializedName("creator")
    val creator: String,

    @SerializedName("members")
    val members: ConversationMembersResponse,

    @SerializedName("name")
    val name: String?,

    @SerializedName("qualified_id")
    val id: ConversationIdResponse,

    @SerializedName("type")
    val type: Int,

    @SerializedName("message_timer")
    val messageTimer: Int
)

data class ConversationIdResponse(
    @SerializedName("id")
    val value: String,

    @SerializedName("domain")
    val domain: String
)

data class ConversationMembersResponse(
    @SerializedName("self")
    val self: ConversationSelfMemberResponse,

    @SerializedName("others")
    val otherMembers: List<ConversationOtherMembersResponse>
)

data class ConversationSelfMemberResponse(
    @SerializedName("hidden_ref")
    val hiddenReference: String?,

    @SerializedName("service")
    val service: ServiceReferenceResponse?,

    @SerializedName("otr_muted_ref")
    val otrMutedReference: String?,

    @SerializedName("hidden")
    val hidden: Boolean?,

    @SerializedName("id")
    override val userId: String,

    @SerializedName("otr_archived")
    val otrArchived: Boolean?,

    @SerializedName("otr_muted")
    val otrMuted: Boolean?,

    @SerializedName("otr_archived_ref")
    val otrArchiveReference: String?
) : ConversationMemberResponse

data class ConversationOtherMembersResponse(
    @SerializedName("service")
    val service: ServiceReferenceResponse?,

    @SerializedName("id")
    override val userId: String,
) : ConversationMemberResponse

interface ConversationMemberResponse {
    val userId: String
}

data class ServiceReferenceResponse(
    @SerializedName("id")
    val id: String,

    @SerializedName("provider")
    val provider: String
)
