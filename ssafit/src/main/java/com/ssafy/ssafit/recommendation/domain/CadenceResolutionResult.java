package com.ssafy.ssafit.recommendation.domain;

public record CadenceResolutionResult(
        Integer targetCadenceSpm,
        String source,
        String explanation
) {
}
