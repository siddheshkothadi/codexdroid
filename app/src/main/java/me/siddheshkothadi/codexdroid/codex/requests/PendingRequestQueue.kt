package me.siddheshkothadi.codexdroid.codex.requests

interface PendingRequestQueue {
    fun enqueue(request: PendingRequest)

    fun nextApproval(current: ApprovalPendingRequest?): ApprovalPendingRequest?

    fun nextUserInput(current: UserInputPendingRequest?): UserInputPendingRequest?

    fun nextUnknown(current: UnknownPendingRequest?): UnknownPendingRequest?
}

class InMemoryPendingRequestQueue : PendingRequestQueue {
    private val approvals = ArrayDeque<ApprovalPendingRequest>()
    private val userInputs = ArrayDeque<UserInputPendingRequest>()
    private val unknown = ArrayDeque<UnknownPendingRequest>()

    override fun enqueue(request: PendingRequest) {
        when (request) {
            is ApprovalPendingRequest -> approvals.addLast(request)
            is UserInputPendingRequest -> userInputs.addLast(request)
            is UnknownPendingRequest -> unknown.addLast(request)
        }
    }

    override fun nextApproval(current: ApprovalPendingRequest?): ApprovalPendingRequest? {
        return current ?: approvals.removeFirstOrNull()
    }

    override fun nextUserInput(current: UserInputPendingRequest?): UserInputPendingRequest? {
        return current ?: userInputs.removeFirstOrNull()
    }

    override fun nextUnknown(current: UnknownPendingRequest?): UnknownPendingRequest? {
        return current ?: unknown.removeFirstOrNull()
    }
}
