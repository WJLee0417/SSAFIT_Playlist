package com.ssafy.ssafit.runningprofile.dto;

import com.ssafy.ssafit.runningprofile.domain.RunningGender;
import com.ssafy.ssafit.runningprofile.domain.RunningProfile;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public class RunningProfileViewForm {

    private RunningGender gender;

    @Min(value = 10, message = "나이는 10세 이상이어야 합니다.")
    @Max(value = 100, message = "나이는 100세 이하여야 합니다.")
    private Integer age;

    private Integer targetBpmMin;
    private Integer targetBpmMax;
    private Integer targetCadenceMin;
    private Integer targetCadenceMax;

    @Size(max = 100, message = "선호 장르는 100자 이하여야 합니다.")
    private String preferredGenre;

    public static RunningProfileViewForm from(RunningProfile profile) {
        RunningProfileViewForm form = new RunningProfileViewForm();
        form.setGender(profile.getGender());
        form.setAge(profile.getAge());
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
        profile.setGender(gender);
        profile.setAge(age);
        profile.setTargetBpmMin(targetBpmMin);
        profile.setTargetBpmMax(targetBpmMax);
        profile.setTargetCadenceMin(targetCadenceMin);
        profile.setTargetCadenceMax(targetCadenceMax);
        profile.setPreferredGenre(preferredGenre);
        return profile;
    }

    public RunningGender getGender() {
        return gender;
    }

    public void setGender(RunningGender gender) {
        this.gender = gender;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
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
