package michal.radecki.request_management.domain;

import java.util.function.BiPredicate;

public final class RequestLifecycle {

    private RequestLifecycle() {}

    public static final BiPredicate<RequestState, RequestState> IS_TRANSITION_ALLOWED =
            (from, to) -> switch (from) {
                case CREATED ->
                        to == RequestState.VERIFIED
                                || to == RequestState.DELETED;

                case VERIFIED ->
                        to == RequestState.ACCEPTED
                                || to == RequestState.REJECTED;

                case ACCEPTED ->
                        to == RequestState.PUBLISHED
                                || to == RequestState.REJECTED;

                default -> false;
            };
}
