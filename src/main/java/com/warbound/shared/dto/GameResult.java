package com.warbound.shared.dto;

import java.io.Serializable;

public record GameResult(
        boolean accepted,
        String message,
        MatchView match
) implements Serializable {
}
