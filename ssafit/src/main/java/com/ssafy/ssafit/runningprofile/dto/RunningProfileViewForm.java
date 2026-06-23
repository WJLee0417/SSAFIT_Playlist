package com.ssafy.ssafit.runningprofile.dto;

import com.ssafy.ssafit.runningprofile.domain.RunningIntensity;
import com.ssafy.ssafit.runningprofile.domain.RunningProfile;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class RunningProfileViewForm {

    @NotNull(message = "러닝 강도를 선택해 주세요.")
    private RunningIntensity runningIntensity;

    @NotNull(message = "최소 BPM을 입력해 주세요.")
    @Min(value = 60, message = "최소 BPM은 60 이상이어야 합니다.")
    @Max(value = 220, message = "최소 BPM은 220 이하여야 합니다.")
    private Integer targetBpmMin;

    @NotNull(message = "최대 BPM을 입력해 주세요.")
    @Min(value = 60, message = "최대 BPM은 60 이상이어야 합니다.")
    @Max(value = 220, message = "최대 BPM은 220 이하여야 합니다.")
    private Integer targetBpmMax;

    @Min(value = 60, message = "목표 케이던스 최소값은 60 SPM 이상이어야 합니다.")
    @Max(value = 240, message = "목표 케이던스 최소값은 240 SPM 이하여야 합니다.")
    private Integer targetCadenceMin;

    @Min(value = 60, message = "목표 케이던스 최대값은 60 SPM 이상이어야 합니다.")
    @Max(value = 240, message = "목표 케이던스 최대값은 240 SPM 이하여야 합니다.")
    private Integer targetCadenceMax;

    @Size(max = 100, message = "선호 장르는 100자 이하여야 합니다.")
    private String preferredGenre;

    public static RunningProfileViewForm from(RunningProfile profile) {
        RunningProfileViewForm form = new RunningProfileViewForm();
        form.setRunningIntensity(profile.getRunningIntensity());
        form.setTargetBpmMin(profile.getTargetBpmMin());
        form.setTargetBpmMax(profile.getTargetBpmMax());
        form.setTargetCadenceMin(profile.getTargetCadenceMin());
        form.setTargetCadenceMax(profile.getTargetCadenceMax());
        form.setPreferredGenre(profile.getPreferredGenre());
        return form;
    }

    public RunningProfile toDomain(Long memberId) {
        RunningProfile profile = new RunningProfile();
        profile.setMemberId(memberId);
        profile.setRunningIntensity(runningIntensity);
        profile.setTargetBpmMin(targetBpmMin);
        profile.setTargetBpmMax(targetBpmMax);
        profile.setTargetCadenceMin(targetCadenceMin);
        profile.setTargetCadenceMax(targetCadenceMax);
        profile.setPreferredGenre(preferredGenre);
        return profile;
    }

    public RunningIntensity getRunningIntensity() {
        return runningIntensity;
    }

    public void setRunningIntensity(RunningIntensity runningIntensity) {
        this.runningIntensity = runningIntensity;
    }

    public Integer getTargetBpmMin() {
        return targetBpmMin;
    }

    public void setTargetBpmMin(Integer targetBpmMin) {
        this.targetBpmMin = targetBpmMin;
    }

    public Integer getTargetBpmMax() {
        return targetBpmMax;
    }

    public void setTargetBpmMax(Integer targetBpmMax) {
        this.targetBpmMax = targetBpmMax;
    }

    public Integer getTargetCadenceMin() {
        return targetCadenceMin;
    }

    public void setTargetCadenceMin(Integer targetCadenceMin) {
        this.targetCadenceMin = targetCadenceMin;
    }

    public Integer getTargetCadenceMax() {
        return targetCadenceMax;
    }

    public void setTargetCadenceMax(Integer targetCadenceMax) {
        this.targetCadenceMax = targetCadenceMax;
    }

    public String getPreferredGenre() {
        return preferredGenre;
    }

    public void setPreferredGenre(String preferredGenre) {
        this.preferredGenre = preferredGenre;
    }
}
